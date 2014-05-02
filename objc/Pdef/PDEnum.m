//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <objc/runtime.h>
#import "PDEnum.h"


@implementation PDEnum

// Implement in a subclass.
+ (NSDictionary *)valuesToNames {
    return nil;
}

// Implement in a subclass.
+ (NSDictionary *)namesToValues {
    return nil;
}

+ (NSInteger)enumValueForName:(NSString *)caseInsensitiveName {
    if (!caseInsensitiveName) {
        return 0;
    }

    NSDictionary *dict = [self namesToValues];
    NSNumber *value = dict[caseInsensitiveName];
    if (value) {
        return value.integerValue;
    }

    NSString *upper = [caseInsensitiveName uppercaseString];
    for (NSString *key in [dict allKeys]) {
        if ([[key uppercaseString] isEqualToString:upper]) {
            value = dict[key];
            return value.integerValue;
        }
    }

    return 0;
}

+ (NSString *)nameForEnumValue:(NSInteger)value {
    return [self valuesToNames][@(value)];
}
@end
