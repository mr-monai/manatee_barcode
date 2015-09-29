/*
 * Copyright (C) 2012  Manatee Works, Inc.
 *
 */

package com.manateeworks;

import java.nio.ByteOrder;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class BarcodeScanner {

	static {
		System.loadLibrary("BarcodeScannerLib");
	}

	/**
	 * @name Basic return values for API functions
	 * @{
	 */
	public static final int MWB_RT_OK = 0;
	public static final int MWB_RT_FAIL = -1;
	public static final int MWB_RT_NOT_SUPPORTED = -2;
	public static final int MWB_RT_BAD_PARAM = -3;

	/**
	 * @brief Code39 decoder flags value: require checksum check
	 */
	public static final int MWB_CFG_CODE39_REQUIRE_CHECKSUM = 0x2;
	/**/

	/**
	 * @brief Code39 decoder flags value: don't require stop symbol - can lead
	 *        to false results
	 */
	public static final int MWB_CFG_CODE39_DONT_REQUIRE_STOP = 0x4;
	/**/

	/**
	 * @brief Code39 decoder flags value: decode full ASCII
	 */
	public static final int MWB_CFG_CODE39_EXTENDED_MODE = 0x8;
	/**/

	/**
	 * @brief Code93 decoder flags value: decode full ASCII
	 */
	public static final int MWB_CFG_CODE93_EXTENDED_MODE = 0x8;
	/**/

	/**
	 * @brief Code25 decoder flags value: require checksum check
	 */
	public static final int MWB_CFG_CODE25_REQ_CHKSUM = 0x1;
	/**/

	/**
	 * @brief Code11 decoder flags value: require checksum check
	 *        MWB_CFG_CODE11_REQ_SINGLE_CHKSUM is set by default
	 */
	public static final int MWB_CFG_CODE11_REQ_SINGLE_CHKSUM = 0x1;
	public static final int MWB_CFG_CODE11_REQ_DOUBLE_CHKSUM = 0x2;
	/**/

	/**
	 * @brief MSI Plessey decoder flags value: require checksum check
	 *        MWB_CFG_MSI_REQ_10_CHKSUM is set by default
	 */
	public static final int MWB_CFG_MSI_REQ_10_CHKSUM = 0x01;
	public static final int MWB_CFG_MSI_REQ_1010_CHKSUM = 0x02;
	public static final int MWB_CFG_MSI_REQ_11_IBM_CHKSUM = 0x04;
	public static final int MWB_CFG_MSI_REQ_11_NCR_CHKSUM = 0x08;
	public static final int MWB_CFG_MSI_REQ_1110_IBM_CHKSUM = 0x10;
	public static final int MWB_CFG_MSI_REQ_1110_NCR_CHKSUM = 0x20;
	/**/

	/**
	 * @brief Codabar decoder flags value: include start/stop symbols in result
	 */
	public static final int MWB_CFG_CODABAR_INCLUDE_STARTSTOP = 0x1;
	/**/

	/**
	 * @brief Global decoder flags value: apply sharpening on input image
	 */
	public static final int MWB_CFG_GLOBAL_HORIZONTAL_SHARPENING = 0x01;
	public static final int MWB_CFG_GLOBAL_VERTICAL_SHARPENING = 0x02;
	public static final int MWB_CFG_GLOBAL_SHARPENING = 0x03;

	/**
	 * @brief Global decoder flags value: apply rotation on input image
	 */
	public static final int MWB_CFG_GLOBAL_ROTATE90 = 0x04;

	/**
	 * @name Bit mask identifiers for supported decoder types
	 * @{
	 */
	public static final int MWB_CODE_MASK_NONE = 0x00000000;
	public static final int MWB_CODE_MASK_QR = 0x00000001;
	public static final int MWB_CODE_MASK_DM = 0x00000002;
	public static final int MWB_CODE_MASK_RSS = 0x00000004;
	public static final int MWB_CODE_MASK_39 = 0x00000008;
	public static final int MWB_CODE_MASK_EANUPC = 0x00000010;
	public static final int MWB_CODE_MASK_128 = 0x00000020;
	public static final int MWB_CODE_MASK_PDF = 0x00000040;
	public static final int MWB_CODE_MASK_AZTEC = 0x00000080;
	public static final int MWB_CODE_MASK_25 = 0x00000100;
	public static final int MWB_CODE_MASK_93 = 0x00000200;
	public static final int MWB_CODE_MASK_CODABAR = 0x00000400;
	public static final int MWB_CODE_MASK_DOTCODE = 0x00000800;
	public static final int MWB_CODE_MASK_11 = 0x00001000;
	public static final int MWB_CODE_MASK_MSI = 0x00002000;
	public static final int MWB_CODE_MASK_ALL = 0xffffffff;
	/** @} */

	/**
	 * @name Bit mask identifiers for RSS decoder types
	 * @{
	 */
	public static final int MWB_SUBC_MASK_RSS_14 = 0x00000001;
	public static final int MWB_SUBC_MASK_RSS_LIM = 0x00000004;
	public static final int MWB_SUBC_MASK_RSS_EXP = 0x00000008;
	/** @} */

	/**
	 * @name Bit mask identifiers for Code 2 of 5 decoder types
	 * @{
	 */
	public static final int MWB_SUBC_MASK_C25_INTERLEAVED = 0x00000001;
	public static final int MWB_SUBC_MASK_C25_STANDARD = 0x00000002;
	public static final int MWB_SUBC_MASK_C25_ITF14 = 0x00000004;
	/** @} */

	/**
	 * @name Bit mask identifiers for UPC/EAN decoder types
	 * @{
	 */
	public static final int MWB_SUBC_MASK_EANUPC_EAN_13 = 0x00000001;
	public static final int MWB_SUBC_MASK_EANUPC_EAN_8 = 0x00000002;
	public static final int MWB_SUBC_MASK_EANUPC_UPC_A = 0x00000004;
	public static final int MWB_SUBC_MASK_EANUPC_UPC_E = 0x00000008;
	/** @} */

	/**
	 * @name Bit mask identifiers for 1D scanning direction
	 * @{
	 */
	public static final int MWB_SCANDIRECTION_HORIZONTAL = 0x00000001;
	public static final int MWB_SCANDIRECTION_VERTICAL = 0x00000002;
	public static final int MWB_SCANDIRECTION_OMNI = 0x00000004;
	public static final int MWB_SCANDIRECTION_AUTODETECT = 0x00000008;
	/** @} */

	public static final int FOUND_NONE = 0;
	public static final int FOUND_DM = 1;
	public static final int FOUND_39 = 2;
	public static final int FOUND_RSS_14 = 3;
	public static final int FOUND_RSS_14_STACK = 4;
	public static final int FOUND_RSS_LIM = 5;
	public static final int FOUND_RSS_EXP = 6;
	public static final int FOUND_EAN_13 = 7;
	public static final int FOUND_EAN_8 = 8;
	public static final int FOUND_UPC_A = 9;
	public static final int FOUND_UPC_E = 10;
	public static final int FOUND_128 = 11;
	public static final int FOUND_PDF = 12;
	public static final int FOUND_QR = 13;
	public static final int FOUND_AZTEC = 14;
	public static final int FOUND_25_INTERLEAVED = 15;
	public static final int FOUND_25_STANDARD = 16;
	public static final int FOUND_93 = 17;
	public static final int FOUND_CODABAR = 18;
	public static final int FOUND_DOTCODE = 19;
	public static final int FOUND_128_GS1 = 20;
	public static final int FOUND_ITF14 = 21;
	public static final int FOUND_11 = 22;
	public static final int FOUND_MSI = 23;
	public static final int FOUND_25_IATA = 24;

	/**
	 * @name Result structure constants
	 * @{
	 */

	/**
	 * @name Identifiers for result types
	 * @{
	 */

	public static final int MWB_RESULT_TYPE_RAW = 0x00000001;
	public static final int MWB_RESULT_TYPE_MW = 0x00000002;
	// public static final int MWB_RESULT_TYPE_JSON = 0x00000003; // not
	// supported yet

	/** @} */

	/**
	 * @name Identifiers for result fields types
	 * @{
	 */
	public static final int MWB_RESULT_FT_BYTES = 0x00000001;
	public static final int MWB_RESULT_FT_TEXT = 0x00000002;
	public static final int MWB_RESULT_FT_TYPE = 0x00000003;
	public static final int MWB_RESULT_FT_SUBTYPE = 0x00000004;
	public static final int MWB_RESULT_FT_SUCCESS = 0x00000005;
	public static final int MWB_RESULT_FT_ISGS1 = 0x00000006;
	public static final int MWB_RESULT_FT_LOCATION = 0x00000007;
	public static final int MWB_RESULT_FT_IMAGE_WIDTH = 0x00000008;
	public static final int MWB_RESULT_FT_IMAGE_HEIGHT = 0x00000009;
	public static final int MWB_RESULT_FT_PARSER_BYTES = 0x0000000A;

	/** @} */

	/** @} */

	public native static int MWBinit(Activity activity);

	public native static int MWBgetLibVersion();

	public native static int MWBgetSupportedCodes();

	public native static int MWBsetScanningRect(int codeMask, float left, float top, float width, float height);

	public native static float[] MWBgetScanningRectArray(int codeMask);

	public native static int MWBregisterCode(int codeMask, String userName, String key);

	public native static int MWBsetActiveCodes(int codeMask);

	public native static int MWBgetActiveCodes();

	public native static int MWBsetActiveSubcodes(int codeMask, int subMask);

	public native static int MWBcleanupLib();

	public native static int MWBgetLastType();

	public native static int MWBisLastGS1();

	public native static byte[] MWBscanGrayscaleImage(byte[] gray, int width, int height);

	public native static int MWBsetFlags(int codeMask, int flags);

	public native static int MWBsetLevel(int level);

	public native static int MWBsetDirection(int direction);

	public native static int MWBgetDirection();

	public native static int MWBvalidateVIN(byte[] vin);

	public native static float[] MWBgetBarcodeLocation();

	public native static int MWBsetResultType(int resultType);

	public native static int MWBgetResultType();

	public native static int MWBsetMinLength(int codeMask, int minLength);

	public static int MWBsetScanningRect(int codeMask, Rect rect) {

		return MWBsetScanningRect(codeMask, rect.left, rect.top, rect.width() + rect.left, rect.height() + rect.top);

	}

	public static RectF MWBgetScanningRect(int codeMask) {
		float f[] = MWBgetScanningRectArray(codeMask);
		return new RectF(f[0], f[1], f[2], f[3]);

	}

	public static final class MWLocation {

		public PointF p1;
		public PointF p2;
		public PointF p3;
		public PointF p4;

		public PointF points[];

		public MWLocation(float[] _points) {

			points = new PointF[4];

			for (int i = 0; i < 4; i++) {
				points[i] = new PointF();
				points[i].x = _points[i * 2];
				points[i].y = _points[i * 2 + 1];
			}
			p1 = new PointF();
			p2 = new PointF();
			p3 = new PointF();
			p4 = new PointF();

			p1.x = _points[0];
			p1.y = _points[1];
			p2.x = _points[2];
			p2.y = _points[3];
			p3.x = _points[4];
			p3.y = _points[5];
			p4.x = _points[6];
			p4.y = _points[7];

		}

	}

	public static final class MWResult {
		public String text;
		public byte[] bytes;
		public String encryptedResult;
		public int bytesLength;
		public int type;
		public int subtype;
		public int imageWidth;
		public int imageHeight;
		public boolean isGS1;
		public MWLocation locationPoints;

		public MWResult() {
			text = null;
			bytes = null;
			bytesLength = 0;
			type = 0;
			subtype = 0;
			isGS1 = false;
			locationPoints = null;
			imageWidth = 0;
			imageHeight = 0;
		}

	}

	public static final class MWResults {

		public int version;
		public ArrayList<MWResult> results;
		public int count;

		public MWResults(byte[] buffer) {
			results = new ArrayList<MWResult>();
			count = 0;
			version = 0;

			if (buffer[0] != 'M' || buffer[1] != 'W' || buffer[2] != 'R') {
				return;
			}

			version = buffer[3];

			count = buffer[4];

			int currentPos = 5;

			for (int i = 0; i < count; i++) {

				MWResult result = new MWResult();

				int fieldsCount = buffer[currentPos];
				currentPos++;
				for (int f = 0; f < fieldsCount; f++) {
					int fieldType = buffer[currentPos];
					int fieldNameLength = buffer[currentPos + 1];
					int fieldContentLength = 256 * (buffer[currentPos + 3 + fieldNameLength] & 0xFF)
							+ (buffer[currentPos + 2 + fieldNameLength] & 0xFF);
					String fieldName = null;

					if (fieldNameLength > 0) {
						fieldName = new String(buffer, currentPos + 2, fieldNameLength);
					}

					int contentPos = currentPos + fieldNameLength + 4;
					float locations[] = new float[8];
					switch (fieldType) {
					case MWB_RESULT_FT_TYPE:

						result.type = java.nio.ByteBuffer.wrap(buffer, contentPos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
						break;
					case MWB_RESULT_FT_SUBTYPE:
						result.subtype = java.nio.ByteBuffer.wrap(buffer, contentPos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
						break;
					case MWB_RESULT_FT_ISGS1:
						result.isGS1 = (java.nio.ByteBuffer.wrap(buffer, contentPos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() == 1);
						break;
					case MWB_RESULT_FT_IMAGE_WIDTH:
						result.imageWidth = java.nio.ByteBuffer.wrap(buffer, contentPos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
						break;
					case MWB_RESULT_FT_IMAGE_HEIGHT:
						result.imageHeight = java.nio.ByteBuffer.wrap(buffer, contentPos, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
						break;
					case MWB_RESULT_FT_LOCATION:
						for (int l = 0; l < 8; l++) {
							locations[l] = java.nio.ByteBuffer.wrap(buffer, contentPos + l * 4, 4).order(ByteOrder.LITTLE_ENDIAN)
									.getFloat();
						}
						result.locationPoints = new MWLocation(locations);

						break;
					case MWB_RESULT_FT_TEXT:
						result.text = new String(buffer, contentPos, fieldContentLength);
						break;
					case MWB_RESULT_FT_BYTES:
						result.bytes = new byte[fieldContentLength];
						result.bytesLength = fieldContentLength;
						for (int c = 0; c < fieldContentLength; c++) {
							result.bytes[c] = buffer[contentPos + c];
						}

						break;
					case MWB_RESULT_FT_PARSER_BYTES:
						result.encryptedResult = new String(buffer, contentPos, fieldContentLength);
						break;

					default:
						break;
					}

					currentPos += (fieldNameLength + fieldContentLength + 4);

				}

				results.add(result);

			}

		}

		public MWResult getResult(int index) {
			return results.get(index);
		}

	}

}
