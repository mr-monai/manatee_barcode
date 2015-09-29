/**
 * Your Copyright Here
 *
 * Appcelerator Titanium is Copyright (c) 2009-2010 by Appcelerator, Inc.
 * and licensed under the Apache Public License (version 2)
 */
#import "TiModule.h"
#import "MWScannerViewController.h"

@interface ComManateeworksBarcodescannerModule : TiModule <ScanningFinishedDelegate>
{
    MWScannerViewController *controller;
    
    KrollCallback *successCallback;
    KrollCallback *errorCallback;
    KrollCallback *cancelCallback;
    
    NSMutableArray *scanningRects;
}


@end
