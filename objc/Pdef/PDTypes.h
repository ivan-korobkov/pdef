//
// Created by Ivan Korobkov on 30.04.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>


typedef NS_ENUM(int, PDType) {
    PDTypeUndefined = 0,
    PDTypePrimitive = 1,
    PDTypeList = 2,
    PDTypeSet = 3,
    PDTypeMap = 4,
    PDTypeEnum = 5,
    PDTypeStruct = 6,
    PDTypeInterface =7
};


/** Primitive pdef types. */
typedef NS_ENUM(int, PDPrimitive) {
    PDPrimitiveBool = 1,
    PDPrimitiveInt16 = 2,
    PDPrimitiveInt32 = 3,
    PDPrimitiveInt64 = 4,
    PDPrimitiveFloat = 5,
    PDPrimitiveDouble = 6,
    PDPrimitiveString = 7,
    PDPrimitiveDate = 8,
    PDPrimitiveVoid = 9
};


PDType PDTypeForType(id type);


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
