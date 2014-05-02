//
// Created by Ivan Korobkov on 02.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import "NSInvocation+PDef.h"
#import "PDInterface.h"
#import "PDTypes.h"


@implementation NSInvocation (PDef)
- (NSArray *)pdef_argumentsForMethod:(PDMethod *)method {
    return [self pdef_argumentsForTypes:method.paramTypes];
}

- (NSArray *)pdef_argumentsForTypes:(NSArray *)types {
    NSMutableArray *args = [NSMutableArray array];

    // From docs:
    // Indices 0 and 1 indicate the hidden arguments self and _cmd, respectively;
    // Use indices 2 and greater for the arguments normally passed in a message.

    for (uint j = 2; j < types.count + 2; j++) {
        id value;
        id type = types[j - 2];
        PDType type0 = PDTypeForType(type);

        switch (type0) {
            case PDTypeBool: {
                BOOL v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }
            case PDTypeInt16: {
                int16_t v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }
            case PDTypeInt32: {
                int32_t v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }
            case PDTypeInt64: {
                int64_t v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }
            case PDTypeFloat: {
                float v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }
            case PDTypeDouble: {
                double v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }
            case PDTypeString: {
                [self getArgument:&value atIndex:j];
                break;
            }
            case PDTypeDate: {
                [self getArgument:&value atIndex:j];
                break;
            }
            case PDTypeEnum: {
                NSInteger v;
                [self getArgument:&v atIndex:j];
                value = @(v);
                break;
            }

            case PDTypeList:
            case PDTypeSet:
            case PDTypeMap:
            case PDTypeStruct: {
                [self getArgument:&value atIndex:j];
                break;
            }

            default: {
                [self raiseIllegalArgumentType:type];
            }
        }

        [args addObject:value];
    }

    return args;
}

- (void)raiseIllegalArgumentType:(id)type {
    [NSException raise:@"IllegalArgumentTypeException"
        format:@"Cannot get an invocation argument of pdef type '%@'", type];
}
@end
