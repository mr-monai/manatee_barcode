Ti.include('/MWBScanner.js');
var button = Titanium.UI.createButton({
	   title: 'Start Scanning',
	   top: 20,
	   width: 300,
	   height: 80
	});
	
	button.addEventListener('click',startScanning);
var MWWindow = Titanium.UI.createWindow({
    backgroundColor:'#000'
});
MWWindow.add(button);
MWWindow.open();