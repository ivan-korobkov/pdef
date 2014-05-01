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
#import "PDTestStruct+Tests.h"


SpecBegin(PDStruct)
        describe(@"NSCoding", ^{
            it(@"should encode and decode the structs", ^() {
                PDTestStruct *s0 = [PDTestStruct createFixture];
                NSData *data = [NSKeyedArchiver archivedDataWithRootObject:s0];
                PDTestStruct *s1 = [NSKeyedUnarchiver unarchiveObjectWithData:data];

                expect(s1).to.equal(s0);
            });
        });

        describe(@"NSCopying", ^{
            it(@"should return a deep copy of a struct", ^() {
                PDTestStruct *s0 = [PDTestStruct createFixture];
                PDTestStruct *s1 = [s0 copy];

                expect(s1).to.equal(s0);
            });
        });

        describe(@"equalTo", ^() {
            it(@"should compare a struct to another", ^() {
                PDTestStruct *s0 = [PDTestStruct createFixture];
                PDTestStruct *s1 = [PDTestStruct createFixture];

                expect(s0).to.equal(s0);
                expect(s0).to.equal(s1);

                s1.bool0 = !s1.bool0;
                expect(s0).notTo.equal(s1);
            });
        });

        describe(@"hash", ^() {
            it(@"should return a struct hash code", ^() {
                PDTestStruct *s0 = [PDTestStruct createFixture];
                PDTestStruct *s1 = [PDTestStruct createFixture];

                expect(s0.hash).to.equal(s0.hash);
                expect(s0.hash).to.equal(s1.hash);

                s1.bool0 = !s1.bool0;
                expect(s0.hash).notTo.equal(s1.hash);
            });
        });
        SpecEnd
