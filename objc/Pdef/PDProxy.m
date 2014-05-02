//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDProxy.h"
#import "RACSignal.h"
#import "PDInterface.h"


@implementation PDProxy {
    Class _cls;
    NSArray *_invocations;
    PDProxyInvocationHandler _handler;
}

+ (id)proxyForClass:(Class)cls withHandler:(PDProxyInvocationHandler)handler {
    return [[self alloc] initWithClass:cls handler:handler];
}

- (instancetype)initWithClass:(Class)cls handler:(PDProxyInvocationHandler)handler {
    return [self initWithClass:cls handler:handler invocations:@[]];
}

- (instancetype)initWithClass:(Class)cls handler:(PDProxyInvocationHandler)handler
                  invocations:(NSArray *)invocations {
    NSParameterAssert(cls != nil);
    NSParameterAssert(handler != nil);
    NSParameterAssert([cls isSubclassOfClass:PDInterface.class]);
    NSParameterAssert(invocations != nil);

    if (self) {
        _cls = cls;
        _handler = [handler copy];
        _invocations = invocations;
    }

    return self;
}

- (NSString *)description {
    NSMutableString *s = [NSMutableString string];
    [s appendString:@"<Proxy of "];
    [s appendString:NSStringFromClass(_cls)];
    [s appendString:@">"];
    return s;
}

- (void)forwardInvocation:(NSInvocation *)invocation {
    SEL selector = invocation.selector;
    NSArray *methods = [_cls pdef_methods];

    // Find a method by the invocation selector.
    NSUInteger index = [methods indexOfObjectPassingTest:
        ^BOOL(PDMethod *method, NSUInteger idx, BOOL *stop) {
            return method.selector == selector;
        }];
    if (index == NSNotFound) {
        return [super forwardInvocation:invocation];
    }

    // Create a new chain with this invocation.
    [invocation retainArguments];
    NSArray *invocations = [_invocations arrayByAddingObject:invocation];

    // Get the expected method result.
    PDMethod *method = methods[index];
    BOOL resultIsInterface = class_isMetaClass(object_getClass(method.result))
        && [method.result isSubclassOfClass:PDInterface.class];

    // If the result is a pdef interface, return another proxy,
    // Otherwise, invoke the handler.
    id result = resultIsInterface
        ? (id) [[PDProxy alloc] initWithClass:method.result handler:_handler invocations:invocations]
        : (id) _handler(invocations);

    __unsafe_unretained id value = result;
    [invocation setReturnValue:&value];
}

- (NSMethodSignature *)methodSignatureForSelector:(SEL)sel {
    return [_cls instanceMethodSignatureForSelector:sel];
}

#pragma mark NSObject protocol

- (BOOL)isKindOfClass:(Class)aClass {
    return [_cls isSubclassOfClass:aClass];
}

- (BOOL)respondsToSelector:(SEL)aSelector {
    return [_cls instancesRespondToSelector:aSelector];
}
@end
