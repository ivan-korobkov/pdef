//
// Created by Ivan Korobkov on 05.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import "PDInterface+PDProxy.h"


@implementation PDInterface (PDProxy)
+ (instancetype)proxyWithHandler:(PDProxyInvocationHandler)handler {
    return (id) [PDProxy proxyForClass:self withHandler:handler];
}
@end
