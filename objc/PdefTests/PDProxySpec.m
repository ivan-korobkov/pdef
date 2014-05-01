//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#define MOCKITO_SHORTHAND
#define EXP_SHORTHAND

#import <OCMockito/OCMockito.h>
#import <Expecta/Expecta.h>
#import "Specta.h"
#import "PDTestInterface.h"
#import "PDProxy.h"
#import "PDTestSubInterface.h"
#import "RACSignal.h"


SpecBegin(PDProxy)
        __block PDTestInterface *proxy;

        beforeEach(^() {
            proxy = (id) [PDProxy proxyForClass:PDTestInterface.class withHandler:
                ^RACSignal *(NSArray *invocations) {
                    return nil;
                }];
        });

        describe(@"forwardInvocation", ^() {
            it(@"should return a new proxy when the result is a pdef interface", ^() {
                PDTestSubInterface *subface = [proxy interface0Bool0:YES int0:-16 string0:@"Hello"];

                Class cls = subface.class;
                expect(cls).to.beIdenticalTo(PDProxy.class);
            });

            it(@"should return the handler result when a method returns a RAC signal", ^() {
                PDProxyInvocationHandler handler = ^RACSignal *(NSArray *invocations) {
                    return [RACSignal return:@"hello"];
                };

                PDTestInterface *iface = (id) [PDProxy proxyForClass:PDTestInterface.class
                    withHandler:handler];
                PDTestSubInterface *subface = [iface interface0Bool0:YES int0:32 string0:@"hello"];
                RACSignal *signal = [subface getInt0:100 string0:@"world"];

                __block NSString *result;
                [signal subscribeNext:^(id x) {
                    result = x;
                }];
                expect(result).to.equal(@"hello");
            });
        });

        describe(@"respondsToSelector", ^() {
            it(@"should respond to the proxied class selectors", ^() {
                BOOL responds = [proxy respondsToSelector:@selector(interface0Bool0:int0:string0:)];
                expect(responds).to.beTruthy;
            });
        });
        SpecEnd
