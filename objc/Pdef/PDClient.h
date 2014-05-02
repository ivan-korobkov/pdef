//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class AFHTTPSessionManager;
@class RACSignal;


typedef id (^PDClientErrorHandler)(NSData *data, NSHTTPURLResponse *response, NSError **error);


@interface PDClient : NSObject
@property(nonatomic, readonly) Class iface;
@property(nonatomic, readonly) NSString *url;
@property(nonatomic, readonly) NSURLSession *session;
@property(nonatomic, readonly, copy) PDClientErrorHandler errorHandler;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session
                     errorHandler:(PDClientErrorHandler)errorHandler;

- (RACSignal *)handle:(NSArray *)invocations;
@end


/** Internal client request, visible for testing. */
@interface PDClientRequest : NSObject
@end
