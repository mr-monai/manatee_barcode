package com.manateeworks.camera;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public final class CameraManager {

	public static float currentFPS = 0f;
	public static int REFOCUSING_DELAY = 2500;
	public static boolean USE_SAMSUNG_FOCUS_ZOOM_PATCH = false;

	private static CameraManager cameraManager;

	private final Context context;
	public final CameraConfigurationManager configManager;
	public Camera camera;
	private boolean initialized;
	public boolean previewing;
	private final boolean useOneShotPreviewCallback;
	public static boolean useBufferedCallback = true;
	private Camera.PreviewCallback cb;

	public static int mDesiredWidth = 1280;
	public static int mDesiredHeight = 720;

	public SurfaceHolder lastHolder;

	public Timer focusTimer;

	public static AutoFocusCallback afCallback;
	public static boolean refocusingActive = false;

	public static boolean DEBUG = false;
	public static String TAG = "CameraManager";

	public static void setDesiredPreviewSize(int width, int height) {
		mDesiredWidth = width;
		mDesiredHeight = height;
	}

	public Point getMaxResolution() {

		if (camera != null)
			return CameraConfigurationManager.getMaxResolution(camera
					.getParameters());
		else
			return null;

	}

	public Point getCurrentResolution() {

		if (camera != null) {

			Parameters params = camera.getParameters();

			Point res = new Point(params.getPreviewSize().width,
					params.getPreviewSize().height);

			return res;
		} else
			return null;

	}

	public Point getNormalResolution(Point normalRes) {

		if (camera != null)
			return CameraConfigurationManager.getCameraResolution(
					camera.getParameters(), normalRes);
		else
			return null;

	}

	public final PreviewCallback previewCallback;

	public static void init(Context context) {
		if (cameraManager == null) {
			cameraManager = new CameraManager(context);

		}
	}

	public static CameraManager get() {
		return cameraManager;
	}

	private CameraManager(Context context) {

		this.context = context;
		this.configManager = new CameraConfigurationManager(context);

		useOneShotPreviewCallback = true;
		useBufferedCallback = true;

		previewCallback = new PreviewCallback(configManager,
				useOneShotPreviewCallback);

	}

	public void openDriver(SurfaceHolder holder, boolean isPortrait)
			throws IOException {

		if (camera == null) {
			if (DEBUG)
				Log.i(TAG, "Camera opening...");
			camera = Camera.open();
			if (camera == null) {
				if (DEBUG)
					Log.i(TAG, "First camera open failed");
				camera = Camera.open(0);

				if (camera == null) {
					if (DEBUG)
						Log.i(TAG, "Secoond camera open failed");
					throw new IOException();
				}
			}

			if (DEBUG)
				Log.i(TAG, "Camera open success");

			if (android.os.Build.VERSION.SDK_INT >= 9) {
				setCameraDisplayOrientation(0, camera, isPortrait);
			} else {
				if (isPortrait)
					camera.setDisplayOrientation(90);
			}

			if (holder != null) {
				lastHolder = holder;
				camera.setPreviewDisplay(holder);
				if (DEBUG)
					Log.i(TAG, "Set camera current holder");
			} else {

				camera.setPreviewDisplay(lastHolder);
				if (DEBUG)
					Log.i(TAG, "Set camera last holder");
				if (lastHolder == null) {
					if (DEBUG)
						Log.i(TAG, "Camera last holder is NULL");
				} else {

				}

			}

			if (!initialized) {
				initialized = true;
				configManager.initFromCameraParameters(camera);
				if (DEBUG)
					Log.i(TAG, "configManager initialized");
			}
			configManager.setDesiredCameraParameters(camera);
			if (DEBUG)
				Log.i(TAG, "Camera set desired parameters");

		} else {
			if (DEBUG)
				Log.i(TAG, "Camera already opened");
		}

	}

	public int getMaxZoom() {

		if (camera == null)
			return -1;

		Parameters cp = camera.getParameters();
		if (!cp.isZoomSupported()) {
			return -1;
		}

		List<Integer> zoomRatios = cp.getZoomRatios();

		return zoomRatios.get(zoomRatios.size() - 1);

	}

	public void setZoom(int zoom) {

		if (camera == null)
			return;

		final Parameters cp = camera.getParameters();

		int minDist = 100000;
		int bestIndex = 0;

		if (zoom == -1) {
			int zoomIndex = cp.getZoom() - 1;

			if (zoomIndex >= 0) {
				zoom = cp.getZoomRatios().get(zoomIndex);
			}

		}

		List<Integer> zoomRatios = cp.getZoomRatios();

		if (zoomRatios != null) {

			for (int i = 0; i < zoomRatios.size(); i++) {
				int z = zoomRatios.get(i);

				if (Math.abs(z - zoom) < minDist) {
					minDist = Math.abs(z - zoom);
					bestIndex = i;
				}
			}

			final int fBestIndex = bestIndex;

			if (USE_SAMSUNG_FOCUS_ZOOM_PATCH) {

				if (bestIndex > 10) {

					// camera.cancelAutoFocus();
					stopFocusing();

					cp.setZoom(fBestIndex - 5);
					camera.setParameters(cp);
					camera.autoFocus(null);

					new Handler().postDelayed(new Runnable() {

						@Override
						public void run() {
							if (camera != null) {
								camera.cancelAutoFocus();
								cp.setZoom(fBestIndex);
								camera.setParameters(cp);
							}

							startFocusing();

						}
					}, 200);

				} else {
					stopFocusing();
					cp.setZoom(fBestIndex);
					camera.setParameters(cp);
					startFocusing();
				}

			} else {
				stopFocusing();
				cp.setZoom(fBestIndex);
				camera.setParameters(cp);
				startFocusing();
			}
		}

	}

	public boolean isTorchAvailable() {

		if (camera == null)
			return false;

		Parameters cp = camera.getParameters();
		List<String> flashModes = cp.getSupportedFlashModes();
		if (flashModes != null
				&& flashModes.contains(Parameters.FLASH_MODE_TORCH))
			return true;
		else
			return false;

	}

	public void setTorch(final boolean enabled) {

		if (camera == null)
			return;

		try {
			final Parameters cp = camera.getParameters();

			List<String> flashModes = cp.getSupportedFlashModes();

			if (flashModes != null
					&& flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
				camera.cancelAutoFocus();

				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						if (camera != null) {
							if (enabled)
								cp.setFlashMode(Parameters.FLASH_MODE_TORCH);
							else
								cp.setFlashMode(Parameters.FLASH_MODE_OFF);
							camera.setParameters(cp);
						}

					}
				}, 300);

			}
		} catch (Exception e) {

		}

	}

	public float[] getExposureCompensationRange() {

		if (camera == null)
			return null;

		try {

			Parameters cp = camera.getParameters();

			float ecStep = cp.getExposureCompensationStep();
			float minEC = cp.getMinExposureCompensation();
			float maxEC = cp.getMaxExposureCompensation();

			float[] res = new float[3];
			res[0] = minEC;
			res[1] = maxEC;
			res[2] = ecStep;

			return res;

		} catch (Exception e) {

			return null;
		}

	}

	public void setExposureCompensation(float value) {

		if (camera == null)
			return;

		try {

			Parameters cp = camera.getParameters();
			// int currentEC = cp.getExposureCompensation();
			float ecStep = cp.getExposureCompensationStep();
			float minEC = cp.getMinExposureCompensation();
			float maxEC = cp.getMaxExposureCompensation();

			if (value > maxEC)
				value = maxEC;
			if (value < minEC)
				value = minEC;

			cp.setExposureCompensation((int) value);

			camera.setParameters(cp);

			// Log.d("exposure compensation", String.valueOf(value));

		} catch (Exception e) {
			// Log.d("exposure compensation", "failed to set");
		}

	}

	public void closeDriver() {

		if (camera != null) {

			if (useBufferedCallback) {

			}

			camera.release();
			camera = null;
		}
	}

	public void startFocusing() {

		if (refocusingActive) {
			return;
		}
		refocusingActive = true;

		focusTimer = new Timer();
		focusTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (camera != null) {
					try {
						camera.autoFocus(null);
					} catch (Exception e) {

					}
				}

			}
		}, 500, REFOCUSING_DELAY);
	}

	public void stopFocusing() {

		camera.cancelAutoFocus();
		if (!refocusingActive) {
			return;
		}

		if (focusTimer != null) {
			focusTimer.cancel();
			focusTimer.purge();
		}

		refocusingActive = false;

	}

	public void startPreview() {
		Log.i("preview", "starting preview");
		if (camera != null && !previewing) {
			Log.i("preview", "preview started");
			camera.startPreview();
			previewing = true;

			startFocusing();

		}
	}

	public void stopPreview() {
		if (camera != null && previewing) {

			if (useBufferedCallback) {
				previewCallback.setPreviewCallback(camera, null, 0, 0);
			}
			if (!useOneShotPreviewCallback) {
				camera.setPreviewCallback(null);
			}
			stopFocusing();
			camera.stopPreview();
			previewCallback.setHandler(null, 0);
			previewing = false;
		}
	}

	public void requestPreviewFrame(Handler handler, int message) {

		if (camera != null && previewing) {
			previewCallback.setHandler(handler, message);
			if (useBufferedCallback) {
				// if (cb == null) {

				cb = previewCallback.getCallback();
				// }
				previewCallback.setPreviewCallback(camera, cb,
						configManager.cameraResolution.x,
						configManager.cameraResolution.y);
			} else if (useOneShotPreviewCallback) {
				camera.setOneShotPreviewCallback(previewCallback);
			} else {
				camera.setPreviewCallback(previewCallback);
			}
		}
	}

	public void requestAutoFocus(Handler handler, int message) {
		/*
		 * if (camera != null && previewing) {
		 * autoFocusCallback.setHandler(handler, message); Log.d(TAG,
		 * "Requesting auto-focus callback");
		 * camera.autoFocus(autoFocusCallback);
		 * 
		 * }
		 */
	}

	public int getDeviceDefaultOrientation() {

		WindowManager windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		Configuration config = context.getResources().getConfiguration();

		int rotation = windowManager.getDefaultDisplay().getRotation();

		if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
				|| ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
			return Configuration.ORIENTATION_LANDSCAPE;
		} else {
			return Configuration.ORIENTATION_PORTRAIT;
		}
	}

	public void updateCameraOrientation(int rotation) {

		if (camera == null) {
			return;
		}

		int deviceOrientation = getDeviceDefaultOrientation();

		if (deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
			switch (rotation) {
			case Surface.ROTATION_0:
				camera.setDisplayOrientation(90);
				break;
			case Surface.ROTATION_180:
				camera.setDisplayOrientation(270);
				break;
			case Surface.ROTATION_270:
				camera.setDisplayOrientation(180);
				break;
			case Surface.ROTATION_90:
				camera.setDisplayOrientation(0);
				break;

			default:
				break;
			}
		} else {

			switch (rotation) {
			case Surface.ROTATION_0:
				camera.setDisplayOrientation(0);
				break;
			case Surface.ROTATION_180:
				camera.setDisplayOrientation(180);
				break;
			case Surface.ROTATION_270:
				camera.setDisplayOrientation(90);
				break;
			case Surface.ROTATION_90:
				camera.setDisplayOrientation(270);
				break;

			default:
				break;
			}

		}

	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public void setCameraDisplayOrientation(int cameraId,
			android.hardware.Camera camera, boolean isPortrait) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);

		Display d = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		int rotation = d.getRotation();

		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	public Bitmap renderCroppedGreyscaleBitmap(byte[] data, int width,
			int height) {
		int[] pixels = new int[width * height];
		byte[] yuv = data;
		int row = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int grey = yuv[row + x] & 0xff;
				pixels[row + x] = 0xFF000000 | (grey * 0x00010101);
			}
			row += width;
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

}

final class CameraConfigurationManager {

	private static final String TAG = CameraConfigurationManager.class
			.getSimpleName();

	private final Context context;
	public static Point screenResolution;
	public Point cameraResolution;
	private int previewFormat;
	private String previewFormatString;

	CameraConfigurationManager(Context context) {
		this.context = context;
	}

	/**
	 * Reads, one time, values from the camera that are needed by the app.
	 */
	void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		previewFormat = parameters.getPreviewFormat();
		previewFormatString = parameters.get("preview-format");
		Log.d(TAG, "Default preview format: " + previewFormat + '/'
				+ previewFormatString);
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		screenResolution = new Point(display.getWidth(), display.getHeight());
		Log.d(TAG, "Screen resolution: " + screenResolution);
		cameraResolution = getCameraResolution(parameters, new Point(
				CameraManager.mDesiredWidth, CameraManager.mDesiredHeight));
		Log.d(TAG, "Camera resolution: " + cameraResolution);
	}

	void setDesiredCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		cameraResolution = getCameraResolution(parameters, new Point(
				CameraManager.mDesiredWidth, CameraManager.mDesiredHeight));
		Log.d(TAG, "Setting preview size: " + cameraResolution);
		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);

		/*
		 * try { String vs = parameters.get("anti-shake"); if (vs != null) {
		 * parameters.set("anti-shake", "1"); } } catch (Exception e){ }
		 */

		try {
			String vs = parameters.get("video-stabilization");
			if (vs != null) {
				parameters.set("video-stabilization", "true");
			}
		} catch (Exception e) {
		}

		try {
			String vs = parameters.get("video-stabilization-ocr");
			if (vs != null) {
				parameters.set("video-stabilization-ocr", "true");
			}
		} catch (Exception e) {
		}

		try {
			String vs = parameters.get("touch-af-aec-values");
			if (vs != null) {
				parameters.set("touch-af-aec-values", "touch-on");
			}
		} catch (Exception e) {
		}

		String focusMode = parameters.getFocusMode();

		try {
			parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
			// parameters.setFocusMode("macro");
			camera.setParameters(parameters);
		} catch (Exception e) {

			try {
				parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
				camera.setParameters(parameters);
			} catch (Exception e2) {
				parameters.setFocusMode(focusMode);
			}

		}

		try {
			List<int[]> supportedFPS = parameters.getSupportedPreviewFpsRange();

			int maxFps = -1;
			int maxFpsIndex = -1;
			for (int i = 0; i < supportedFPS.size(); i++) {
				int[] sr = supportedFPS.get(i);
				if (sr[1] > maxFps) {
					maxFps = sr[1];
					maxFpsIndex = i;
				}
			}

			parameters.setPreviewFpsRange(supportedFPS.get(maxFpsIndex)[0],
					supportedFPS.get(maxFpsIndex)[1]);

		} catch (Exception e) {
		}

		Log.d(TAG, "Camera parameters flat: " + parameters.flatten());
		camera.setParameters(parameters);
	}

	public Point getCameraResolution() {
		return cameraResolution;
	}

	Point getScreenResolution() {
		return screenResolution;
	}

	int getPreviewFormat() {
		return previewFormat;
	}

	String getPreviewFormatString() {
		return previewFormatString;
	}

	public static Point getMaxResolution(Camera.Parameters parameters) {

		List<Size> sizes = parameters.getSupportedPreviewSizes();

		int maxIndex = -1;
		int maxSize = 0;

		for (int i = 0; i < sizes.size(); i++) {
			int size = sizes.get(i).width * sizes.get(i).height;
			if (size > maxSize) {
				maxSize = size;
				maxIndex = i;
			}
		}

		return new Point(sizes.get(maxIndex).width, sizes.get(maxIndex).height);
	}

	public static Point getCameraResolution(Camera.Parameters parameters,
			Point desiredResolution) {

		String previewSizeValueString = parameters.get("preview-size-values");

		if (previewSizeValueString == null) {
			previewSizeValueString = parameters.get("preview-size-value");
		}

		Point cameraResolution = null;

		/*
		 * if (CameraManager.mDesiredWidth == 0) CameraManager.mDesiredWidth =
		 * desiredResolution.x; if (CameraManager.mDesiredHeight == 0)
		 * CameraManager.mDesiredHeight = desiredResolution.y;
		 */

		List<Size> sizes = parameters.getSupportedPreviewSizes();

		int minDif = 99999;
		int minIndex = -1;

		float screenAR = ((float) CameraConfigurationManager.screenResolution.x)
				/ CameraConfigurationManager.screenResolution.y;

		for (int i = 0; i < sizes.size(); i++) {

			float resAR = ((float) sizes.get(i).width) / sizes.get(i).height;

			int dif = Math.abs(sizes.get(i).width - desiredResolution.x)
					+ Math.abs(sizes.get(i).height - desiredResolution.y);

			// int dif = Math.abs((sizes.get(i).width * sizes.get(i).height) -
			// (CameraManager.mDesiredWidth * CameraManager.mDesiredHeight));

			if (dif < minDif) {
				minDif = dif;
				minIndex = i;
			}
		}

		float desiredTotalSize = desiredResolution.x * desiredResolution.y;
		float bestARdifference = 100;

		for (int i = 0; i < sizes.size(); i++) {

			float resAR = ((float) sizes.get(i).width) / sizes.get(i).height;

			float totalSize = sizes.get(i).width * sizes.get(i).height;

			float difference;

			if (totalSize >= desiredTotalSize) {
				difference = totalSize / desiredTotalSize;
			} else {
				difference = desiredTotalSize / totalSize;
			}

			float ARdifference;

			if (resAR >= screenAR) {
				ARdifference = resAR / screenAR;
			} else {
				ARdifference = screenAR / resAR;
			}

			if (difference < 1.1 && ARdifference < bestARdifference) {
				bestARdifference = ARdifference;
				minIndex = i;
			}

		}

		cameraResolution = new Point(sizes.get(minIndex).width,
				sizes.get(minIndex).height);

		return cameraResolution;
	}

}

final class PreviewCallback implements Camera.PreviewCallback {

	int fpscount;

	long lasttime = 0;

	private final CameraConfigurationManager configManager;
	private final boolean useOneShotPreviewCallback;
	public Handler previewHandler;
	public int previewMessage;

	public byte[][] frameBuffers;
	public int fbCounter = 0;
	public boolean callbackActive = false;

	PreviewCallback(CameraConfigurationManager configManager,
			boolean useOneShotPreviewCallback) {
		this.configManager = configManager;
		this.useOneShotPreviewCallback = useOneShotPreviewCallback;
	}

	void setHandler(Handler previewHandler, int previewMessage) {
		this.previewHandler = previewHandler;
		this.previewMessage = previewMessage;
	}

	public void onPreviewFrame(byte[] data, Camera camera) {

		updateFps();

		Point cameraResolution = configManager.getCameraResolution();
		if (!useOneShotPreviewCallback) {
			camera.setPreviewCallback(null);
		}
		if (previewHandler != null) {
			Message message = previewHandler.obtainMessage(previewMessage,
					cameraResolution.x, cameraResolution.y, data);
			message.sendToTarget();
			previewHandler = null;
		}
	}

	public int setPreviewCallback(Camera camera,
			Camera.PreviewCallback callback, int width, int height) {

		if (callback != null) {
			if (frameBuffers == null) {
				// add 10% additional space for any case
				frameBuffers = new byte[2][width * height * 2 * 110 / 100];
				fbCounter = 0;
				Log.i("preview resolution", String.valueOf(width) + "x"
						+ String.valueOf(height));

			}
			if (!callbackActive) {
				camera.setPreviewCallbackWithBuffer(callback);
				callbackActive = true;
			}
			// CameraDriver.bufferProccessed = -1;
			camera.addCallbackBuffer(frameBuffers[fbCounter]);
			fbCounter = 1 - fbCounter;
		} else {
			camera.setPreviewCallbackWithBuffer(callback);
			callbackActive = false;
		}

		if (callback == null) {
			frameBuffers = null;
			System.gc();
		}

		return 0;
	}

	public Camera.PreviewCallback getCallback() {

		return new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {

				updateFps();

				Point cameraResolution = configManager.getCameraResolution();

				if (CameraManager.useBufferedCallback) {
					// camera.addCallbackBuffer(frameBuffers[fbCounter]);
					// fbCounter = 1 - fbCounter;
					setPreviewCallback(camera, this, cameraResolution.x,
							cameraResolution.y);
				}

				if (previewHandler != null) {
					Message message = previewHandler.obtainMessage(
							previewMessage, cameraResolution.x,
							cameraResolution.y, data);
					message.sendToTarget();
					if (!CameraManager.useBufferedCallback) {
						previewHandler = null;
					}
				}
			}
		};

	}

	private void updateFps() {
		if (lasttime == 0) {
			lasttime = System.currentTimeMillis();
			fpscount = 0;
			CameraManager.currentFPS = 0;
		} else {
			long delay = System.currentTimeMillis() - lasttime;
			if (delay > 2000) {
				lasttime = System.currentTimeMillis();
				CameraManager.currentFPS = fpscount * 10000 / delay;
				CameraManager.currentFPS /= 10;
				fpscount = 0;
			}
		}
		fpscount++;
	}

}
