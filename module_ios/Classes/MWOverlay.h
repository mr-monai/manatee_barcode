//
//  MWOverlay.h
//  mobiscan_ALL
//
//  Created by vladimir zivkovic on 12/12/13.
//  Copyright (c) 2013 Manatee Works. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

@interface MWOverlay : NSObject 

+ (void) addToPreviewLayer:(AVCaptureVideoPreviewLayer *) videoPreviewLayer;
+ (void) removeFromPreviewLayer;
+ (void) updateOverlay;

//customization of overlay
+ (void) setViewportVisible: (BOOL) value;
+ (void) setBlinkingLineVisible: (BOOL) value;
+ (void) setViewportLineWidth: (float) value;
+ (void) setBlinkingLineWidth: (float) value;
+ (void) setViewportAlpha: (float) value;
+ (void) setViewportLineAlpha: (float) value;
+ (void) setBlinkingLineAlpha: (float) value;
+ (void) setBlinkingSpeed: (float) value;
+ (void) setViewportLineRGBColor: (int) value;
+ (void) setBlinkingLineRGBColor: (int) value;
+ (void) setViewportLineUIColor: (UIColor*) value;
+ (void) setBlinkingLineUIColor: (UIColor*) value;

@end
