/*
 * Copyright (C) 2012  Manatee Works, Inc.
 *
 */

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreGraphics/CoreGraphics.h>
#import <CoreVideo/CoreVideo.h>
#import <CoreMedia/CoreMedia.h>

@protocol ScanningFinishedDelegate <NSObject>
    - (void)scanningFinished:(NSString *)result withType: (NSString *) lastFormat isGS1: (bool) isGS1 andRawResult: (NSData *) rawResult closeRequested: (BOOL) closeRequested;
@end


@class MWResult;

@interface DecoderResult : NSObject {
    BOOL succeeded;
    MWResult *mwResult;
}

@property (nonatomic, assign) BOOL succeeded;
@property (nonatomic, retain) MWResult *mwResult;


+(DecoderResult *)createSuccess:(MWResult *)result;
+(DecoderResult *)createFailure;

@end


typedef enum eCameraState {
	NORMAL,
	LAUNCHING_CAMERA,
	CAMERA,
	CAMERA_DECODING,
    CAMERA_PAUSED,
	DECODE_DISPLAY,
	CANCELLING
} CameraState;


@interface MWScannerViewController : UIViewController<AVCaptureVideoDataOutputSampleBufferDelegate,UINavigationControllerDelegate, UIAlertViewDelegate>{
    
    IBOutlet UIImageView *cameraOverlay;
    IBOutlet UIButton *closeButton;
    IBOutlet UIButton *flashButton;
    IBOutlet UIButton *zoomButton;
    
    float firstZoom;
    float secondZoom;
    BOOL videoZoomSupported;
    CGRect unionRect;
    
}




@property (nonatomic, retain) AVCaptureSession *captureSession;
@property (nonatomic, retain) AVCaptureVideoPreviewLayer *prevLayer;
@property (nonatomic, retain) AVCaptureDevice *device;
@property (nonatomic, retain) NSTimer *focusTimer;
@property (nonatomic, retain) id <ScanningFinishedDelegate> delegate;


- (IBAction)doClose:(id)sender;
+ (void) setUseHiRes: (BOOL) hiRes;
+ (void) setUseBlinkingLineOverlay: (BOOL) blinkingOverlay;
+ (void) setOverlayImage: (UIImage *) image;
+ (void) setCloseButtonImage: (UIImage *) image;
+ (void) setFlashButtonImages: (UIImage *) imageOn imageOff: (UIImage *) imageOff;
+ (void) setZoomImage: (UIImage *) zImage;
+ (void) setButtonsVisible: (BOOL) flashButtonVisible closeButtonVisible: (BOOL) closeButtonVisible;
+ (void) setCloseCameraOnDetection: (BOOL) closeOnDetection;
+ (void) resumeScanning;
- (void)revertToNormal;
- (void)decodeResultNotification: (NSNotification *)notification;
- (void)initCapture;
- (void) startScanning;
- (void) stopScanning;
- (void) toggleTorch;
+ (void) enableZoom: (BOOL) zoom;
+ (void) setMaxThreads: (int) maxThreads;
+ (void) setZoomLevels: (int) zoomLevel1 zoomLevel2: (int) zoomLevel2 initialZoomLevel: (int) initialZoomLevel;


+ (void) updateScanningRects: (NSMutableArray *) newScanningRects;
+ (void) updateActiveCodes: (int) newActiveCodes;
+ (void) updateOrientation: (int) newOrientation;

@end