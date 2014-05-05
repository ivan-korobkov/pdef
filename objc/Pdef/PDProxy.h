//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class RACSignal;


typedef RACSignal *(^PDProxyInvocationHandler)(NSArray *invocations);


/**
 * Client proxy for PDInterface subclasses.
 * Returns a new proxy if a method return a PDInterface subclass,
 * Otherwise invokes a handler with an invocation chain.
 *
 * It supports only PDInterface and RACSignal as method return types.
 */
@interface PDProxy : NSProxy
+ (id)proxyForClass:(Class)cls withHandler:(PDProxyInvocationHandler)handler;

- (instancetype)initWithClass:(Class)cls handler:(PDProxyInvocationHandler)handler;
@end
