//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class PDStruct;


/**
 * Parses/serializes pdef types.
 * Type is a PDType, a pdef collection, an pdef enum class, or a pdef struct class
 */
@interface PDJson : NSObject

+ (id)parse:(NSData *)data type:(id)type error:(NSError **)error;

/** Parses json data and merges it into a struct. */
+ (void)parseStruct:(PDStruct *)aStruct fromData:(NSData *)data error:(NSError **)error;

+ (NSData *)serialize:(id)object type:(id)type options:(NSJSONWritingOptions)options
                error:(NSError **)error;

+ (NSData *)serializeStruct:(PDStruct *)aStruct options:(NSJSONWritingOptions)options
                      error:(NSError **)error;
@end
