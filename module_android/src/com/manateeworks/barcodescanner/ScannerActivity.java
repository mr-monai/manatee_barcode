package com.manateeworks.barcodescanner;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.appcelerator.kroll.common.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.manateeworks.BarcodeScanner;
import com.manateeworks.BarcodeScanner.MWResult;
import com.manateeworks.barcodescanner.MwbarcodescannerModule.BarcodeResultHandler;
import com.manateeworks.camera.CameraManager;

public class ScannerActivity extends Activity implements SurfaceHolder.Callback {

	private static ScannerActivity instance = null;
	public static BarcodeResultHandler resultHandler;

	private Handler handler;
	public static final int MSG_DECODE = 1;
	public static final int MSG_AUTOFOCUS = 2;
	public static final int MSG_DECODE_SUCCESS = 3;
	public static final int MSG_DECODE_FAILED = 4;

	private boolean hasSurface;

	public static int desiredCameraWidth = 800;
	public static int desiredCameraHeight = 480;

	public static ArrayList<int[]> scanningRects;
	public static int orientation;
	public static int activeCodes;

	public static boolean useBLinkingLineOverlay = true;

	public static boolean closeCameraOnDetection = true;

	public static boolean flashVisible = true;
	public static boolean closeVisible = true;

	public static float densityScale;

	private ImageButton buttonFlash;
	private ImageButton buttonZoom;
	boolean flashOn = false;

	Bitmap flashOnBitmap;
	Bitmap flashOffBitmap;

	private SurfaceView surfaceView;
	public static ImageView overlayImage;

	public static int param_maxThreads = 4;
	private int activeThreads = 0;
	public static int MAX_THREADS = Runtime.getRuntime().availableProcessors();

	public static boolean param_EnableZoom = true;
	public static int param_ZoomLevel1 = 0;
	public static int param_ZoomLevel2 = 0;
	public static int zoomLevel = 0;
	private int firstZoom = 150;
	private int secondZoom = 300;

	private enum State {
		STOPPED, PREVIEW, DECODING
	}

	static State state = State.STOPPED;

	public static Drawable getAssetImage(Context context, String filename)
			throws IOException {
		AssetManager assets = context.getResources().getAssets();
		InputStream buffer = new BufferedInputStream((assets.open(filename)));
		Bitmap bitmap = BitmapFactory.decodeStream(buffer);
		return new BitmapDrawable(bitmap);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		instance = this;
		// LinearLayout
		// setContentView(R.layout.scanner);
		state = State.STOPPED;

		densityScale = getApplicationContext().getResources()
				.getDisplayMetrics().density;

		RelativeLayout.LayoutParams previewParams = new RelativeLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		RelativeLayout mainLayout = new RelativeLayout(this);
		mainLayout.setLayoutParams(previewParams);

		surfaceView = new SurfaceView(this);
		surfaceView.setLayoutParams(previewParams);
		mainLayout.addView(surfaceView);

		overlayImage = new ImageView(this);
		overlayImage.setLayoutParams(previewParams);
		overlayImage.setScaleType(ScaleType.FIT_XY);
		try {
			overlayImage.setImageDrawable(getAssetImage(this, "overlay.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mainLayout.addView(overlayImage);

		if (flashVisible) {
			try {
				Bitmap bitmap = ((BitmapDrawable) getAssetImage(this,
						"flashbuttonon.png")).getBitmap();
				flashOnBitmap = Bitmap.createScaledBitmap(bitmap,
						(int) (densityScale * 32), (int) (densityScale * 32),
						true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				Bitmap bitmap = ((BitmapDrawable) getAssetImage(this,
						"flashbuttonoff.png")).getBitmap();
				flashOffBitmap = Bitmap.createScaledBitmap(bitmap,
						(int) (densityScale * 32), (int) (densityScale * 32),
						true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			buttonFlash = new ImageButton(this);

			buttonFlash.setBackgroundColor(0x00000000);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					(int) (32f * densityScale + 0.5f),
					(int) (32f * densityScale + 0.5f));

			// params.width = (int)(32 * scale + 0.5f);
			// params.height = (int)(32 * scale + 0.5f);
			params.topMargin = (int) (20f * densityScale + 0.5f);
			params.leftMargin = (int) (20f * densityScale + 0.5f);
			buttonFlash.setLayoutParams(params);
			buttonFlash.setPadding(0, 0, 0, 0);
			buttonFlash.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleFlash();

				}
			});
			mainLayout.addView(buttonFlash);

		}
		if (param_EnableZoom) {

			Bitmap zoomBitmap = null;
			try {
				Bitmap bitmap = ((BitmapDrawable) getAssetImage(this,
						"zoom.png")).getBitmap();
				zoomBitmap = Bitmap.createScaledBitmap(bitmap,
						(int) (densityScale * 32), (int) (densityScale * 32),
						true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			buttonZoom = new ImageButton(this);

			buttonZoom.setBackgroundColor(0x00000000);

			RelativeLayout.LayoutParams paramsZoom = new RelativeLayout.LayoutParams(
					(int) (32f * densityScale + 0.5f),
					(int) (32f * densityScale + 0.5f));

			paramsZoom.topMargin = (int) (20f * densityScale + 0.5f);
			paramsZoom.rightMargin = (int) (20f * densityScale + 0.5f);
			paramsZoom.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			buttonZoom.setLayoutParams(paramsZoom);
			buttonZoom.setPadding(0, 0, 0, 0);
			buttonZoom.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleZoom();

				}
			});
			buttonZoom.setImageBitmap(zoomBitmap);
			mainLayout.addView(buttonZoom);
		}
		setContentView(mainLayout);

		CameraManager.init(getApplication());

		/*
		 * BarcodeScanner.MWBsetActiveCodes(BarcodeScanner.MWB_CODE_MASK_PDF);
		 * BarcodeScanner
		 * .MWBsetDirection(BarcodeScanner.MWB_SCANDIRECTION_HORIZONTAL);
		 * BarcodeScanner.MWBsetScanningRect(BarcodeScanner.MWB_CODE_MASK_PDF,
		 * 0,0,100,100); BarcodeScanner.MWBsetLevel(2);
		 */

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (useBLinkingLineOverlay) {
			overlayImage.setVisibility(View.GONE);

		} else {
			overlayImage.setVisibility(View.VISIBLE);

		}

		// SurfaceView surfaceView = (SurfaceView)
		// findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		int ver = BarcodeScanner.MWBgetLibVersion();
		int v1 = (ver >> 16);
		int v2 = (ver >> 8) & 0xff;
		int v3 = (ver & 0xff);
		String libVersion = "Lib version: " + String.valueOf(v1) + "."
				+ String.valueOf(v2) + "." + String.valueOf(v3);
		Toast.makeText(this, libVersion, Toast.LENGTH_LONG).show();

		if (useBLinkingLineOverlay) {
			MWOverlay.addOverlay(this, surfaceView);
		}

	}

	@Override
	protected void onPause() {

		flashOn = false;
		updateFlash();

		super.onPause();
		if (handler != null) {
			CameraManager.get().stopPreview();
			handler = null;
		}
		CameraManager.get().closeDriver();

		if (useBLinkingLineOverlay) {
			MWOverlay.removeOverlay();
		}
		state = State.STOPPED;

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		hasSurface = false;

	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			// Select desired camera resoloution. Not all devices supports all
			// resolutions, closest available will be chosen
			// If not selected, closest match to screen resolution will be
			// chosen
			// High resolutions will slow down scanning proccess on slower
			// devices

			CameraManager.setDesiredPreviewSize(desiredCameraWidth,
					desiredCameraHeight);

			CameraManager.get().openDriver(surfaceHolder, false);

			int maxZoom = CameraManager.get().getMaxZoom();
			if (maxZoom > 100) {
				if (param_EnableZoom) {
					updateZoom();
				}
			}
		} catch (IOException ioe) {
			displayFrameworkBugMessageAndExit();
			return;
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			displayFrameworkBugMessageAndExit();
			return;
		}
		if (handler == null) {
			handler = new Handler(new Handler.Callback() {

				@Override
				public boolean handleMessage(Message msg) {

					switch (msg.what) {
					case MSG_AUTOFOCUS:
						if (state == State.PREVIEW || state == State.DECODING) {

							CameraManager.get().requestAutoFocus(handler,
									MSG_AUTOFOCUS);
						}
						break;
					case MSG_DECODE:
						decode((byte[]) msg.obj, msg.arg1, msg.arg2);
						break;
					case MSG_DECODE_FAILED:
						// CameraManager.get().requestPreviewFrame(handler,
						// MSG_DECODE);
						break;
					case MSG_DECODE_SUCCESS:
						state = State.STOPPED;

						handleDecode((MWResult) msg.obj);
						break;

					default:
						break;
					}

					return false;
				}
			});
		}

		flashOn = false;
		updateFlash();

		startScanning();

	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Error");
		builder.setMessage("Camera error");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
				finish();
			}
		});
		builder.show();
	}

	private void startScanning() {
		CameraManager.get().startPreview();
		state = State.PREVIEW;
		CameraManager.get().requestPreviewFrame(handler, MSG_DECODE);
		CameraManager.get().requestAutoFocus(handler, MSG_AUTOFOCUS);
		BarcodeScanner.MWBsetResultType(BarcodeScanner.MWB_RESULT_TYPE_MW);
	}

	private void decode(final byte[] data, final int width, final int height) {

		if (param_maxThreads > MAX_THREADS) {
			param_maxThreads = MAX_THREADS;
		}

		if (activeThreads >= param_maxThreads || state == State.STOPPED) {
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				activeThreads++;

				//Log.i("Active threads", activeThreads + "/" + param_maxThreads);
				// final long timeNow = System.currentTimeMillis();
				// Check for barcode inside buffer
				byte[] rawResult = BarcodeScanner.MWBscanGrayscaleImage(data,
						width, height);
				if (state == State.STOPPED) {
					activeThreads--;
					return;
				}

				
				MWResult mwResult = null;

				if (rawResult != null && BarcodeScanner.MWBgetResultType() == BarcodeScanner.MWB_RESULT_TYPE_MW) {

					BarcodeScanner.MWResults results = new BarcodeScanner.MWResults(rawResult);

					if (results.count > 0) {
						mwResult = results.getResult(0);
						rawResult = mwResult.bytes;
					}

				}
				
				// ignore results less than 4 characters - probably false
				// detection
				if (mwResult != null && mwResult.bytesLength > 0) {
					state = State.STOPPED;

					if (handler != null) {
						Message message = Message.obtain(handler,
								MSG_DECODE_SUCCESS, mwResult);
						message.sendToTarget();
					}
				} else {
					if (handler != null) {
						Message message = Message.obtain(handler,
								MSG_DECODE_FAILED);
						message.sendToTarget();
					}
				}
				activeThreads--;
				// Log.i("EllapseThreadTime",
				// String.valueOf(System.currentTimeMillis() - timeNow));
			}
		}).start();

	}

	public void handleDecode(MWResult mwResult) {

		
		String s = "";

		try {
			s = new String(mwResult.bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {

			s = "";
			for (int i = 0; i < mwResult.bytesLength; i++)
				s = s + (char) mwResult.bytes[i];
			e.printStackTrace();
		}

		int bcType = mwResult.type;
		String typeName = "";
		switch (bcType) {
		case BarcodeScanner.FOUND_25_INTERLEAVED:
			typeName = "Code 25";
			break;
		case BarcodeScanner.FOUND_25_STANDARD:
			typeName = "Code 25 Standard";
			break;
		case BarcodeScanner.FOUND_128:
			typeName = "Code 128";
			break;
		case BarcodeScanner.FOUND_39:
			typeName = "Code 39";
			break;
		case BarcodeScanner.FOUND_93:
			typeName = "Code 93";
			break;
		case BarcodeScanner.FOUND_AZTEC:
			typeName = "AZTEC";
			break;
		case BarcodeScanner.FOUND_DM:
			typeName = "Datamatrix";
			break;
		case BarcodeScanner.FOUND_EAN_13:
			typeName = "EAN 13";
			break;
		case BarcodeScanner.FOUND_EAN_8:
			typeName = "EAN 8";
			break;
		case BarcodeScanner.FOUND_NONE:
			typeName = "None";
			break;
		case BarcodeScanner.FOUND_RSS_14:
			typeName = "Databar 14";
			break;
		case BarcodeScanner.FOUND_RSS_14_STACK:
			typeName = "Databar 14 Stacked";
			break;
		case BarcodeScanner.FOUND_RSS_EXP:
			typeName = "Databar Expanded";
			break;
		case BarcodeScanner.FOUND_RSS_LIM:
			typeName = "Databar Limited";
			break;
		case BarcodeScanner.FOUND_UPC_A:
			typeName = "UPC A";
			break;
		case BarcodeScanner.FOUND_UPC_E:
			typeName = "UPC E";
			break;
		case BarcodeScanner.FOUND_PDF:
			typeName = "PDF417";
			break;
		case BarcodeScanner.FOUND_QR:
			typeName = "QR";
			break;
		case BarcodeScanner.FOUND_CODABAR:
			typeName = "Codabar";
			break;
		case BarcodeScanner.FOUND_128_GS1:
			typeName = "Code 128 GS1";
			break;
		case BarcodeScanner.FOUND_DOTCODE:
			typeName = "Dotcode";
			break;
		case BarcodeScanner.FOUND_ITF14:
			typeName = "ITF 14";
			break;
		case BarcodeScanner.FOUND_11:
			typeName = "Code 11";
			break;
		case BarcodeScanner.FOUND_MSI:
			typeName = "MSI Plessey";
			break;
		}

		Intent data = new Intent();
		data.putExtra("code", s);
		data.putExtra("type", typeName);
		data.putExtra("bytes", mwResult.bytes);
		data.putExtra("isGS1", mwResult.isGS1);

		if (closeCameraOnDetection) {
			setResult(1, data);
			finish();
		} else {
			//Log.i("Scanner result", "calling result handler");
			resultHandler.onResult(this, 1, 1, data);
		}

	}

	public static void resumeScanning() {
		if (instance.handler != null) {
			// Message message = Message.obtain(instance.handler,
			// MSG_DECODE_FAILED);
			// message.sendToTarget();
			state = State.PREVIEW;
		}
	}

	public static void closeScanner() {
		instance.setResult(0, null);
		instance.finish();
	}

	private void toggleFlash() {
		flashOn = !flashOn;
		updateFlash();
	}

	private void updateFlash() {
		if (flashVisible) {

			if (!CameraManager.get().isTorchAvailable()) {
				buttonFlash.setVisibility(View.GONE);
				return;

			}
			// else {
			// if (flashVisible)
			// buttonFlash.setVisibility(View.VISIBLE);
			// else
			// buttonFlash.setVisibility(View.GONE);
			// }

			if (flashOn) {

				buttonFlash.setImageBitmap(flashOnBitmap);

			} else {
				buttonFlash.setImageBitmap(flashOffBitmap);
			}

			CameraManager.get().setTorch(flashOn);

			buttonFlash.postInvalidate();
		}

	}

	private void toggleZoom() {

		zoomLevel++;
		if (zoomLevel > 2) {
			zoomLevel = 0;
		}

		updateZoom();
	}

	public void updateZoom() {

		if (param_ZoomLevel1 == 0 || param_ZoomLevel2 == 0) {
			firstZoom = 150;
			secondZoom = 300;
		} else {
			firstZoom = param_ZoomLevel1;
			secondZoom = param_ZoomLevel2;

			int maxZoom = CameraManager.get().getMaxZoom();

			if (maxZoom < secondZoom) {
				secondZoom = maxZoom;
			}
			if (maxZoom < firstZoom) {
				firstZoom = maxZoom;
			}

		}

		switch (zoomLevel) {
		case 0:
			CameraManager.get().setZoom(100);
			break;
		case 1:
			CameraManager.get().setZoom(firstZoom);
			break;
		case 2:
			CameraManager.get().setZoom(secondZoom);
			break;

		default:
			break;
		}
	}
}
