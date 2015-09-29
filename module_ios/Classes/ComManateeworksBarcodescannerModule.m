/**
 * Your Copyright Here
 *
 * Appcelerator Titanium is Copyright (c) 2009-2010 by Appcelerator, Inc.
 * and licensed under the Apache Public License (version 2)
 */
#import "ComManateeworksBarcodescannerModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"
#import "BarcodeScanner.h"
#import "TiApp.h"
#import "MWScannerViewController.h"

@implementation ComManateeworksBarcodescannerModule

#pragma mark Internal

// this is generated for your module, please do not change it
-(id)moduleGUID
{
	return @"cbb43234-dbfa-4fe4-b116-f8a97632760c";
}

// this is generated for your module, please do not change it
-(NSString*)moduleId
{
	return @"com.manateeworks.barcodescanner";
}


-(UIImage*)loadImageFromModule:(NSString *) imageName
{
	NSString *pathComponent = [NSString stringWithFormat:@"modules/%@/%@", [self moduleId], imageName];
	NSString *imagePath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:pathComponent];
    NSLog(@"image url: %@", imagePath);
	UIImage *image = [UIImage imageWithContentsOfFile:imagePath];
    NSLog(@"image: %@", image);
	return image;
}

#pragma mark Lifecycle

-(void)startup
{
	// this method is called when the module is first loaded
	// you *must* call the superclass
	[super startup];
    
    //register your copy of library with givern user/password
    
    // Now you can use registering calls from JS, without recompiling the modules
   /* MWB_registerCode(MWB_CODE_MASK_39,      "username", "key");
    MWB_registerCode(MWB_CODE_MASK_93,      "username", "key");
    MWB_registerCode(MWB_CODE_MASK_25,      "username", "key");
    MWB_registerCode(MWB_CODE_MASK_128,     "username", "key");
    MWB_registerCode(MWB_CODE_MASK_AZTEC,   "username", "key");
    MWB_registerCode(MWB_CODE_MASK_DM,      "username", "key");
    MWB_registerCode(MWB_CODE_MASK_EANUPC,  "username", "key");
    MWB_registerCode(MWB_CODE_MASK_QR,      "username", "key");
    MWB_registerCode(MWB_CODE_MASK_PDF,     "username", "key");
    MWB_registerCode(MWB_CODE_MASK_RSS,     "username", "key");
    MWB_registerCode(MWB_CODE_MASK_CODABAR, "username", "key");*/
    
      
    controller = [[MWScannerViewController alloc] init];
    controller.delegate = self;
    
    [MWScannerViewController setOverlayImage: [[self loadImageFromModule:@"overlay.png"]copy]];
    [MWScannerViewController setCloseButtonImage: [[self loadImageFromModule:@"close_button.png"] copy]];
    [MWScannerViewController setFlashButtonImages:[[self loadImageFromModule:@"flashbuttonon.png"]copy] imageOff:[[self loadImageFromModule:@"flashbuttonoff.png"]copy] ];
    [MWScannerViewController setZoomImage:[[self loadImageFromModule:@"zoom.png"]copy]];

    scanningRects = [[NSMutableArray alloc] init];
    
	NSLog(@"[INFO] %@ loaded",self);
}



-(void)shutdown:(id)sender
{
	// this method is called when the module is being unloaded
	// typically this is during shutdown. make sure you don't do too
	// much processing here or the app will be quit forceably
    
    [controller release];
	
	// you *must* call the superclass
	[super shutdown:sender];
    
    
}

#pragma mark Cleanup 

-(void)dealloc
{
	// release any resources that have been retained by the module
	[super dealloc];
}

#pragma mark Internal Memory Management

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
	// optionally release any resources that can be dynamically
	// reloaded once memory is available - such as caches
	[super didReceiveMemoryWarning:notification];
}

#pragma mark Listener Notifications

-(void)_listenerAdded:(NSString *)type count:(int)count
{
	if (count == 1 && [type isEqualToString:@"my_event"])
	{
		// the first (of potentially many) listener is being added 
		// for event named 'my_event'
	}
}

-(void)_listenerRemoved:(NSString *)type count:(int)count
{
	if (count == 0 && [type isEqualToString:@"my_event"])
	{
		// the last listener called for event named 'my_event' has
		// been removed, we can optionally clean up any resources
		// since no body is listening at this point for that event
	}
}

#pragma Public APIs


-(int)MWBregisterCode:(id)args
{

	NSString *username = [args objectAtIndex: 1];
	char *charUsername = [username UTF8String];
	NSString *key = [args objectAtIndex: 2];
	char *charKey = [key UTF8String];
	
	MWB_registerCode([[args objectAtIndex: 0] intValue], charUsername, charKey);
	
	
	int activeCodes = MWB_getActiveCodes();
	MWB_setActiveCodes(activeCodes);
	
	

}

-(int)MWBsetLevel:(id)args
{
	
	 return MWB_setLevel([[args objectAtIndex: 0] intValue]);
	   
}

-(int)MWBsetActiveCodes:(id)args
{
	[MWScannerViewController updateActiveCodes:[[args objectAtIndex: 0] intValue]];
	return MWB_setActiveCodes([[args objectAtIndex: 0] intValue]);
    
}

-(int)MWBsetActiveSubcodes:(id)args
{
	
	return MWB_setActiveSubcodes([[args objectAtIndex: 0] intValue], [[args objectAtIndex: 1] intValue]);
    
}

-(int)MWBgetLibVersion:(id)args
{
	
	return MWB_getLibVersion();
    
}

-(NSString*) MWBgetLibVersionString: (id) args
{
		
    int ver = MWB_getLibVersion();
    int v1 = (ver >> 16);
    int v2 = (ver >> 8) & 0xff;
    int v3 = (ver & 0xff);
    NSString *libVersion = [NSString stringWithFormat:@"%d.%d.%d", v1, v2, v3];
    return libVersion;
    
}

-(int)MWBsetFlags:(id)args
{
	
	return MWB_setFlags([[args objectAtIndex: 0] intValue], [[args objectAtIndex: 1] intValue]);
    
}

-(int)MWBsetDirection:(id)args
{
	
    [MWScannerViewController updateOrientation:[[args objectAtIndex: 0] intValue]];
	return MWB_setDirection([[args objectAtIndex: 0] intValue]);
    
    
}

-(int)MWBsetScanningRect:(id)args
{
	
    
    int rectIndex = -1;
    for (int i = 0; i < scanningRects.count; i++){
        
        NSMutableDictionary *curRect = [scanningRects objectAtIndex:i];
        if ([[curRect objectForKey:@"mask"] intValue] == [[args objectAtIndex: 0] intValue]) {
            rectIndex = i;
            break;
        }
    }
    
    NSMutableDictionary *newRect = [[NSMutableDictionary alloc] init];
    [newRect setObject:[args objectAtIndex: 0] forKey:@"mask"];
    [newRect setObject:[NSValue valueWithCGRect:CGRectMake([[args objectAtIndex: 1] intValue],[[args objectAtIndex: 2] intValue],
                                                           [[args objectAtIndex: 3] intValue],[[args objectAtIndex: 4] intValue])] forKey:@"rect"];
    
    if (rectIndex >= 0){
        [scanningRects replaceObjectAtIndex:rectIndex withObject:newRect];
    } else {
        [scanningRects addObject:newRect];
    }
    
    [MWScannerViewController updateScanningRects:scanningRects];

	return MWB_setScanningRect([[args objectAtIndex: 0] intValue], [[args objectAtIndex: 1] intValue]
                               , [[args objectAtIndex: 2] intValue], [[args objectAtIndex: 3] intValue]
                               , [[args objectAtIndex: 4] intValue]);
    
    
    
    
    
    
    
}


-(int)MWBsetDesiredResolution:(id)args
{
	
    if ([[args objectAtIndex: 0] intValue] >= 1024){
        
        [MWScannerViewController setUseHiRes:true];
        
    } else {
        
        [MWScannerViewController setUseHiRes:false];
        
    }
    
    return 0;
}

-(void)MWBuseHiRes:(id)args
{
	
	[MWScannerViewController setUseHiRes:[[args objectAtIndex: 0] boolValue]];
    
}


-(void)MWBuseBlinkingLineOverlay:(id)args
{
	
	[MWScannerViewController setUseBlinkingLineOverlay:[[args objectAtIndex: 0] boolValue]];
    
}

-(void)MWBsetCloseCameraOnDetection:(id)args
{
	
	[MWScannerViewController setCloseCameraOnDetection:[[args objectAtIndex: 0] boolValue]];
    
}

-(void)MWBsetButtonsVisible:(id)args
{
	
	[MWScannerViewController setButtonsVisible:[[args objectAtIndex: 0] boolValue] closeButtonVisible:[[args objectAtIndex: 1] boolValue]];
    
}


-(void)MWBresumeScanning:(id)args
{
	
	[MWScannerViewController resumeScanning];
    
}

-(void)MWBcloseScanner:(id)args
{
	
	[controller performSelectorOnMainThread:@selector(doClose:) withObject:nil waitUntilDone:NO];
    
}
-(void)MWBsetMaxThreads:(id)args
{
    [MWScannerViewController setMaxThreads:[[args objectAtIndex: 0] intValue]];
        
}
-(void)MWBenableZoom:(id)args
{
    [MWScannerViewController enableZoom:[[args objectAtIndex: 0] boolValue]];
}
-(void)MWBsetZoomLevels:(id)args
{
    [MWScannerViewController setZoomLevels:[[args objectAtIndex:0]intValue] zoomLevel2:[[args objectAtIndex:1]intValue] initialZoomLevel:[[args objectAtIndex:2]intValue]]; 
}


-(void)startScanning:(id)args
{
    
    
    ENSURE_UI_THREAD(startScanning,args);
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    
    // callbacks
    if ([args objectForKey:@"success"] != nil) {
        successCallback = [args objectForKey:@"success"];
        ENSURE_TYPE_OR_NIL(successCallback,KrollCallback);
        [successCallback retain];
    }
    
    if ([args objectForKey:@"error"] != nil) {
        errorCallback = [args objectForKey:@"error"];
        ENSURE_TYPE_OR_NIL(errorCallback,KrollCallback);
        [errorCallback retain];
    }
    
    if ([args objectForKey:@"cancel"] != nil) {
        cancelCallback = [args objectForKey:@"cancel"];
        ENSURE_TYPE_OR_NIL(cancelCallback,KrollCallback);
        [cancelCallback retain];
    }
    
   
    [[TiApp app] showModalController: controller animated: YES];
    
    
}


- (void) scanningFinished:(NSString *)result withType:(NSString *)lastFormat isGS1: (bool) isGS1 andRawResult:(NSData *)rawResult closeRequested: (BOOL) closeRequested{
    
    if (rawResult != nil){
    
    
        NSMutableArray *bytesArray = [[NSMutableArray alloc] init];
        unsigned char *bytes = (unsigned char *) [rawResult bytes];
        for (int i = 0; i < rawResult.length; i++){
            [bytesArray addObject:[NSNumber numberWithInt: bytes[i]]];
        }
        
        NSMutableDictionary *resultDict = [[NSMutableDictionary alloc] initWithObjects:[NSArray arrayWithObjects:result, lastFormat, bytesArray,[NSNumber numberWithBool:isGS1], nil] forKeys:[NSArray arrayWithObjects:@"code", @"type",@"bytes",@"isGS1", nil]];
        
        if (successCallback!=nil){
            id listener = [[successCallback retain] autorelease];
            
        
            [self _fireEventToListener:@"cancel" withObject:resultDict listener:listener thisObject:nil];
        }

        
    } else {
        
        if (cancelCallback!=nil){
            id listener = [[cancelCallback retain] autorelease];
            
            NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
            [self _fireEventToListener:@"cancel" withObject:dictionary listener:listener thisObject:nil];
        }

    }
    
    if (closeRequested){
        [controller dismissModalViewControllerAnimated: YES];
    }
    
}


@end





















