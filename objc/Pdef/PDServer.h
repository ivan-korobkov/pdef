//
// Created by Ivan Korobkov on 27.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class PDInterface;
@class RACSignal;


@interface PDServer : NSObject
@property(nonatomic, readonly) PDInterface *iface;
- (instancetype)initWithInterface:(PDInterface *)iface;

// Returns RACSignal<Method result> or RACSignal<NSError>.
- (RACSignal *)resultForRequest:(NSURLRequest *)request resultType:(id *)resultType;
@end
