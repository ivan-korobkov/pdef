//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface PDJson : NSObject
+ (id)parse:(NSData *)data type:(id)type error:(NSError **)error;

+ (NSData *)serialize:(id)object type:(id)type options:(NSJSONWritingOptions)options
                error:(NSError **)error;
@end
