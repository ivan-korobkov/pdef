//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import "PDTestStruct+Tests.h"


@implementation PDTestStruct (Tests)
+ (PDTestStruct *)createFixture {
    PDTestStruct *s = [[PDTestStruct alloc] init];
    s.bool0 = YES;
    s.short0 = -16;
    s.int0 = -32;
    s.long0 = -64;
    s.float0 = -1.5;
    s.double0 = -2.5;
    s.string0 = @"Hello";
    s.datetime0 = [NSDate dateWithTimeIntervalSince1970:1];

    s.list0 = @[@(1), @(2), @(3)];
    s.set0 = [NSSet setWithObjects:@(4), @(5), @(6), nil];
    s.map0 = @{@(1) : @"a", @(2): @"b"};

    s.enum0 = PDTestNumber_ONE;
    s.struct0 = [[PDTestStruct alloc] init];
    s.struct0.int0 = 32;
    return s;
}
@end