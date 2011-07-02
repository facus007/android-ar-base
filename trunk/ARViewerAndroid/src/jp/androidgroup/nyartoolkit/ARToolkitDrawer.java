/* 
 * PROJECT: NyARToolkit for Android SDK
 * --------------------------------------------------------------------------------
 * This work is based on the original ARToolKit developed by
 *   Hirokazu Kato
 *   Mark Billinghurst
 *   HITLab, University of Washington, Seattle
 * http://www.hitl.washington.edu/artoolkit/
 *
 * NyARToolkit for Android SDK
 *   Copyright (C)2010 NyARToolkit for Android team
 *   Copyright (C)2010 R.Iizuka(nyatla)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For further information please contact.
 *  http://sourceforge.jp/projects/nyartoolkit-and/
 *  
 * This work is based on the NyARToolKit developed by
 *  R.Iizuka (nyatla)
 *    http://nyatla.jp/nyatoolkit/
 * 
 * contributor(s)
 *  Atsuo Igarashi
 */

package jp.androidgroup.nyartoolkit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Parameters;
import android.os.SystemClock;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;

import com.google.zxing.client.android.camera.CameraConfigurationManager;

import dev.agustin.BaseRenderer;
import dev.agustin.JPCTARManager;

import jp.androidgroup.nyartoolkit.model.VoicePlayer;

import jp.nyatla.nyartoolkit.NyARException;
import jp.nyatla.nyartoolkit.core.NyARCode;
import jp.nyatla.nyartoolkit.core.param.NyARParam;
import jp.nyatla.nyartoolkit.core.raster.rgb.NyARRgbRaster_RGB;
import jp.nyatla.nyartoolkit.core.transmat.NyARTransMatResult;
import jp.nyatla.nyartoolkit.core.types.NyARBufferType;
import jp.nyatla.nyartoolkit.detector.NyARDetectMarker;
import jp.nyatla.nyartoolkit.jogl.utils.NyARGLUtil;
import dev.agustin.R;

/**
 * ARToolKit Drawer
 *  マーカー認識部分などが含まれたクラス
 */
public class ARToolkitDrawer
{
	/**
	 * マーカーの最大パターン数
	 */
	private static final int PATT_MAX = 2;
	
	/**
	 * 一回の処理で検出できる最大のマーカー数。誤認識分も含まれるので少なすぎても駄目、多すぎると処理負荷増
	 */
	private static final int MARKER_MAX = 8;
	
	/**
	 * @see jp.nyatla.nyartoolkit.detector.NyARDetectMarker
	 */
	private NyARDetectMarker nya = null;
	
	/**
	 * @see jp.nyatla.nyartoolkit.core.raster.rgb.NyARRgbRaster_RGB
	 */
	private NyARRgbRaster_RGB raster = null;
	
	/**
	 * @see jp.nyatla.nyartoolkit.jogl.utils.NyARGLUtil
	 */
	private NyARGLUtil ar_util = null;
	
	/**
	 * カメラパラメータを保持するクラス
	 * @see jp.nyatla.nyartoolkit.core.param.NyARParam
	 */
	private NyARParam ar_param = null;
	
	/**
	 * マーカーのパターン情報を管理するクラス
	 * @see jp.nyatla.nyartoolkit.core.param.NyARParam
	 */
	private NyARCode[] ar_code = new NyARCode[PATT_MAX];
	
	/**
	 * 検出したマーカーの座標変換行列
	 * @see jp.nyatla.nyartoolkit.core.transmat.NyARTransMatResult
	 */
	private NyARTransMatResult ar_transmat_result = new NyARTransMatResult();

	
	private BaseRenderer mRenderer = null;
	
	private boolean mTranslucentBackground;
	
	private boolean isYuv420spPreviewFormat;
	
	private VoicePlayer mVoiceSound = null;

	private Parameters cameraParameters;
	
	static {
		System.loadLibrary("yuv420sp2rgb");
	}
	public static native void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height, int type);
	public static native void decodeYUV420SP(byte[] rgb, byte[] yuv420sp, int width, int height, int type);
	
	/**
	 * Constructor
	 * 
	 * @param camePara
	 * @param patt
	 * @param mRenderer2
	 * @param mTranslucentBackground
	 * @param isYuv420spPreviewFormat
	 */
	public ARToolkitDrawer(InputStream camePara, ArrayList<InputStream> patt, BaseRenderer mRenderer2, boolean mTranslucentBackground, boolean isYuv420spPreviewFormat) {
		this.mRenderer = mRenderer2;
		this.mTranslucentBackground = mTranslucentBackground;
		this.isYuv420spPreviewFormat = isYuv420spPreviewFormat;
		this.initialization(camePara, patt);
	}
	
	/**
	 * 初期化処理
	 * マーカーパターン、カメラパラメータはここで読み込む
	 */
	private void initialization(InputStream camePara, ArrayList<InputStream> patt) {
		try {
			for (int i = 0; i < PATT_MAX; i++) {
				// マーカーの分割数
				ar_code[i] = new NyARCode(16, 16);
				ar_code[i].loadARPatt(patt.get(i));
			}
			
			ar_param = new NyARParam();
			ar_param.loadARParam(camePara);
		} catch (Exception e) {
			Log.e("nyar", "resource loading failed", e);
		}
	}
	
	/**
	 * NyARToolKitの初期化
	 *  カメラパラメータの読み込み
	 * 
	 * @param w スクリーンサイズ(width)
	 * @param h スクリーンサイズ(height)
	 */
	private void createNyARTool(int w, int h) {
		// NyARToolkit setting.
		try {
			if (nya == null) {
				ar_util = new NyARGLUtil();
				ar_param.changeScreenSize(w, h);
				
				double[] width = new double[PATT_MAX];
				for (int i = 0; i < PATT_MAX; i++) {
					width[i] = 80.0;
				}
				// マーカーの枠線幅を変えることは可能。
				// NyARDetectMarker 内にコメントを追加したのでその部分を参照のこと
				nya = new NyARDetectMarker(ar_param, ar_code, width, PATT_MAX, NyARBufferType.BYTE1D_B8G8R8_24);
				nya.setContinueMode(true);
			}
			Log.d("nyar", "resources have been loaded");
		} catch (Exception e) {
			Log.e("nyar", "resource loading failed", e);
		}

	}

	/**
	 * 
	 * @param mVoiceSound
	 */
	public void setVoicePlayer(VoicePlayer mVoiceSound) {
		this.mVoiceSound = mVoiceSound;
	}
	
	/**
	 * 描画処理部分
	 *  メインループ処理と読み替えても良い
	 * @param data
	 */
	public void draw(byte[] data) {
		
		if(data == null) {
			Log.d("AR draw", "data= null");
			return;
		}
		Log.d("AR draw", "data.length= " + data.length);
		int width = CameraConfigurationManager.GetPreviewSize().width;
		int height = CameraConfigurationManager.GetPreviewSize().height;

		Bitmap bitmap = null;
		if (!isYuv420spPreviewFormat) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			if (bitmap == null) {
				Log.d("AR draw", "data is not BitMap data.");
				return;
			}

			if(bitmap.getHeight() < height) {
				bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bitmap == null) {
					Log.d("AR draw", "data is not BitMap data.");
					return;
				}
			}

			width = bitmap.getWidth();
			height = bitmap.getHeight();
			Log.d("AR draw", "bitmap width * height()= " + width + " * " + height);

			mRenderer.setBgBitmap(bitmap);

		} else if (!mTranslucentBackground) {
			// assume YUV420SP
			int[] rgb = new int[(width * height)];
			try {
				bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			} catch (Exception e) {
				Log.d("AR draw", "bitmap create error.");
				return;
			}		
			long time1 = SystemClock.uptimeMillis();
			// convert YUV420SP to ARGB
			decodeYUV420SP(rgb, data, width, height, 2);
			long time2 = SystemClock.uptimeMillis();
			Log.d("ARToolkitDrawer", "ARGB decode time: " + (time2 - time1) + "ms");
			bitmap.setPixels(rgb, 0, width, 0, 0, width, height);

			mRenderer.setBgBitmap(bitmap);
		}

		// start coordinates calculation.
		byte[] buf = new byte[width * height * 3];

		if (!isYuv420spPreviewFormat) {
			int[] rgb = new int[(width * height)];

			bitmap.getPixels(rgb, 0, width, 0, 0, width, height);

			// convert ARGB to RGB24
			for (int i = 0; i < rgb.length; i++) {
				byte r = (byte) (rgb[i] & 0x00FF0000 >> 16);
				byte g = (byte) (rgb[i] & 0x0000FF00 >> 8);
				byte b = (byte) (rgb[i] & 0x000000FF);
				buf[i * 3] = r;
				buf[i * 3 + 1] = g;
				buf[i * 3 + 2] = b;
			}
		} else {
			// assume YUV420SP
			long time1 = SystemClock.uptimeMillis();
			// convert YUV420SP to RGB24
			decodeYUV420SP(buf, data, width, height, 1);
			long time2 = SystemClock.uptimeMillis();
			Log.d("ARToolkitDrawer", "RGB decode time: " + (time2 - time1) + "ms");
		}
		
		float[][] resultf = new float[MARKER_MAX][16];

		int found_markers;
		int ar_code_index[] = new int[MARKER_MAX];
		
		createNyARTool(width, height);
		// Marker detection 
		try {
			Log.d("AR draw", "Marker detection.");
			raster = new NyARRgbRaster_RGB(width, height);
			raster.wrapBuffer(buf);
			found_markers = nya.detectMarkerLite(raster, 150); //AgustinS: changed from 100, Threshhold for converting image to B/W. Must be between 0 and 255
		} catch (NyARException e) {
			Log.e("AR draw", "marker detection failed", e);
			return;
		}
		
		// An OpenGL object will be drawn if matched.
		if (found_markers > 0) {
			Log.d("AR draw", "!!!!!!!!!!!exist marker." + found_markers + "!!!!!!!!!!!");
			// Projection transformation.
			float[] cameraRHf = new float[16];
			//ar_util.toCameraFrustumRHf(ar_param, cameraRHf); //not needed for JPCT

			if (found_markers > MARKER_MAX)
				found_markers = MARKER_MAX;
			for (int i = 0; i < found_markers; i++) {
				if (nya.getConfidence(i) < 0.60f)
					continue;
				try {
					ar_code_index[i] = nya.getARCodeIndex(i);
					NyARTransMatResult transmat_result = ar_transmat_result;
					nya.getTransmationMatrix(i, transmat_result);
					ar_util.toCameraViewRHf(transmat_result, resultf[i]);
				} catch (NyARException e) {
					Log.e("AR draw", "getCameraViewRH failed", e);
					return;
				}
			}
			mRenderer.objectPointChanged(found_markers, ar_code_index, resultf, cameraRHf);
			if(mVoiceSound != null)
				mVoiceSound.startVoice();
		} else {
			Log.d("AR draw", "not exist marker.");
			if(mVoiceSound != null)
				mVoiceSound.stopVoice();
			mRenderer.objectClear();
		}

	}
	
}
