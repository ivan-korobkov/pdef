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
