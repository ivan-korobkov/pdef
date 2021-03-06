//
// Created by Ivan Korobkov on 02.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import "PDInterface+PDClient.h"
#import "PDProxy.h"


@implementation PDInterface (PDClient)
+ (instancetype)clientWithUrl:(NSString *)url {
    PDClient *client = [[PDClient alloc] initWithInterface:self url:url];
    return [self proxyWithClient:client];
}

+ (instancetype)clientWithUrl:(NSString *)url session:(NSURLSession *)session {
    PDClient *client = [[PDClient alloc] initWithInterface:self url:url session:session];
    return [self proxyWithClient:client];
}

+ (instancetype)clientWithUrl:(NSString *)url session:(NSURLSession *)session
                     delegate:(id <PDClientDelegate>)delegate {
    PDClient *client = [[PDClient alloc]
        initWithInterface:self url:url session:session delegate:delegate];
    return [self proxyWithClient:client];
}

+ (instancetype)proxyWithClient:(PDClient *)client {
    return (id) [PDProxy proxyForClass:self withHandler:^RACSignal *(NSArray *invocations) {
        return [client handle:invocations];
    }];
}
@end
