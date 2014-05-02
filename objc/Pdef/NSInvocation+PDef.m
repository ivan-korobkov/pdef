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
            case PDTypePrimitive: {
                PDPrimitive primitive = (PDPrimitive) ((NSNumber *) type).intValue;

                switch (primitive) {
                    case PDPrimitiveBool: {
                        BOOL v;
                        [self getArgument:&v atIndex:j];
                        value = @(v);
                        break;
                    }

                    case PDPrimitiveInt16: {
                        int16_t v;
                        [self getArgument:&v atIndex:j];
                        value = @(v);
                        break;
                    }

                    case PDPrimitiveInt32: {
                        int32_t v;
                        [self getArgument:&v atIndex:j];
                        value = @(v);
                        break;
                    }

                    case PDPrimitiveInt64: {
                        int64_t v;
                        [self getArgument:&v atIndex:j];
                        value = @(v);
                        break;
                    }

                    case PDPrimitiveFloat: {
                        float v;
                        [self getArgument:&v atIndex:j];
                        value = @(v);
                        break;
                    }

                    case PDPrimitiveDouble: {
                        double v;
                        [self getArgument:&v atIndex:j];
                        value = @(v);
                        break;
                    }

                    case PDPrimitiveString: {
                        [self getArgument:&value atIndex:j];
                        break;
                    }

                    case PDPrimitiveDate: {
                        [self getArgument:&value atIndex:j];
                        break;
                    }

                    default: {
                        [self raiseIllegalArgumentType:type];
                    }
                }
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
            case PDTypeStruct:
            case PDTypeInterface: {
                [self getArgument:&value atIndex:j];
                break;
            }
            default:
                [self raiseIllegalArgumentType:type];
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
