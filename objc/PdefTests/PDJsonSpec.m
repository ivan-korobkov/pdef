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
#import "PDJson.h"
#import "PDTestStruct+Tests.h"
#import "PDError.h"


SpecBegin(PDJson)
        describe(@"fragment", ^{
            it(@"should parse/serialize a boolean", ^() {
                NSError *error = nil;
                NSData *data = [PDJson serialize:@YES type:@(PDPrimitiveBool)
                    options:NSJSONWritingPrettyPrinted error:&error];

                expect(data).notTo.beNil;
                expect(error).to.beNil;

                NSNumber *result = [PDJson parseJson:data type:@(PDPrimitiveBool) error:&error];
                expect(result).notTo.beNil;
                expect(error).to.beNil;

                expect(result.boolValue).to.equal(@YES);
            });
        });

        describe(@"struct", ^{
            it(@"should parse/serialize a struct", ^() {
                NSError *error = nil;
                PDTestStruct *s0 = [PDTestStruct createFixture];
                NSData *data = [PDJson serialize:s0 type:PDTestStruct.class
                    options:NSJSONWritingPrettyPrinted error:&error];

                expect(data).notTo.beNil;
                expect(error).to.beNil;

                PDTestStruct *s1 = [PDJson parseJson:data type:PDTestStruct.class error:&error];
                expect(s1).notTo.beNil;
                expect(error).to.beNil;

                expect(s1).to.equal(s0);
            });
        });

        describe(@"parse error", ^() {
            it(@"should return an error", ^() {
                NSString *s = @"{\"bool0\": [], \"int0\": {}}";
                NSData *data = [s dataUsingEncoding:NSUTF8StringEncoding];

                NSError *error;
                PDTestStruct *result = [PDJson parseJson:data type:PDTestStruct.class error:&error];

                expect(result).to.beNil;
                expect(error.domain).to.equal(PDErrorDomain);
                expect(error.code).to.equal(PDErrorJsonError);
            });
        });
        SpecEnd
