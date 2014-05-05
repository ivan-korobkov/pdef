//
// Created by Ivan Korobkov on 05.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "PDInterface.h"
#import "PDProxy.h"

@interface PDInterface (PDProxy)
+ (instancetype)proxyWithHandler:(PDProxyInvocationHandler)handler;
@end
