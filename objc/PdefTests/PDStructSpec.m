//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#define MOCKITO_SHORTHAND
#define EXP_SHORTHAND

#import <OCMockito/OCMockito.h>
#import "Specta.h"
#import "Expecta.h"
#import "PDTestStruct.h"


PDTestStruct *createStruct() {
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
};


SpecBegin(PDStruct)
        describe(@"NSCoding", ^{
            it(@"should encode and decode the structs", ^() {
                PDTestStruct *s0 = createStruct();
                NSData *data = [NSKeyedArchiver archivedDataWithRootObject:s0];
                PDTestStruct *s1 = [NSKeyedUnarchiver unarchiveObjectWithData:data];

                expect(s1).to.equal(s0);
            });
        });

        describe(@"NSCopying", ^{
            it(@"should return a deep copy of a struct", ^() {
                PDTestStruct *s0 = createStruct();
                PDTestStruct *s1 = [s0 copy];

                expect(s1).to.equal(s0);
            });
        });

        describe(@"equalTo", ^() {
            it(@"should compare a struct to another", ^() {
                PDTestStruct *s0 = createStruct();
                PDTestStruct *s1 = createStruct();

                expect(s0).to.equal(s0);
                expect(s0).to.equal(s1);

                s1.bool0 = !s1.bool0;
                expect(s0).notTo.equal(s1);
            });
        });

        describe(@"hash", ^() {
            it(@"should return a struct hash code", ^() {
                PDTestStruct *s0 = createStruct();
                PDTestStruct *s1 = createStruct();

                expect(s0.hash).to.equal(s0.hash);
                expect(s0.hash).to.equal(s1.hash);

                s1.bool0 = !s1.bool0;
                expect(s0.hash).notTo.equal(s1.hash);
            });
        });
        SpecEnd
