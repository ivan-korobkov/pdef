//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <ReactiveCocoa/ReactiveCocoa/RACSignal.h>
#import "PDClient.h"
#import "PDInterface.h"
#import "RACDisposable.h"
#import "RACSubscriber.h"
#import "PDError.h"
#import "PDJson.h"
#import "NSInvocation+PDef.h"
#import "PDStruct.h"
#import "PDTypes.h"
#import "PDEnum.h"


#define AssertTrue(expr, error, msg, ...) \
    if(!expr) { \
        NSString *message = [NSString stringWithFormat:msg, ## __VA_ARGS__]; \
        *error = [NSError errorWithDomain:PDErrorDomain code:PDErrorClientError \
            userInfo:@{NSLocalizedDescriptionKey : message}]; \
        return nil; \
    }


@interface PDClientRequest ()
@property(nonatomic) NSString *method;
@property(nonatomic) NSString *path;
@property(nonatomic) NSString *post;
@end


@implementation PDClientRequest
@end


@implementation PDClient
static NSDateFormatter *dateFormatter;

+ (void)initialize {
    dateFormatter = [[NSDateFormatter alloc] init];
    dateFormatter.timeZone = [NSTimeZone timeZoneWithName:@"UTC"];
    dateFormatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"en_US_POSIX"];
    dateFormatter.dateFormat = @"yyyy-MM-dd'T'HH:mm:ss'Z'";
}

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url {
    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:config];
    return [self initWithInterface:iface url:url session:session];
}

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session {
    return [self initWithInterface:iface url:url session:session errorHandler:nil];
}

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session
                     errorHandler:(PDClientErrorHandler)errorHandler {
    NSParameterAssert(iface != nil);
    NSParameterAssert(url != nil);
    NSParameterAssert(session != nil);
    NSParameterAssert([iface isSubclassOfClass:PDInterface.class]);

    if (self = [super init]) {
        _iface = iface;
        _session = session;
        _errorHandler = [errorHandler copy];
        _url = [url hasSuffix:@"/"] ? url : [url stringByAppendingString:@"/"];
    }

    return self;
}

- (RACSignal *)handle:(NSArray *)invocations {
    id resultType;
    NSError *reqError;
    NSURLRequest *request = [self requestForInvocations:invocations resultType:&resultType
        error:&reqError];
    if (reqError) {
        return [RACSignal error:reqError];
    }

    return [RACSignal createSignal:^RACDisposable *(id <RACSubscriber> subscriber) {
        void (^handler)(NSData *, NSURLResponse *, NSError *) =
            ^(NSData *data, NSURLResponse *response, NSError *error) {
                NSHTTPURLResponse *http = (NSHTTPURLResponse *) response;

                id result;
                if (!error && [self isValidResponse:http]) {
                    result = [self handleResponse:data resultType:resultType error:&error];
                } else {
                    result = [self handleError:data response:http error:&error];
                }

                if (error) {
                    [subscriber sendError:error];
                } else {
                    [subscriber sendNext:result];
                    [subscriber sendCompleted];
                }
            };

        NSURLSessionDataTask *t = [_session dataTaskWithRequest:request completionHandler:handler];
        return [RACDisposable disposableWithBlock:^{
            [t cancel];
        }];
    }];
}

- (NSURLRequest *)requestForInvocations:(NSArray *)invocations resultType:(id *)resultType
                                  error:(NSError **)error {
    PDClientRequest *req = [PDClient serializeInvocations:invocations iface:_iface
        resultType:resultType error:error];
    if (!req) {
        return nil;
    }

    NSURL *url = [NSURL URLWithString:[_url stringByAppendingString:req.path]];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];
    request.HTTPMethod = req.method;
    [request addValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];

    if (req.post) {
        request.HTTPBody = [req.post dataUsingEncoding:NSUTF8StringEncoding];
    }

    return request;
}

- (BOOL)isValidResponse:(NSHTTPURLResponse *)response {
    if (response.statusCode != 200) {
        return NO;
    }

    NSString *ctype = response.allHeaderFields[@"Content-Type"];
    ctype = [ctype lowercaseString];

    NSArray *types = @[@"application/json", @"text/json", @"text/javascript"];
    return [types containsObject:ctype];
}

- (id)handleResponse:(NSData *)data resultType:(id)resultType error:(NSError **)error {
    id object = [NSJSONSerialization JSONObjectWithData:data options:0 error:error];
    AssertTrue([object isKindOfClass:NSDictionary.class], error,
    @"Cannot parse an invocation result from '%@'", [object class])

    id resultData = object[@"data"];
    return [PDJson parseJsonObject:resultData type:resultType error:error];
}

- (id)handleError:(NSData *)data response:(NSHTTPURLResponse *)response
            error:(NSError **)error {
    if (_errorHandler) {
        return _errorHandler(data, response, error);
    }

    if (*error) {
        return nil;
    }

    *error = [PDClient errorWithDescription:@"Failed to handle a server response"];
    return nil;
}

#pragma mark - Serialization

+ (PDClientRequest *)serializeInvocations:(NSArray *)invocations iface:(Class)iface
                               resultType:(id *)resultType error:(NSError **)error {
    NSString *httpMethod = nil;
    NSMutableArray *path = [NSMutableArray array];
    NSMutableDictionary *params = [NSMutableDictionary dictionary];

    for (NSInvocation *invocation in invocations) {
        AssertTrue(PDTypeForType(iface) == PDTypeInterface, error,
        @"Wrong interface class '%@'", iface)

        PDMethod *method = [iface pdef_methodForSelector:invocation.selector];
        AssertTrue(method, error,
        @"Cannot find a method for selector %@", NSStringFromSelector(invocation.selector))

        NSArray *names = method.paramNames;
        NSArray *types = method.paramTypes;
        NSArray *values = [invocation pdef_argumentsForMethod:method];
        [path addObject:method.name];

        if (method.isRequest) {
            NSDictionary *params0 = [self serializeRequest:values[0] error:error];
            if (!params0) return nil;

            [params addEntriesFromDictionary:params0];

        } else if (method.isPost || method.isLast) {
            NSDictionary *params0 = [self serializeParams:values names:names types:types
                error:error];
            if (!params0) return nil;

            [params addEntriesFromDictionary:params0];

        } else {
            NSArray *path0 = [self serializeArgs:values types:types error:error];
            if (!path0) return nil;

            [path addObjectsFromArray:path0];

        }

        if (method.isLast) {
            iface = nil;
            *resultType = method.result;
            httpMethod = method.isPost ? @"POST" : @"GET";
        } else {
            iface = method.result;
        }
    }

    NSString *path1 = [self urlencodePath:path];
    return [self requestWithMethod:httpMethod path:path1 params:params];
}

+ (NSDictionary *)serializeRequest:(PDStruct *)request error:(NSError **)error {
    NSMutableDictionary *params = [NSMutableDictionary dictionary];
    NSDictionary *properties = [request pdef_properties];

    for (NSString *key in properties) {
        id type = properties[key];
        id value = [request valueForKey:key];
        if (!value) {
            continue;
        }

        NSString *string = [self serializeValue:value type:type error:error];
        if (!string) {
            return nil;
        }

        params[key] = string;
    }

    return params;
}

+ (NSArray *)serializeArgs:(NSArray *)values types:(NSArray *)types error:(NSError **)error {
    NSMutableArray *args = [NSMutableArray array];

    for (uint j = 0; j < values.count; j++) {
        id type = types[j];
        id arg = values[j];

        NSString *string = [self serializeValue:arg type:type error:error];
        if (!string) {
            return nil;
        }

        [args addObject:string];
    }

    return args;
}

+ (NSDictionary *)serializeParams:(NSArray *)values names:(NSArray *)names types:(NSArray *)types
                            error:(NSError **)error {
    NSMutableDictionary *params = [NSMutableDictionary dictionary];

    for (uint j = 0; j < values.count; j++) {
        NSString *name = names[j];
        id type = types[j];
        id value = values[j];

        NSString *string = [self serializeValue:value type:type error:error];
        if (!string) {
            return nil;
        }

        params[name] = string;
    }

    return params;
}

+ (NSString *)serializeValue:(id)value type:(id)type error:(NSError **)error {
    if (!value) return @"";
    PDType type0 = PDTypeForType(type);

    switch (type0) {
        case PDTypeBool:{
            AssertTrue([value isKindOfClass:NSNumber.class], error,
            @"Cannot serialize an enum to string from value '%@'", value)

            BOOL b = ((NSNumber *) value).boolValue;
            return b ? @"1" : @"0";
        }

        case PDTypeInt16:
        case PDTypeInt32:
        case PDTypeInt64:
        case PDTypeFloat:
        case PDTypeDouble:{
            AssertTrue([value isKindOfClass:NSNumber.class], error,
            @"Cannot serialize a number to string from value '%@'", value)

            NSNumber *number = value;
            return [number stringValue];
        }

        case PDTypeString: {
            AssertTrue([value isKindOfClass:NSString.class], error,
            @"Cannot serialize a string from value '%@'", value)

            return value;
        }

        case PDTypeDate: {
            AssertTrue([value isKindOfClass:NSDate.class], error,
            @"Cannot serialize a date from value '%@'", value)

            @synchronized (self) {
                return [dateFormatter stringFromDate:value];
            }
        }

        case PDTypeEnum: {
            AssertTrue([value isKindOfClass:NSNumber.class], error,
            @"Cannot serialize an enum from value '%@'", value)

            Class enum0 = type;
            NSNumber *number = value;

            NSString *name = [enum0 nameForEnumValue:number.integerValue];
            AssertTrue(name, error, @"Cannot serialize an enum '%@' as type '%@'", value, type)
            return [name lowercaseString];
        }

        case PDTypeList:
        case PDTypeSet:
        case PDTypeMap:
        case PDTypeStruct: {
            NSData *data = [PDJson serialize:value type:type options:0 error:error];
            if (!data) {
                return nil;
            }

            return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        }

        default: {
            *error = [self errorWithDescription:@"Cannot serialize to string '%@'"];
            return nil;
        }
    }
}

+ (PDClientRequest *)requestWithMethod:(NSString *)method path:(NSString *)path
                                params:(NSMutableDictionary *)params {
    NSString *post = nil;
    NSString *query = nil;

    if ([@"POST" isEqualToString:method]) {
        post = [self urlencodeParams:params];
    } else {
        query = [self urlencodeParams:params];
    }

    if (query) {
        path = [[path stringByAppendingString:@"?"] stringByAppendingString:query];
    }

    PDClientRequest *request = [[PDClientRequest alloc] init];
    request.method = method;
    request.path = path;
    request.post = post;
    return request;
}

+ (NSString *)urlencodePath:(NSArray *)path {
    NSMutableArray *array = [NSMutableArray array];

    for (NSString *s in path) {
        NSString *encoded = [self urlencode:s];
        [array addObject:encoded];
    }

    return [array componentsJoinedByString:@"/"];
}

+ (NSString *)urlencodeParams:(NSDictionary *)params {
    NSMutableString *result = [NSMutableString string];

    for (NSString *key in params) {
        NSString *value = params[key];
        NSString *encoded = [self urlencode:value];

        if (result.length != 0) [result appendString:@"&"];
        [result appendString:key];
        [result appendString:@"="];
        [result appendString:encoded];
    }

    return result;
}

+ (NSString *)urlencode:(NSString *)value {
    return (__bridge NSString *) CFURLCreateStringByAddingPercentEscapes(
        NULL,
        (__bridge CFStringRef) value,
        (CFStringRef) @"-{}[]\"",
        (CFStringRef)@"!*'():;@&=+$/?%#",
        kCFStringEncodingUTF8);
}

+ (NSError *)errorWithDescription:(NSString *)description {
    description = description ? description : @"Client error";

    return [NSError errorWithDomain:PDErrorDomain code:PDErrorClientError
        userInfo:@{NSLocalizedDescriptionKey : description}];
}
@end
