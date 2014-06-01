//
// Created by Ivan Korobkov on 27.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class PDInterface;
@class RACSignal;


@interface PDServer : NSObject
@property (nonatomic, readonly) PDInterface *iface;
- (instancetype)initWithInterface:(PDInterface *)iface;

- (RACSignal *)handleRequest:(NSURLRequest *)request;
- (id)parseEnum:(Class)type string:(NSString *)string error:(NSError **)error;
@end
