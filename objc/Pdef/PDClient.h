//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class RACSignal;
@class PDClientRequest;
@protocol PDClientDelegate;


@interface PDClient : NSObject
@property(nonatomic, readonly) Class iface;
@property(nonatomic, readonly) NSString *url;
@property(nonatomic, readonly) NSURLSession *session;
@property(nonatomic, readonly, weak) id <PDClientDelegate> delegate;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session;

- (instancetype)initWithInterface:(Class)iface url:(NSString *)url session:(NSURLSession *)session
                         delegate:(id <PDClientDelegate>)delegate;

- (RACSignal *)handle:(NSArray *)invocations;
@end


typedef RACSignal *(^PDClientRequestHandler)(NSURLRequest *request);

typedef id (^PDClientResponseHandler)(NSHTTPURLResponse *response, NSData *data, NSError **error);


@protocol PDClientDelegate <NSObject>
@optional
- (RACSignal *)handleRequest:(NSURLRequest *)request nextHandler:(PDClientRequestHandler)handler;

- (id)handleResponse:(NSHTTPURLResponse *)response data:(NSData *)data error:(NSError **)error
         nextHandler:(PDClientResponseHandler)handler;
@end
