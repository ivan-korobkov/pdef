//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//


@interface PDInterface : NSObject
+ (NSArray *)methods;
@end


typedef NS_ENUM(NSInteger, PDMethodOptions) {
    PDMethodGet = 1 << 0,
    PDMethodPost = 1 << 2,
    PDMethodRequest = 1 << 3
};


@interface PDMethod : NSObject
@property(nonatomic, readonly) SEL selector;
@property(nonatomic, readonly) NSString *name;
@property(nonatomic, readonly) PDMethodOptions options;

@property(nonatomic, readonly) id result;
@property(nonatomic, readonly) NSArray *paramNames;
@property(nonatomic, readonly) NSArray *paramTypes;

@property(nonatomic, readonly, getter=isGet) BOOL get;
@property(nonatomic, readonly, getter=isPost) BOOL post;
@property(nonatomic, readonly, getter=isRequest) BOOL request;

+ (PDMethod *)methodWithSelector:(SEL)selector name:(NSString *)name
                         options:(PDMethodOptions)options result:(id)result
                      paramNames:(NSArray *)names paramTypes:(NSArray *)types;
@end
