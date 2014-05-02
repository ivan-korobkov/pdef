//
// Created by Ivan Korobkov on 02.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#define MOCKITO_SHORTHAND
#define EXP_SHORTHAND

#import <OCMockito/OCMockito.h>
#import <Expecta/Expecta.h>
#import <ReactiveCocoa/ReactiveCocoa/RACSignal.h>
#import "Specta.h"
#import "PDClient.h"
#import "PDProxy.h"
#import "PDTestInterface.h"
#import "PDTestSubInterface.h"
#import "PDTestStruct.h"
#import "PDTestStruct+Tests.h"


@interface PDClientRequest (Tests)
@property(nonatomic) NSString *method;
@property(nonatomic) NSString *path;
@property(nonatomic) NSString *post;
@end


@interface PDClient (Tests)
+ (PDClientRequest *)serializeInvocations:(NSArray *)invocations iface:(Class)iface
                               resultType:(id *)resultType error:(NSError **)error;
@end


NSDictionary *parsePathQuery(NSString *path) {
    NSString *query = path;

    NSRange range = [path rangeOfString:@"?"];
    if (range.location != NSNotFound) {
        query = [path substringFromIndex:range.location + 1]; // Mind "?"
    }

    NSMutableDictionary *d = [NSMutableDictionary dictionary];
    NSArray *keyValues = [query componentsSeparatedByString:@"&"];
    for (NSString *keyValue in keyValues) {
        NSArray *array = [keyValue componentsSeparatedByString:@"="];
        d[array[0]] = array[1];
    }

    return d;
}

SpecBegin(PDClient)
        describe(@"serializeInvocations", ^() {
            it(@"should serialize invocations", ^() {
                __block NSArray *invocations;
                PDTestInterface *iface = (id) [PDProxy proxyForClass:PDTestInterface.class
                    withHandler:^RACSignal *(NSArray *invs) {
                        invocations = invs;
                        return nil;
                    }];

                [[iface interface0Bool0:YES int0:-32 string0:@"hello/world"]
                    getInt0:32 string0:@"good-bye"];

                id resultType;
                NSError *error;
                PDClientRequest *request = [PDClient serializeInvocations:invocations
                    iface:PDTestInterface.class resultType:&resultType error:&error];

                expect(error).to.beNil;
                expect(resultType).to.equal(@(PDTypeInt32));

                NSDictionary *q = parsePathQuery(request.path);
                expect([request.path hasPrefix:@"interface0/1/-32/hello%2Fworld/get?"]).to.beTruthy;
                expect(q).to.equal(@{
                    @"int0" : @"32",
                    @"string0" : @"good-bye"
                });
                expect(request.method).to.equal(@"GET");
                expect(request.post).to.beNil;
            });

            it(@"should serialize request method invocations", ^() {
                __block NSArray *invocations;
                PDTestInterface *iface = (id) [PDProxy proxyForClass:PDTestInterface.class
                    withHandler:^RACSignal *(NSArray *invs) {
                        invocations = invs;
                        return nil;
                    }];

                PDTestStruct *struct0 = [PDTestStruct createFixture];
                [iface requestTestStruct:struct0];

                id resultType;
                NSError *error;
                PDClientRequest *request = [PDClient serializeInvocations:invocations
                    iface:PDTestInterface.class resultType:&resultType error:&error];

                expect(error).to.beNil;
                expect(resultType).to.equal(PDTestStruct.class);

                expect([request.path hasPrefix:@"request?"]).to.beTruthy;
                expect(request.method).to.equal(@"GET");
                expect(request.post).to.beNil;
            });

            it(@"should serialize post method invocations", ^() {
                __block NSArray *invocations;
                PDTestInterface *iface = (id) [PDProxy proxyForClass:PDTestInterface.class
                    withHandler:^RACSignal *(NSArray *invs) {
                        invocations = invs;
                        return nil;
                    }];

                [[iface interface0Bool0:NO int0:-32 string0:@"world"]
                    postInt0:32 string0:@"привет"];

                id resultType;
                NSError *error;
                PDClientRequest *request = [PDClient serializeInvocations:invocations
                    iface:PDTestInterface.class resultType:&resultType error:&error];

                expect(error).to.beNil;
                expect(resultType).to.equal(@(PDTypeInt32));

                NSDictionary *query = parsePathQuery(request.post);
                expect(request.path).to.equal(@"interface0/0/-32/world/post");
                expect(request.method).to.equal(@"POST");
                expect(query).to.equal(@{
                    @"int0" : @"32",
                    @"string0" : @"%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82"
                });
            });
        });
        SpecEnd
