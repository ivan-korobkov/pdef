//
// Created by Ivan Korobkov on 02.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "PDInterface.h"
#import "PDClient.h"

@interface PDInterface (PDClient)
+ (instancetype)clientWithUrl:(NSString *)url;

+ (instancetype)clientWithUrl:(NSString *)url session:(NSURLSession *)session;

+ (instancetype)clientWithUrl:(NSString *)url session:(NSURLSession *)session
                  interceptor:(PDClientRequestInterceptor)interceptor
                 errorHandler:(PDClientResponseErrorHandler)errorHandler;
@end
