//
// Created by Ivan Korobkov on 02.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>

@class PDMethod;

@interface NSInvocation (PDef)
- (NSArray *)pdef_argumentsForMethod:(PDMethod *)method;

- (NSArray *)pdef_argumentsForTypes:(NSArray *)types;
@end
