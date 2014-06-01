//
// Created by Ivan Korobkov on 01.05.14.
// Copyright (c) 2014 io.pdef. All rights reserved.
//

#import <Foundation/Foundation.h>


static NSString *const PDErrorDomain = @"PdefError";


typedef NS_ENUM(int, PDErrorCode) {
    PDErrorJsonError = 1,
    PDErrorClientError = 2,
    PDErrorServerInternalError = 3,
    PDErrorServerBadRequest = 4,
};
