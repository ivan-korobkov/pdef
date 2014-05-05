//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class RACSignal;
@class PDClientRequest;


typedef RACSignal *(^PDClientRequestHandler)(NSURLRequest *request);

typedef RACSignal *(^PDClientRequestInterceptor)(NSURLRequest *request, PDClientRequestHandler handler);

typedef void (^PDClientResponseErrorHandler)(NSData *data, NSHTTPURLResponse *response, NSError **error);


@interface PDClient : NSObject
@property(nonatomic, readonly) Class iface;
@property(nonatomic, readonly) NSString *url;
@property(nonatomic, readonly) NSURLSession *session;
@property(nonatomic, readonly, copy) PDClientResponseErrorHandler errorHandler;
@property(nonatomic, readonly, copy) PDClientRequestInterceptor interceptor;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session
                      interceptor:(PDClientRequestInterceptor)interceptor
                     errorHandler:(PDClientResponseErrorHandler)errorHandler;

- (RACSignal *)handle:(NSArray *)invocations;
@end


/** Internal client request, visible for testing. */
@interface PDClientRequest : NSObject
@property(nonatomic) NSString *method;
@property(nonatomic) NSString *path;
@property(nonatomic) NSString *post;
@end
