//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//


@interface PDStruct : NSObject <NSCopying, NSCoding>
+ (NSDictionary *)properties;

- (BOOL)isEqual:(id)other;

- (BOOL)isEqualToStruct:(PDStruct *)other;

- (NSUInteger)hash;
@end
