//
// Created by Ivan Korobkov on 30.04.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>


/** Primitive pdef types. */
typedef NS_ENUM(int, PDType) {
    PDTypeBool = 1,
    PDTypeInt16 = 2,
    PDTypeInt32 = 3,
    PDTypeInt64 = 4,
    PDTypeFloat = 5,
    PDTypeDouble = 6,
    PDTypeString = 7,
    PDTypeDate = 8,
    PDTypeVoid = 9
};


/** Generic list type. */
@interface PDList : NSObject
@property(nonatomic, readonly) id item;

+ (id)listWithItem:(id)item;
@end


/** Generic set type. */
@interface PDSet : NSObject
@property(nonatomic, readonly) id item;

+ (id)setWithItem:(id)item;
@end


/** Generic map type. */
@interface PDMap : NSObject
@property(nonatomic, readonly) id key;
@property(nonatomic, readonly) id value;

+ (id)mapWithKey:(id)key value:(id)value;
@end


/** Enum type. */
@interface PDEnum : NSObject
+ (NSDictionary *)valuesToNames;    // NSDictionary<NSInteger, NSString>
@end


@interface PDStruct : NSObject
+ (NSDictionary *)properties;
@end


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
