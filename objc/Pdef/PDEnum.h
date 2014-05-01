//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//


/** Enum type. */
@interface PDEnum : NSObject
+ (NSDictionary *)valuesToNames;    // NSDictionary<NSInteger, NSString>

+ (NSDictionary *)namesToValues;    // NSDictionary<NSString, NSInteger>

+ (NSInteger)enumValueForName:(NSString *)caseInsensitiveName;

+ (NSString *)nameForEnumValue:(NSInteger)value;
@end
