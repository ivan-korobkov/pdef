//
// Created by Ivan Korobkov on 27.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <ReactiveCocoa/ReactiveCocoa/RACSignal.h>
#import "PDServer.h"
#import "PDInterface.h"
#import "PDError.h"
#import "PDTypes.h"
#import "PDStruct.h"
#import "PDEnum.h"
#import "PDJson.h"


@interface PDServer ()
@property(nonatomic) Class cls;
@end


@implementation PDServer
static NSDateFormatter *dateFormatter; // Always access as @synchronized(PDServer.class)

+ (void)initialize {
    NSTimeZone *tz = [NSTimeZone timeZoneWithAbbreviation:@"UTC"];
    NSParameterAssert(tz != nil);

    dateFormatter = [[NSDateFormatter alloc] init];
    dateFormatter.timeZone = tz;
    dateFormatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    dateFormatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ss'Z'";
}

- (instancetype)initWithInterface:(PDInterface *)iface {
    NSParameterAssert(iface != nil);

    if (self = [super init]) {
        _iface = iface;
        _cls = iface.class;
    }

    return self;
}

- (RACSignal *)resultForRequest:(NSURLRequest *)request resultType:(id *)resultType {
    NSParameterAssert(request != nil);

    NSError *error = nil;
    NSArray *invocations = [self parseRequest:request cls:_cls error:&error resultType:resultType];
    if (error) {
        return [RACSignal error:error];
    }

    __unsafe_unretained id target = _iface;
    for (NSUInteger j = 0; j < invocations.count; j++) {
        NSInvocation *invocation = invocations[j];
        BOOL isLast = j == (invocations.count - 1);

        [invocation invokeWithTarget:target];
        [invocation getReturnValue:&target];

        if (!isLast && !target) {
            NSString *desc = [NSString stringWithFormat:
                @"Got a nil interface in an invocation chain, target=%@, invocation=%@",
                target, invocation];
            error = [NSError errorWithDomain:PDErrorDomain code:PDErrorServerInternalError
                userInfo:@{NSLocalizedDescriptionKey : desc}];
            return [RACSignal error:error];
        }
    }

    return target;
}

- (NSArray *)parseRequest:(NSURLRequest *)request cls:(Class)cls
                    error:(NSError **)error resultType:(id *)resultType {
    NSMutableArray *invocations = [[NSMutableArray alloc] init];
    NSURL *url = request.URL;

    NSMutableArray *path = [self parsePath:url.path];
    NSDictionary *params = [request.HTTPMethod isEqualToString:@"POST"] ?
        [self parsePost:request.HTTPBody] : [self parseQuery:url.query];

    PDMethod *method;
    while (path.count) {

        // Find a method by a name.
        NSString *name = [path objectAtIndex:0];
        [path removeObjectAtIndex:0];
        method = [cls pdef_methodForName:name];
        if (!method) {
            return [self badRequest:error msg:@"Method not found"];
        }

        // Check the required HTTP method.
        if (method.isPost && ![request.HTTPMethod isEqualToString:@"POST"]) {
            return [self badRequest:error msg:@"Method not allowed, POST required"];
        }

        // Parse an invocation.
        NSArray *args = method.isRequest ?
            [self parseArgsAsRequest:method params:params error:error] :
            [self parseArgs:method path:path params:params error:error];
        if (!args) return nil;

        NSInvocation *invocation = [self createInvocation:cls method:method args:args error:error];
        if (!invocation) return nil;
        [invocations addObject:invocation];

        // Continue parsing invocation when the result is an interface, stop otherwise.
        if (PDTypeForType(method.result) == PDTypeInterface) {
            cls = method.result;
        } else {
            break;
        }
    }

    if (path.count) {
        return [self badRequest:error msg:@"Invocation chain did not consume all path segments."];
    }

    if (!invocations.count) {
        return [self badRequest:error msg:@"Method invocation required"];
    }

    if (PDTypeForType(method.result) == PDTypeInterface) {
        return [self badRequest:error msg:@"The last method must be void or return a data type."];
    }

    *resultType = method.result;
    return invocations;
}

- (NSArray *)parseArgs:(PDMethod *)method path:(NSMutableArray *)path
                params:(NSDictionary *)params error:(NSError **)error {
    NSArray *types = method.paramTypes;
    NSArray *names = method.paramNames;

    NSMutableArray *args = [[NSMutableArray alloc] init];
    if (!types.count) {
        return args;
    }

    BOOL resultIsInterface = PDTypeForType(method.result) == PDTypeInterface;
    for (uint i = 0; i < types.count; ++i) {
        NSString *name = names[i];
        NSString *value;

        if (resultIsInterface) {
            if (!path.count) {
                return [self badRequest:error msg:[NSString stringWithFormat:
                    @"Wrong number of args for method \"%@\"", method.name]];
            }

            value = [path objectAtIndex:0];
            value = [self urldecode:value];
            [path removeObjectAtIndex:0];

        } else {
            value = params[name];
        }

        id type = types[i];
        id arg = [self parseArg:name type:type string:value error:error];
        if (*error) {
            return nil;
        }

        [args addObject:arg ? arg : [NSNull null]];
    }

    return args;
}

- (NSArray *)parseArgsAsRequest:(PDMethod *)method params:(NSDictionary *)params
                          error:(NSError **)error {
    Class cls = method.paramTypes[0];
    NSObject *request = [[cls alloc] init];
    NSDictionary *properties = [cls pdef_properties];

    for (NSString *key in properties) {
        NSString *value = params[key];
        if (value == nil) {
            continue;
        }

        id type = properties[key];
        id arg = [self parseArg:key type:type string:value error:error];
        if (*error) {
            return nil;
        }

        [request setValue:arg forKey:key];
    }

    return @[request];
}

- (id)parseArg:(NSString *)name type:(id)type string:(NSString *)string error:(NSError **)error {
    if (!string) {
        return nil;
    }

    PDType type0 = PDTypeForType(type);
    switch (type0) {
        case PDTypeBool:return [self parseBoolean:string];
        case PDTypeInt16:
        case PDTypeInt32:
        case PDTypeInt64:return @(string.intValue);
        case PDTypeFloat:return @(string.floatValue);
        case PDTypeDouble:return @(string.doubleValue);
        case PDTypeString:return string;
        case PDTypeDate:return [self parseDate:string];
        case PDTypeEnum:return [self parseEnum:type string:string error:error];
        case PDTypeList:
        case PDTypeSet:
        case PDTypeMap:
        case PDTypeStruct:return [self parseJson:type string:string error:error];
        default: {
            [self badRequest:error msg:[NSString stringWithFormat:
                @"Failed to parse an argument of name \"%@\"", name]];
            return nil;
        }
    }
}

- (id)parseBoolean:(NSString *)string {
    if ([@"0" isEqualToString:string]) return @(NO);
    if ([@"1" isEqualToString:string]) return @(YES);
    return @(string.boolValue);
}

- (id)parseDate:(NSString *)string {
    if (string.length == 0) return nil;

    @synchronized (PDServer.class) {
        return [dateFormatter dateFromString:string];
    }
}

- (id)parseEnum:(Class)type string:(NSString *)string error:(NSError **)error {
    NSInteger value = [type enumValueForName:string];
    return value ? @(value) : nil;
}

- (id)parseJson:(id)type string:(NSString *)string error:(NSError **)error {
    NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
    return [PDJson parseJson:data type:type error:error];
}

- (NSInvocation *)createInvocation:(Class)cls method:(PDMethod *)method args:(NSArray *)args
                             error:(NSError **)error {
    SEL sel = method.selector;
    NSMethodSignature *signature = [cls instanceMethodSignatureForSelector:sel];
    if (!signature) {
        [self badRequest:error msg:@"Method not found"];
        return nil;
    }

    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
    [invocation setSelector:sel];
    [invocation retainArguments];

    NSArray *types = method.paramTypes;
    NSParameterAssert(types.count == args.count);

    // Indices 0 and 1 indicate the hidden arguments self and _cmd, respectively.
    NSUInteger invocationArg = 2;
    for (uint j = 0; j < args.count; ++j) {
        id arg = args[j];
        id type = types[j];
        if (arg == [NSNull null]) {
            arg = nil;
        }

        PDType type0 = PDTypeForType(type);
        switch (type0) {
            case PDTypeBool: {
                BOOL v = arg ? ((NSNumber *) arg).boolValue : NO;
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            case PDTypeInt16: {
                uint16_t v = (uint16_t) (arg ? ((NSNumber *) arg).intValue : 0);
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            case PDTypeInt32: {
                uint32_t v = (uint32_t) (arg ? ((NSNumber *) arg).intValue : 0);
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            case PDTypeInt64: {
                uint64_t v = (uint64_t) (arg ? ((NSNumber *) arg).longLongValue : 0);
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            case PDTypeFloat: {
                float v = arg ? ((NSNumber *) arg).floatValue : 0;
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            case PDTypeDouble: {
                double v = arg ? ((NSNumber *) arg).doubleValue : 0;
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            case PDTypeEnum: {
                NSInteger v = arg ? ((NSNumber *) arg).intValue : 0;
                [invocation setArgument:&v atIndex:invocationArg];
                break;
            }
            default: {
                __unsafe_unretained id arg0 = arg;
                [invocation setArgument:&arg0 atIndex:invocationArg];
            }
        }

        invocationArg++;
    }

    return invocation;
}

- (NSMutableArray *)parsePath:(NSString *)path {
    NSCharacterSet *set = [NSCharacterSet characterSetWithCharactersInString:@"/"];
    path = [path stringByTrimmingCharactersInSet:set];
    return [path componentsSeparatedByString:@"/"].mutableCopy;
}

- (NSDictionary *)parsePost:(NSData *)data {
    NSString *string = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return [self parseQuery:string];
}

- (NSDictionary *)parseQuery:(NSString *)query {
    NSMutableDictionary *params = [[NSMutableDictionary alloc] initWithCapacity:6];
    NSArray *pairs = [query componentsSeparatedByString:@"&"];

    for (NSString *pair in pairs) {
        NSArray *elements = [pair componentsSeparatedByString:@"="];
        NSString *key = elements[0];
        NSString *val = elements.count == 2 ? elements[1] : @"";
        key = [self urldecode:key];
        val = [self urldecode:val];

        params[key] = val;
    }

    return params;
}

- (NSString *)urldecode:(NSString *)value {
    return (__bridge NSString *) CFURLCreateStringByReplacingPercentEscapesUsingEncoding(
        NULL, (__bridge CFStringRef) value, CFSTR(""), kCFStringEncodingUTF8);
}

- (NSArray *)badRequest:(NSError **)error msg:(NSString *)msg {
    *error = [NSError errorWithDomain:PDErrorDomain code:PDErrorServerBadRequest
        userInfo:@{
            NSLocalizedDescriptionKey : msg ? msg : @"Bad request"
        }];
    return nil;
}
@end
