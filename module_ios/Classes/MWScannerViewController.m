/*
 * Copyright (C) 2012  Manatee Works, Inc.
 *
 */

#import "MWScannerViewController.h"
#import "BarcodeScanner.h"
#import "MWOverlay.h"
#import "MWResult.h"
#include <mach/mach_host.h>

// !!! Rects are in format: x, y, width, height !!!
#define RECT_LANDSCAPE_1D       4, 20, 92, 60
#define RECT_LANDSCAPE_2D       20, 5, 60, 90
#define RECT_PORTRAIT_1D        20, 4, 60, 92
#define RECT_PORTRAIT_2D        20, 5, 60, 90
#define RECT_FULL_1D            4, 4, 92, 92
#define RECT_FULL_2D            20, 5, 60, 90
#define RECT_DOTCODE            30, 20, 40, 60

static NSString *DecoderResultNotification = @"DecoderResultNotification";

BOOL useHiRes = NO;
UIImage *overlayImage = nil;
UIImage *closeButtonImage = nil;
UIImage *flashButtonImageOn = nil;
UIImage *flashButtonImageOff = nil;
UIImage *zoomImage = nil;


NSMutableArray *scanningRects;
int orientation;
int activeCodes;

BOOL useBLinkingLineOverlay = YES;

BOOL closeCameraOnDetection = YES;
BOOL param_EnableZoom = YES;

BOOL flashVisible = YES;
BOOL closeVisible = YES;
int param_ZoomLevel1 = 0;
int param_ZoomLevel2 = 0;
int zoomLevel = 0;
int param_maxThreads = 4;
int activeThreads = 0;
int availableThreads = 0;

CameraState state;


@implementation MWScannerViewController {
    AVCaptureSession *_captureSession;
	AVCaptureDevice *_device;
	UIImageView *_imageView;
	CALayer *_customLayer;
	AVCaptureVideoPreviewLayer *_prevLayer;
	bool running;
    NSString * lastFormat;
	
	
	CGImageRef	decodeImage;
	NSString *	decodeResult;
	int width;
	int height;
	int bytesPerRow;
	unsigned char *baseAddress;
    NSTimer *focusTimer;
}

@synthesize captureSession = _captureSession;
@synthesize prevLayer = _prevLayer;
@synthesize device = _device;
@synthesize focusTimer;

#pragma mark -
#pragma mark Initialization


- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    
    closeButton.frame = CGRectMake( self.view.frame.size.width - 42,10,32,32);
    cameraOverlay.frame = CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height);
    zoomButton.hidden = !param_EnableZoom;

    [self startCamera];
    [self setTorchEnabled:NO];

    
    if (flashVisible){
        [flashButton setHidden: NO];
    } else {
        [flashButton setHidden: YES];
    }
    
    if (closeVisible){
        [closeButton setHidden: NO];
    } else {
        [closeButton setHidden: YES];
    }
    
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self startScanning];
    if (useBLinkingLineOverlay){
        [MWOverlay addToPreviewLayer:self.prevLayer];
        [cameraOverlay setHidden:YES];
    } else {
        [cameraOverlay setHidden:NO];

        [cameraOverlay setImage: overlayImage];
    }
    
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [self setTorchEnabled:NO];
    [MWOverlay removeFromPreviewLayer];
    
    [self stopCamera];
    [self deinitCapture];
    
}

// IOS 7 statusbar hide
- (BOOL)prefersStatusBarHidden
{
    return YES;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    
    
    self.prevLayer = nil;
    [[NSNotificationCenter defaultCenter] addObserver: self selector:@selector(decodeResultNotification:) name: DecoderResultNotification object: nil];
   
    

    
    cameraOverlay = [[UIImageView alloc] initWithImage:overlayImage];
    [cameraOverlay setHidden:YES];
    
    closeButton = [UIButton buttonWithType:UIButtonTypeCustom];
    
    [closeButton setImage:closeButtonImage forState:UIControlStateNormal];
    [closeButton addTarget:self action:@selector(doClose:) forControlEvents:UIControlEventTouchUpInside];
    closeButton.userInteractionEnabled = YES;
    
    
    flashButton = [UIButton buttonWithType:UIButtonTypeCustom];
    flashButton.frame = CGRectMake(10,10,32,32);
    [flashButton setImage:flashButtonImageOn forState:UIControlStateSelected];
    [flashButton setImage:flashButtonImageOff forState:UIControlStateNormal];
    [flashButton addTarget:self action:@selector(toggleTorch) forControlEvents:UIControlEventTouchUpInside];
    flashButton.userInteractionEnabled = YES;
    
    [flashButton setHidden:NO];
    
    
    zoomButton = [UIButton buttonWithType:UIButtonTypeCustom];
    zoomButton.frame = CGRectMake(self.view.frame.size.width-42,self.view.frame.size.height-42,32,32);
    
    [zoomButton setImage:zoomImage forState:UIControlStateNormal];
    [zoomButton addTarget:self action:@selector(doZoomToggle:) forControlEvents:UIControlEventTouchUpInside];
    zoomButton.userInteractionEnabled = YES;
    
    [zoomButton setHidden:NO];
    
    
    [self.view addSubview:cameraOverlay];
    [self.view addSubview:closeButton];
    [self.view addSubview:flashButton];
    [self.view addSubview:zoomButton];
    
    
    
}

- (void) startCamera {
    
#if TARGET_IPHONE_SIMULATOR
    NSLog(@"On iOS simulator camera is not Supported");
#else
	[self initCapture];
#endif
    
}

- (void) stopCamera {
    
    [self.focusTimer invalidate];
    
    [self.captureSession stopRunning];
    
}
+ (void) enableZoom: (BOOL) zoom {
    param_EnableZoom = zoom;
    
}

+ (void) setMaxThreads: (int) maxThreads {
    
        NSLog(@"setMaxThreads: %d",maxThreads);
    if (availableThreads == 0){
        host_basic_info_data_t hostInfo;
        mach_msg_type_number_t infoCount;
        infoCount = HOST_BASIC_INFO_COUNT;
        host_info( mach_host_self(), HOST_BASIC_INFO, (host_info_t)&hostInfo, &infoCount ) ;
        availableThreads = hostInfo.max_cpus;
    }
    
    
    param_maxThreads = maxThreads;
    if (param_maxThreads > availableThreads){
        param_maxThreads = availableThreads;
    }
    
    
    
}


+ (void) setOverlayImage: (UIImage *) image {
    
    overlayImage = image;
    
}

+ (void) setCloseButtonImage: (UIImage *) image {
    
    closeButtonImage = image;
    
}

+ (void) setFlashButtonImages: (UIImage *) imageOn imageOff: (UIImage *) imageOff {
    
    flashButtonImageOn = imageOn;
    flashButtonImageOff = imageOff;
    
}
+ (void) setZoomImage: (UIImage *) zImage  {

    zoomImage = zImage;
    
}

+ (void) setButtonsVisible: (BOOL) flashButtonVisible closeButtonVisible: (BOOL) closeButtonVisible {
    
    flashVisible = flashButtonVisible;
    closeVisible = closeButtonVisible;
}

+ (void) setCloseCameraOnDetection: (BOOL) closeOnDetection {
    
    closeCameraOnDetection = closeOnDetection;
}

+ (void) resumeScanning {
    
    state = CAMERA;
    
}


-(void) reFocus {
   //NSLog(@"refocus");

    NSError *error;
    if ([self.device lockForConfiguration:&error]) {
        
        if ([self.device isFocusPointOfInterestSupported]){
            [self.device setFocusPointOfInterest:CGPointMake(0.49,0.49)];
            [self.device setFocusMode:AVCaptureFocusModeAutoFocus];
        }
        [self.device unlockForConfiguration];
        
    }
}


- (void)setTorchEnabled: (BOOL) enabled
{
    if ([self.device isTorchModeSupported:AVCaptureTorchModeOn]) {
        NSError *error;
        
        if ([self.device lockForConfiguration:&error]) {
            
            if (enabled){
                [self.device setTorchMode:AVCaptureTorchModeOn];
                flashButton.selected = YES;
            } else {
                [self.device setTorchMode:AVCaptureTorchModeOff];
                flashButton.selected = NO;
            }
            
            [self.device unlockForConfiguration];
        } else {
            
        }
    }
}

- (void)toggleTorch
{
    if ([self.device isTorchModeSupported:AVCaptureTorchModeOn]) {
        NSError *error;
        
        if ([self.device lockForConfiguration:&error]) {
            if ([self.device torchMode] == AVCaptureTorchModeOn){
                [self.device setTorchMode:AVCaptureTorchModeOff];
                flashButton.selected = NO;
            }
            else {
                [self.device setTorchMode:AVCaptureTorchModeOn];
                flashButton.selected = YES;
            }
            
            [self.device unlockForConfiguration];
        } else {
            
        }
    }
}
- (IBAction)doZoomToggle:(id)sender {
    
    zoomLevel++;
    if (zoomLevel > 2){
        zoomLevel = 0;
    }
    
    [self updateDigitalZoom];
    
}

+ (void) setZoomLevels: (int) zoomLevel1 zoomLevel2: (int) zoomLevel2 initialZoomLevel: (int) initialZoomLevel {
    
    param_ZoomLevel1 = zoomLevel1;
    param_ZoomLevel2 = zoomLevel2;
    zoomLevel = initialZoomLevel;
    if (zoomLevel > 2){
        zoomLevel = 2;
    }
    if (zoomLevel < 0){
        zoomLevel = 0;
    }
    
}

- (void) updateDigitalZoom {
    
    if (videoZoomSupported){
        
        [self.device lockForConfiguration:nil];
        
        switch (zoomLevel) {
            case 0:
                [self.device setVideoZoomFactor:1 /*rampToVideoZoomFactor:1 withRate:4*/];
                break;
            case 1:
                [self.device setVideoZoomFactor:firstZoom /*rampToVideoZoomFactor:firstZooom withRate:4*/];
                break;
            case 2:
                [self.device setVideoZoomFactor:secondZoom /*rampToVideoZoomFactor:secondZoom withRate:4*/];
                break;
                
            default:
                break;
        }
        [self.device unlockForConfiguration];
        
        zoomButton.hidden = !param_EnableZoom;
    } else {
        zoomButton.hidden = true;
    }
}


+ (void) setUseHiRes: (BOOL) hiRes{
    
    useHiRes = hiRes;
    
}

+ (void) setUseBlinkingLineOverlay: (BOOL) blinkingOverlay{
    
    useBLinkingLineOverlay = blinkingOverlay;
    
}

- (void) deinitCapture {
    if (self.captureSession != nil){
#if !__has_feature(objc_arc)
        [self.captureSession release];
#endif
        self.captureSession=nil;
        
        [self.prevLayer removeFromSuperlayer];
        self.prevLayer = nil;
    }
}

- (void)initCapture
{
	/*We setup the input*/
	self.device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    
	AVCaptureDeviceInput *captureInput = [AVCaptureDeviceInput deviceInputWithDevice:self.device error:nil];
	/*We setupt the output*/
	AVCaptureVideoDataOutput *captureOutput = [[AVCaptureVideoDataOutput alloc] init];
	captureOutput.alwaysDiscardsLateVideoFrames = YES;
	//captureOutput.minFrameDuration = CMTimeMake(1, 10); Uncomment it to specify a minimum duration for each video frame
	[captureOutput setSampleBufferDelegate:self queue:dispatch_get_main_queue()];
	// Set the video output to store frame in BGRA (It is supposed to be faster)
    
    NSString* key = (NSString*)kCVPixelBufferPixelFormatTypeKey;
	// Set the video output to store frame in 422YpCbCr8(It is supposed to be faster)
	
	//************************Note this line
	NSNumber* value = [NSNumber numberWithUnsignedInt:kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange];
	
	NSDictionary* videoSettings = [NSDictionary dictionaryWithObject:value forKey:key];
	[captureOutput setVideoSettings:videoSettings];
    
	//And we create a capture session
	self.captureSession = [[AVCaptureSession alloc] init];
	//We add input and output
	[self.captureSession addInput:captureInput];
	[self.captureSession addOutput:captureOutput];
    
    
    if (useHiRes && [self.captureSession canSetSessionPreset:AVCaptureSessionPreset1280x720])
    {
        NSLog(@"Set preview port to 1280X720");
        self.captureSession.sessionPreset = AVCaptureSessionPreset1280x720;
    } else
        //set to 640x480 if 1280x720 not supported on device
        if ([self.captureSession canSetSessionPreset:AVCaptureSessionPreset640x480])
        {
            NSLog(@"Set preview port to 640X480");
            self.captureSession.sessionPreset = AVCaptureSessionPreset640x480;
        }
    
    host_basic_info_data_t hostInfo;
    mach_msg_type_number_t infoCount;
    infoCount = HOST_BASIC_INFO_COUNT;
    host_info( mach_host_self(), HOST_BASIC_INFO, (host_info_t)&hostInfo, &infoCount ) ;
    
    if (hostInfo.max_cpus < 2){
        if ([self.device respondsToSelector:@selector(setActiveVideoMinFrameDuration:)]){
            [self.device lockForConfiguration:nil];
            [self.device setActiveVideoMinFrameDuration:CMTimeMake(1, 15)];
            [self.device unlockForConfiguration];
        } else {
            AVCaptureConnection *conn = [captureOutput connectionWithMediaType:AVMediaTypeVideo];
            [conn setVideoMinFrameDuration:CMTimeMake(1, 15)];
        }
    }
    
    if (availableThreads == 0){
        availableThreads = hostInfo.max_cpus;
    }
    
    if (param_maxThreads > availableThreads){
        param_maxThreads = availableThreads;
    }

    
	/*We add the preview layer*/
    
    self.prevLayer = [AVCaptureVideoPreviewLayer layerWithSession: self.captureSession];
    
    if (self.interfaceOrientation == UIInterfaceOrientationLandscapeLeft){
        self.prevLayer.connection.videoOrientation = AVCaptureVideoOrientationLandscapeLeft;
        self.prevLayer.frame = CGRectMake(0, 0, MAX(self.view.frame.size.width,self.view.frame.size.height), MIN(self.view.frame.size.width,self.view.frame.size.height));
    }
    if (self.interfaceOrientation == UIInterfaceOrientationLandscapeRight){
        self.prevLayer.connection.videoOrientation = AVCaptureVideoOrientationLandscapeRight;
        self.prevLayer.frame = CGRectMake(0, 0, MAX(self.view.frame.size.width,self.view.frame.size.height), MIN(self.view.frame.size.width,self.view.frame.size.height));
    }
    
    
    if (self.interfaceOrientation == UIInterfaceOrientationPortrait) {
        self.prevLayer.connection.videoOrientation = AVCaptureVideoOrientationPortrait;
        self.prevLayer.frame = CGRectMake(0, 0, MIN(self.view.frame.size.width,self.view.frame.size.height), MAX(self.view.frame.size.width,self.view.frame.size.height));
    }
    if (self.interfaceOrientation == UIInterfaceOrientationPortraitUpsideDown) {
        self.prevLayer.connection.videoOrientation = AVCaptureVideoOrientationPortraitUpsideDown;
        self.prevLayer.frame = CGRectMake(0, 0, MIN(self.view.frame.size.width,self.view.frame.size.height), MAX(self.view.frame.size.width,self.view.frame.size.height));
    }
    
   

	self.prevLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
	[self.view.layer addSublayer: self.prevLayer];
    
    if (flashVisible && [self.device isTorchModeSupported:AVCaptureTorchModeOn]){
        [flashButton setHidden:NO];
    } else {
        [flashButton setHidden:YES];

    }
    
    [self.view bringSubviewToFront:cameraOverlay];
    [self.view bringSubviewToFront:closeButton];
    [self.view bringSubviewToFront:flashButton];
    [self.view bringSubviewToFront:zoomButton];

    self.focusTimer = [NSTimer scheduledTimerWithTimeInterval:3.0 target:self selector:@selector(reFocus) userInfo:nil repeats:YES];
    
    
    videoZoomSupported = false;
    
    if ([self.device respondsToSelector:@selector(setActiveFormat:)] &&
        [self.device.activeFormat respondsToSelector:@selector(videoMaxZoomFactor)] &&
        [self.device respondsToSelector:@selector(setVideoZoomFactor:)]){
        
        float maxZoom = 0;
        if ([self.device.activeFormat respondsToSelector:@selector(videoZoomFactorUpscaleThreshold)]){
            maxZoom = self.device.activeFormat.videoZoomFactorUpscaleThreshold;
        } else {
            maxZoom = self.device.activeFormat.videoMaxZoomFactor;
        }
        
        float maxZoomTotal = self.device.activeFormat.videoMaxZoomFactor;
        
        if ([self.device respondsToSelector:@selector(setVideoZoomFactor:)] && maxZoomTotal > 1.1){
            videoZoomSupported = true;
            
            
            
            if (param_ZoomLevel1 != 0 && param_ZoomLevel2 != 0){
                
                if (param_ZoomLevel1 > maxZoomTotal * 100){
                    param_ZoomLevel1 = (int)(maxZoomTotal * 100);
                }
                if (param_ZoomLevel2 > maxZoomTotal * 100){
                    param_ZoomLevel2 = (int)(maxZoomTotal * 100);
                }
                
                firstZoom = 0.01 * param_ZoomLevel1;
                secondZoom = 0.01 * param_ZoomLevel2;
                
                
            } else {
                
                if (maxZoomTotal > 2){
                    
                    if (maxZoom > 1.0 && maxZoom <= 2.0){
                        firstZoom = maxZoom;
                        secondZoom = maxZoom * 2;
                    } else
                        if (maxZoom > 2.0){
                            firstZoom = 2.0;
                            secondZoom = 4.0;
                        }
                    
                }
            }
            
            
        }
        
        
    }
    
    if (!videoZoomSupported){
        zoomButton.hidden = true;
    } else {
        [self updateDigitalZoom];
    }
    
    activeThreads = 0;
}

- (void) onVideoStart: (NSNotification*) note
{
    if(running)
        return;
    running = YES;
    
    // lock device and set focus mode
    NSError *error = nil;
    if([self.device lockForConfiguration: &error])
    {
        if([self.device isFocusModeSupported: AVCaptureFocusModeContinuousAutoFocus])
            self.device.focusMode = AVCaptureFocusModeContinuousAutoFocus;
    }
}

- (void) onVideoStop: (NSNotification*) note
{
    if(!running)
        return;
    [self.device unlockForConfiguration];
    running = NO;
}

#pragma mark -
#pragma mark AVCaptureSession delegate

- (void)captureOutput:(AVCaptureOutput *)captureOutput
didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
	   fromConnection:(AVCaptureConnection *)connection
{
    if (state != CAMERA && state != CAMERA_DECODING) {
        return;
    }
    
    if (activeThreads >= param_maxThreads){
        return;
    }
    
    if (state != CAMERA_DECODING)
    {
        state = CAMERA_DECODING;
    }
    
    activeThreads++;
	
	
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    //Lock the image buffer
    CVPixelBufferLockBaseAddress(imageBuffer,0);
    //Get information about the image
    baseAddress = (uint8_t *)CVPixelBufferGetBaseAddressOfPlane(imageBuffer,0);
    int pixelFormat = CVPixelBufferGetPixelFormatType(imageBuffer);
	switch (pixelFormat) {
		case kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange:
			//NSLog(@"Capture pixel format=NV12");
			bytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(imageBuffer,0);
			width = bytesPerRow;//CVPixelBufferGetWidthOfPlane(imageBuffer,0);
			height = CVPixelBufferGetHeightOfPlane(imageBuffer,0);
			break;
		case kCVPixelFormatType_422YpCbCr8:
			//NSLog(@"Capture pixel format=UYUY422");
			bytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(imageBuffer,0);
			width = CVPixelBufferGetWidth(imageBuffer);
			height = CVPixelBufferGetHeight(imageBuffer);
			int len = width*height;
			int dstpos=1;
			for (int i=0;i<len;i++){
				baseAddress[i]=baseAddress[dstpos];
				dstpos+=2;
			}
			
			break;
		default:
			//	NSLog(@"Capture pixel format=RGB32");
			break;
	}
	
    unsigned char *frameBuffer = malloc(width * height);
    memcpy(frameBuffer, baseAddress, width * height);
    CVPixelBufferUnlockBaseAddress(imageBuffer,0);
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
	
        unsigned char *pResult=NULL;

        int resLength = MWB_scanGrayscaleImage(frameBuffer,width,height, &pResult);
        free(frameBuffer);
        //NSLog(@"Frame decoded. Active threads: %d", activeThreads);
        
        MWResults *mwResults = nil;
        MWResult *mwResult = nil;
        if (resLength > 0){
            
            if (state == NORMAL){
                resLength = 0;
                free(pResult);
                
            } else {
                mwResults = [[MWResults alloc] initWithBuffer:pResult];
                if (mwResults && mwResults.count > 0){
                    mwResult = [mwResults resultAtIntex:0];
                    
                }
                
                free(pResult);
            }
        }
        
        
        if (mwResult && mwResult.bytesLength > 0){
            
            if (state == NORMAL){
                return;
            }
        }
        
        //ignore results less than 4 characters - probably false detection
        if ( mwResult && mwResult.bytesLength > 0)
        {
            state = NORMAL;

            int bcType = mwResult.type;
            NSString *typeName=@"";
            switch (bcType) {
                case FOUND_25_INTERLEAVED: typeName = @"Code 25 Interleaved";break;
                case FOUND_25_STANDARD: typeName = @"Code 25 Standard";break;
                case FOUND_128: typeName = @"Code 128";break;
                case FOUND_128_GS1: typeName = @"Code 128 GS1";break;
                case FOUND_39: typeName = @"Code 39";break;
                case FOUND_93: typeName = @"Code 93";break;
                case FOUND_AZTEC: typeName = @"AZTEC";break;
                case FOUND_DM: typeName = @"Datamatrix";break;
                case FOUND_QR: typeName = @"QR";break;
                case FOUND_EAN_13: typeName = @"EAN 13";break;
                case FOUND_EAN_8: typeName = @"EAN 8";break;
                case FOUND_NONE: typeName = @"None";break;
                case FOUND_RSS_14: typeName = @"Databar 14";break;
                case FOUND_RSS_14_STACK: typeName = @"Databar 14 Stacked";break;
                case FOUND_RSS_EXP: typeName = @"Databar Expanded";break;
                case FOUND_RSS_LIM: typeName = @"Databar Limited";break;
                case FOUND_UPC_A: typeName = @"UPC A";break;
                case FOUND_UPC_E: typeName = @"UPC E";break;
                case FOUND_PDF: typeName = @"PDF417";break;
                case FOUND_CODABAR: typeName = @"Codabar";break;
                case FOUND_DOTCODE: typeName = @"Dotcode";break;
                case FOUND_ITF14: typeName = @"ITF 14";break;
                case FOUND_11: typeName = @"Code 11";break;
                case FOUND_MSI: typeName = @"MSI Plessey";break;
            }
            
            lastFormat = typeName;
            
            
            
            
            
           /* int size=mwR;
            
            char *temp = (char *)malloc(size+1);
            memcpy(temp, pResult, size+1);
            NSString *resultString = [[NSString alloc] initWithBytes: temp length: size encoding: NSUTF8StringEncoding];
            
            //NSLog(@"Detected %@: %@", lastFormat, resultString);
            
            NSMutableString *binString = [[NSMutableString alloc] init];
            
            for (int i = 0; i < size; i++)
                [binString appendString:[NSString stringWithFormat:@"%c", temp[i]]];
            
            if (MWB_getLastType() == FOUND_PDF || resultString == nil)
                resultString = [binString copy];
            else
                resultString = [resultString copy];
            
            */
            dispatch_async(dispatch_get_main_queue(), ^(void) {
            
                if (decodeImage != nil)
                {
                    CGImageRelease(decodeImage);
                    decodeImage = nil;
                }
                
                if (closeCameraOnDetection){
                    [self.captureSession stopRunning];
                }
                NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
                DecoderResult *notificationResult = [DecoderResult createSuccess:mwResult];
                [center postNotificationName:DecoderResultNotification object: notificationResult];
               
            });
            

            
        }
        else
        {
            state = CAMERA;
        }
        activeThreads --;

    });
	
}

- (IBAction)doClose:(id)sender {
    //[self dismissModalViewControllerAnimated:YES];
   
    [self.delegate scanningFinished:nil withType:nil isGS1:NO andRawResult:nil closeRequested:YES];
}


#pragma mark -
#pragma mark Memory management

- (void)viewDidUnload
{
	[self stopScanning];
	
	self.prevLayer = nil;
	[super viewDidUnload];
}

- (void)dealloc {
    [super dealloc];
	[[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void) startScanning {
	state = LAUNCHING_CAMERA;
	MWB_setResultType(MWB_RESULT_TYPE_MW);
	[self.captureSession startRunning];
	self.prevLayer.hidden = NO;
	state = CAMERA;
}

- (void)stopScanning {
	if (state == CAMERA_DECODING) {
		state = CANCELLING;
		return;
	}
	
	[self revertToNormal];
}

- (void)revertToNormal {
	
	[self.captureSession stopRunning];
	state = NORMAL;
}

- (void)decodeResultNotification: (NSNotification *)notification {
	
	if ([notification.object isKindOfClass:[DecoderResult class]])
	{
		DecoderResult *obj = (DecoderResult*)notification.object;
		if (obj.succeeded)
		{
            
            BOOL shouldClose;
            
            if (closeCameraOnDetection){
                shouldClose = YES;
                
            } else {
                shouldClose = NO;
                state = CAMERA_PAUSED;
            }
            
            NSString *typeName = obj.mwResult.typeName;
           /* if (obj.mwResult.isGS1){
                
                typeName = [NSString stringWithFormat:@"%@ (GS1)", typeName];
            }*/
            
            [self.delegate scanningFinished:obj.mwResult.text withType: typeName isGS1:obj.mwResult.isGS1 andRawResult: [[NSData alloc] initWithBytes: obj.mwResult.bytes length: obj.mwResult.bytesLength] closeRequested:shouldClose];
            
		}
	}
}

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    if (buttonIndex == 0) {
        [self startScanning];
    }
}

- (NSUInteger)supportedInterfaceOrientations {
    
    UIInterfaceOrientation interfaceOrientation =[[UIApplication sharedApplication] statusBarOrientation];
    
    switch (interfaceOrientation) {
        case UIInterfaceOrientationPortrait:
            return UIInterfaceOrientationMaskPortrait;
            break;
        case UIInterfaceOrientationPortraitUpsideDown:
            return UIInterfaceOrientationMaskPortraitUpsideDown;
            break;
        case UIInterfaceOrientationLandscapeLeft:
            return UIInterfaceOrientationMaskLandscapeLeft;
            break;
        case UIInterfaceOrientationLandscapeRight:
            return UIInterfaceOrientationMaskLandscapeRight;
            break;
            
        default:
            break;
    }
    
    return UIInterfaceOrientationMaskAll;
}

- (BOOL) shouldAutorotate {
    
    return YES;
    
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
	return (interfaceOrientation == UIInterfaceOrientationPortrait);
}




/*- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    [self toggleTorch];
}*/

- (UIImage *)imageNamed:(NSString *)name {
    NSString *path = [NSString stringWithFormat:@"modules/%@/%@.png",
                      self, name];
    //NSURL *url = [TiUtils toURL:path proxy:module];
    NSString *urlString = [NSString stringWithFormat:@"%@%@/%@",
                           [[[NSURL fileURLWithPath:path] absoluteString] stringByReplacingOccurrencesOfString:path withString:@""],
                           [[NSBundle mainBundle] resourcePath],
                           path, nil];
    NSURL *url = [NSURL URLWithString:[urlString stringByAddingPercentEscapesUsingEncoding:NSStringEncodingConversionExternalRepresentation]];
    return [UIImage imageWithData:[[NSData alloc] initWithContentsOfURL:url]];
}

#pragma mark -
#pragma mark Overlay functions

+ (void) updateScanningRects: (NSMutableArray *) newScanningRects {
    
    scanningRects = newScanningRects;
    
}

+ (void) updateActiveCodes: (int) newActiveCodes {
    
    activeCodes = newActiveCodes;
    
}

+ (void) updateOrientation: (int) newOrientation {
    
    orientation = newOrientation;
    
}



@end

/*
 *  Implementation of the object that returns decoder results (via the notification
 *	process)
 */

@implementation DecoderResult

@synthesize succeeded;
@synthesize mwResult;

+(DecoderResult *)createSuccess:(MWResult *)result {
    DecoderResult *obj = [[DecoderResult alloc] init];
    if (obj != nil) {
        obj.succeeded = YES;
        obj.mwResult = result;
    }
    return obj;
}

+(DecoderResult *)createFailure {
	DecoderResult *obj = [[DecoderResult alloc] init];
	if (obj != nil) {
		obj.succeeded = NO;
		obj.mwResult = nil;
       
	}
	return obj;
}

- (void)dealloc {
    [super dealloc];
	self.mwResult = nil;
}


@end