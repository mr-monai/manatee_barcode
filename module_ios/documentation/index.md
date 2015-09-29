# MWBarcodeScanner Module

## Description

This module makes the Manatee Works' Barcode Scanner SDK API available to all iOS and Android Titanium developers.

## Licensing

If you want to use a fully functional MW Barcode Scanner SDK module, beyond the free UPC and EAN decoders and to remove the partially masked results upon a successful scan for the rest of the supported barcode types, you will need to obtain a valid MW Barcode Scanner user name and license key, by previously completing a purchase or obtaining an evaluation license by registering on our [Developers Network](http://manateeworks.com/developers)</em>. You can contact our sales team by filling-in the contact form on the home page of our website or directly via sales@manateeworks.com. The license for Titanium apps is the same as for native apps.
The valid license can be added by way of inputing the license data into _Resources/MWBScanner.js_ file inside _MWBinitDecoder function_. 

## Dependencies

This module requires Release 3.1 or newer of the Titanium SDK.
This module does not include support for the ARMv6 architecture on iOS.

## Accessing the mwbarcodescanner Module

To access this module from JavaScript, you should proceed with doing the following:

    var scanner = require('com.manateeworks.barcodescanner');

The mwbarcodescanner_ios variable is a reference to the Module object.

## Reference

#### Decoder Configuration Functions:

**BarcodeScanner.MWBinitDecoder ()**

    Initializes the decoder with default parameters.  

**BarcodeScanner.MWBsetActiveCodes (activeCodes)**

    Sets the status of different decoder types to active or inactive and updates the decoders execution priority list.   

**BarcodeScanner.MWBsetActiveSubcodes (codeMask, activeSubcodes)** 

    Sets active subcodes for given code group flag.   

**BarcodeScanner.MWBsetFlags (codeMask, flags)**

    Configures options for any single barcode type specified in _codeMask_. These options are given in _flags_ as bitwise OR as option bits. The available options depend on previously selected decoder type.

**BarcodeScanner.MWBsetDirection (direction)** 

    Configures scanning direction for 1D and/or PDF417 decoders (not affecting QR, Aztec and DataMatrix)

**BarcodeScanner.MWBsetScanningRect (codeMask, left, top, width, height)** 

    Sets rectangular area for barcode scanning with any selected decoder type. The parameters are percentages of full screen width and height.

**BarcodeScanner.MWBsetLevel (level)**

    Configures the global library effort level, where 1 is fastest and 5 is hardest. For live scanning, recommended are levels 2 or 3.

_For a detailed explanation of all configuration functions available to the decoder, please get official documentation from [Manateeworks site](http://www.manateeworks.com)_ 

### Decoder Interface Functions:

**BarcodeScanner.MWBregisterCode(codeMask, userName, key)** 

Registers the specified barcode type with licensing data
<p>**BarcodeScanner.MWBuseHiRes (hiRes)**

    Forces using higher scanning resolution if available on the device. The default setting is FALSE.

**BarcodeScanner.MWBsetButtonsVisible (flashVisible, closeVisible)**

    Enables/disables "Flash" and "Close" buttons. Close button is ignored on Android and never displayed. 

**BarcodeScanner.MWBuseBlinkingLineOverlay (blinkingLine)**

    If TRUE, it shows scanning rectangle with a blinking line. If FALSE, the overlay image is displayed. The default setting is TRUE.

**BarcodeScanner.MWBsetCloseCameraOnDetection (closeOnDetection)**

    If TRUE, the scanning screen will be closed automatically on next barcode detection. If FALSE, the result callback will be executed but it will remain on scanning screen with scanning paused. The default setting is TRUE.

**BarcodeScanner.MWBresumeScanning()**

    Resumes scanning if previously paused. 

**BarcodeScanner.MWBcloseScanner()**

    Stop scanning and close scanner screen. 

**startScanning ()**

    Show camera screen and start scanning. 

## Usage

Step-by-Step guide for adding the plugin to your Titanium project(s):

1.  Build Android/iOS modules and then add them to the project (by using Titanium Studio build/publish options)2.  Copy _example/MWBScanner.js_ from the plugin folder into your app's _Resources_ folder
3.  Include _MWBScanner.js_ to app.js: _Ti.include('/MWBScanner.js')_;
4.  Adjust _MWBinitDecoder_ function according to your personal preferences
5.  Call  _startScanning()_ function to start scanning
6.  Handle scanner result in _scanner.startScanning_ callback

It's highly recommended to put all decoder and related interface configuration options inside the _MWBinitDecoder_ function in _MWBScanner.js_

## Author

Vladimir Zivkovic, Manatee Works,
vladz@manateeworks.com

## Feedback and Support

Please direct all questions, feedback, and concerns to dev@manateeworks.com

## License

Copyright(c) 2012-2014 by Manatee Works, Inc. All Rights Reserved.
Please see the License Agreement file included in the distribution for further 
details.