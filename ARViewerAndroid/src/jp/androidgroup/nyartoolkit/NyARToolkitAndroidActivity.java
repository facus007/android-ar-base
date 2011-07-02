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

import java.io.InputStream;
import java.util.ArrayList;

import dev.agustin.BaseRenderer;
import dev.agustin.JPCTARManager;

import dev.agustin.R;

import jp.androidgroup.nyartoolkit.hardware.CameraIF;
import jp.androidgroup.nyartoolkit.hardware.HT03ACamera;
import jp.androidgroup.nyartoolkit.hardware.UVCCamera;
import jp.androidgroup.nyartoolkit.hardware.N1Camera;
import jp.androidgroup.nyartoolkit.hardware.SocketCamera;
import jp.androidgroup.nyartoolkit.hardware.StaticCamera;
import jp.androidgroup.nyartoolkit.view.GLSurfaceView;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

public class NyARToolkitAndroidActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback {

	private static final boolean TRACE_ENABLED = false; //true enables trace logging, disable for performance
	private static final boolean FIXED_MAX_BRIGHTNESS = true;
	
	public static final String TAG = "NyARToolkitAndroid";
	
	public static final int CROP_MSG = 1;
    public static final int FIRST_TIME_INIT = 2;
	public static final int RESTART_PREVIEW = 3;
	public static final int CLEAR_SCREEN_DELAY = 4;
	public static final int SHOW_LOADING = 5;
	public static final int HIDE_LOADING = 6;
	public static final int KEEP = 7;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;
	

    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private boolean mTranslucentBackground = true;
    private boolean isYuv420spPreviewFormat = false;

	private CameraIF mCameraDevice;
    private SurfaceHolder mSurfaceHolder = null;
	private GLSurfaceView mGLSurfaceView;
	private BaseRenderer mRenderer;

	private boolean mFirstTimeInitialized;

	private PreviewCallback mPreviewCallback = null;


	private ARToolkitDrawer arToolkit = null;
	
	private Handler mHandler = null;

	private boolean isUseSerface = false;
	
	private boolean drawFlag = false;
	

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized) return;

        Log.d(TAG, "initializeFirstTime");

        // Create orientation listenter. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener =
                new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                // We keep the last known orientation. So if the user
                // first orient the camera then point the camera to
                // floor/sky, we still have the correct orientation.
                if (orientation != ORIENTATION_UNKNOWN) {
                    mLastOrientation = orientation;
                }
            }
        };
        mOrientationListener.enable();
        if (FIXED_MAX_BRIGHTNESS)
        {
	        WindowManager.LayoutParams lp = getWindow().getAttributes();
	        lp.screenBrightness = 100 / 100.0f;
	        getWindow().setAttributes(lp);
        }


        mFirstTimeInitialized = true;
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
		Log.d(TAG, "initializeSecondTime");

		// Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();
    }

//----------------------- Override Methods ------------------------
	
	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mHandler = new MainHandler();
		mPreviewCallback = new JpegPreviewCallback();
		// start tracing to "/sdcard/NyARToolkit.trace"
		if (TRACE_ENABLED)
			Debug.startMethodTracing("NyARToolkit");

		// Renderer
		/*
		String[] modelName = new String[2];
		modelName[0] = "droid.mqo";
		modelName[1] = "miku01.mqo";
		float[] modelScale = new float[] {0.008f, 0.01f};
		mRenderer = new ModelRenderer(mTranslucentBackground, getAssets(), modelName, modelScale);
		mRenderer.setMainHandler(mHandler);
		*/
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		
		String[] modelName = new String[2];
		modelName[0] = "mesh";
		float[] modelScale = new float[] {0.008f, 0.01f};
		mRenderer = new JPCTARManager(mTranslucentBackground, getAssets(), modelName, modelScale, this.getApplicationContext(), this);
		mRenderer.setMainHandler(mHandler);
		
		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// init Camera.
		if(getString(R.string.camera_name).equals("jp.androidgroup.nyartoolkit.hardware.SocketCamera")) {
			mCameraDevice = new SocketCamera(getString(R.string.server_addr),
					Integer.valueOf(getString(R.string.server_port)));
			setContentView(R.layout.camera);
			mGLSurfaceView = (GLSurfaceView) findViewById(R.id.GL_view);
			// OpenGL Verw
			mGLSurfaceView.setRenderer(mRenderer);
			mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
			
		} else if(getString(R.string.camera_name).equals("jp.androidgroup.nyartoolkit.hardware.StaticCamera")) {
			mCameraDevice = new StaticCamera(getAssets());
			setContentView(R.layout.camera);
			mGLSurfaceView = (GLSurfaceView) findViewById(R.id.GL_view);
			// OpenGL Verw
			mGLSurfaceView.setRenderer(mRenderer);
			mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
			
		} else if(getString(R.string.camera_name).equals("jp.androidgroup.nyartoolkit.hardware.HT03ACamera")) {
			
			/* Samsung i9000 camera, most android 2.1 devices */
			isUseSerface = true;
			isYuv420spPreviewFormat = true;

			if (mTranslucentBackground) {
				mGLSurfaceView = new GLSurfaceView(this);
				mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); 
				mGLSurfaceView.setRenderer(mRenderer);
				mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
			
				SurfaceView mSurfaceView = new SurfaceView(this);
				mCameraDevice = new HT03ACamera(this, mSurfaceView);
				PreferenceManager.getDefaultSharedPreferences(this);

				setContentView(mGLSurfaceView);
				addContentView(mSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				setContentView(R.layout.ht03acamera);
				SurfaceView mSurfaceView = (SurfaceView) findViewById(R.id.HT03A_camera_preview);
				mCameraDevice = new HT03ACamera(this, mSurfaceView);
				PreferenceManager.getDefaultSharedPreferences(this);

				mGLSurfaceView = (GLSurfaceView) findViewById(R.id.HT03A_GL_view);
				mGLSurfaceView.setRenderer(mRenderer);
			}
		} else if(getString(R.string.camera_name).equals("jp.androidgroup.nyartoolkit.hardware.UVCCamera")) {
			isUseSerface = true;
			
			if (mTranslucentBackground) {
				mGLSurfaceView = new GLSurfaceView(this);
				mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); 
				mGLSurfaceView.setRenderer(mRenderer);
				mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
			
				SurfaceView mSurfaceView = new SurfaceView(this);
				mCameraDevice = new UVCCamera(this, mSurfaceView);
				PreferenceManager.getDefaultSharedPreferences(this);

				setContentView(mGLSurfaceView);
				addContentView(mSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				setContentView(R.layout.uvccamera);
				SurfaceView mSurfaceView = (SurfaceView) findViewById(R.id.UVC_camera_preview);
				mCameraDevice = new UVCCamera(this, mSurfaceView);
				PreferenceManager.getDefaultSharedPreferences(this);

				mGLSurfaceView = (GLSurfaceView) findViewById(R.id.UVC_GL_view);
				mGLSurfaceView.setRenderer(mRenderer);
			}
		} else if (getString(R.string.camera_name).equals("jp.androidgroup.nyartoolkit.hardware.N1Camera")) {
			isUseSerface = true;
			isYuv420spPreviewFormat = true;

			if (mTranslucentBackground) {
				mGLSurfaceView = new GLSurfaceView(this);
				mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); 
				mGLSurfaceView.setRenderer(mRenderer);
				mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

				SurfaceView mSurfaceView = new SurfaceView(this);
				mCameraDevice = new N1Camera(this, mSurfaceView);
				PreferenceManager.getDefaultSharedPreferences(this);

				setContentView(mGLSurfaceView);
				addContentView(mSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			} else {
				setContentView(R.layout.n1camera);
				SurfaceView mSurfaceView = (SurfaceView) findViewById(R.id.N1_camera_preview);
				mCameraDevice = new N1Camera(this, mSurfaceView);
				PreferenceManager.getDefaultSharedPreferences(this);

				mGLSurfaceView = (GLSurfaceView) findViewById(R.id.N1_GL_view);
				mGLSurfaceView.setRenderer(mRenderer);
			}
		}
		
		mCameraDevice.setPreviewCallback(mPreviewCallback);
		
		// init ARToolkit.
		InputStream camePara = getResources().openRawResource(
				R.raw.camera_para);
		ArrayList<InputStream> patt = new ArrayList<InputStream>();
		patt.add(getResources().openRawResource(R.raw.patthiro));
		patt.add(getResources().openRawResource(R.raw.pattbeamer));
		//patt.add(getResources().openRawResource(R.raw.pattkanji));
		arToolkit = new ARToolkitDrawer(camePara, patt, mRenderer, mTranslucentBackground, isYuv420spPreviewFormat);
		
		
		//TODO init VoicePlayer for ARToolkit.
//		VoicePlayer mVoiceSound = new VoicePlayer();
//		mVoiceSound.initVoice(getResources().openRawResourceFd(R.raw.xxx_voice));
//		arToolkit.setVoicePlayer(mVoiceSound);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d("Activity", "OnStart() method called");
		mCameraDevice.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d("Activity", "OnResume() method called");
		mGLSurfaceView.onResume();
		
		mCameraDevice.onResume();

        if (mSurfaceHolder != null) {
            // If first time initialization is not finished, put it in the
            // message queue.
            if (!mFirstTimeInitialized) {
                mHandler.sendEmptyMessage(FIRST_TIME_INIT);
            } else {
                initializeSecondTime();
            }
        }
        
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
	}

	@Override
	public void onStop() {
		if (TRACE_ENABLED)
			Debug.stopMethodTracing();
		Log.d("Activity", "OnStop() method called");
		mCameraDevice.onStop();
		mOrientationListener.disable();
		//dispose some objects to facilitate GC
		mSurfaceHolder = null;
		mGLSurfaceView = null;
		mRenderer.onStop();
		mRenderer = null;
		arToolkit = null;
		mPreviewCallback = null;
		mCameraDevice = null;
		mHandler = null;
		System.gc();
		super.onStop();
	}

	@Override
	public void onPause() {
		Log.d("Activity", "OnPause() method called");
		if (mOrientationListener != null)
			mOrientationListener.disable();
		if (mCameraDevice != null)
			mCameraDevice.onPause();
		if (mGLSurfaceView != null)
			mGLSurfaceView.onPause();
		if (mRenderer != null)
			mRenderer.onPause();
		super.onPause();
	}
	
	@Override  
	protected void onDestroy() {
		Log.d("Activity", "OnDestroy() method called");
		//mCameraDevice.onDestroy();
		super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
	}
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;

			case MotionEvent.ACTION_MOVE:
				break;

			case MotionEvent.ACTION_UP:
				break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case CROP_MSG: {
				Intent intent = new Intent();
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						intent.putExtras(extras);
					}
				}
				setResult(resultCode, intent);
				finish();
				break;
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(TAG, "surfaceChanged");

		// Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        // We need to save the holder for later use, even when the mCameraDevice
        // is null. This could happen if onResume() is invoked after this
        // function.
        mSurfaceHolder = holder;

        if(!isUseSerface) {
			return;
		}
		
		if(mCameraDevice instanceof HT03ACamera) {
			HT03ACamera cam = (HT03ACamera)mCameraDevice;
			cam.surfaceChanged(holder, format, w, h);
		}
		else if(mCameraDevice instanceof UVCCamera) {
			UVCCamera cam = (UVCCamera)mCameraDevice;
			cam.surfaceChanged(holder, format, w, h);
		}
		else if (mCameraDevice instanceof N1Camera) {
			N1Camera cam = (N1Camera)mCameraDevice;
			cam.surfaceChanged(holder, format, w, h);
		}

        // If first time initialization is not finished, send a message to do
        // it later. We want to finish surfaceChanged as soon as possible to let
        // user see preview first.
        if (!mFirstTimeInitialized) {
            mHandler.sendEmptyMessage(FIRST_TIME_INIT);
        } else {
            initializeSecondTime();
        }
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(!isUseSerface) {
			return;
		}
		if(mCameraDevice instanceof HT03ACamera) {
			HT03ACamera cam = (HT03ACamera)mCameraDevice;
			cam.surfaceCreated(holder);
		}
		else if(mCameraDevice instanceof UVCCamera) {
			UVCCamera cam = (UVCCamera)mCameraDevice;
			cam.surfaceCreated(holder);
		}
		else if (mCameraDevice instanceof N1Camera) {
			N1Camera cam = (N1Camera)mCameraDevice;
			cam.surfaceCreated(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if(!isUseSerface) {
			return;
		}
		if(mCameraDevice instanceof HT03ACamera) {
			HT03ACamera cam = (HT03ACamera)mCameraDevice;
			cam.surfaceDestroyed(holder);
		}
		else if(mCameraDevice instanceof UVCCamera) {
			UVCCamera cam = (UVCCamera)mCameraDevice;
			cam.surfaceDestroyed(holder);
		}
		else if (mCameraDevice instanceof N1Camera) {
			N1Camera cam = (N1Camera)mCameraDevice;
			cam.surfaceDestroyed(holder);
		}

		mSurfaceHolder = null;
	}
	
	
	
	
// ---------------------- getter & setter ---------------------------	

	public int getLastOrientation() {
		return mLastOrientation;
	}
	
	
// ---------------------------- Utils ---------------------------------	

	public static int roundOrientation(int orientationInput) {
		Log.d("roundOrientation", "orientationInput:" + orientationInput);
		int orientation = orientationInput;
		if (orientation == -1)
			orientation = 0;
		
		orientation = orientation % 360;
		int retVal;
		if (orientation < (0*90) + 45) {
			retVal = 0;
		} else if (orientation < (1*90) + 45) {
			retVal = 90;
		} else if (orientation < (2*90) + 45) {
			retVal = 180;
		} else if (orientation < (3*90) + 45) {
			retVal = 270;
		} else {
			retVal = 0;
		}

		return retVal;
	}	
	public static Matrix GetDisplayMatrix(Bitmap b, ImageView v) {
		Matrix m = new Matrix();
		float bw = (float)b.getWidth();
		float bh = (float)b.getHeight();
		float vw = (float)v.getWidth();
		float vh = (float)v.getHeight();
		float scale, x, y;
		if (bw*vh > vw*bh) {
			scale = vh / bh;
			x = (vw - scale*bw)*0.5F;
			y = 0;
		} else {
			scale = vw / bw;
			x = 0;
			y = (vh - scale*bh)*0.5F;
		}
		m.setScale(scale, scale, 0.5F, 0.5F);
		m.postTranslate(x, y);
		return m;
	}
	
// ---------------------------- Callback classes ---------------------------------	
	
	
	private final class JpegPreviewCallback implements PreviewCallback {

		@Override
		public void onPreviewFrame(byte [] jpegData, Camera camera) {
			Log.d(TAG, "JpegPictureCallback.onPreviewFrame");
			
			if(jpegData != null) {
				Log.d(TAG, "data exist");
				arToolkit.draw(jpegData);
			} else {
				try {
					// The measure against over load. 
					Thread.sleep(500);
				} catch (InterruptedException e) {
					;
				}
			}
		}

	};	
	
	
// ---------------------------- Handler classes ---------------------------------	
	
	/** This Handler is used to post message back onto the main thread of the application */
	private class MainHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (mCameraDevice != null)
				mCameraDevice.handleMessage(msg);
			switch (msg.what) {
				case KEEP: {
					if (msg.obj != null) {
						mHandler.post((Runnable)msg.obj);
					}
					break;
				}
			
				case CLEAR_SCREEN_DELAY: {
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					break;
				}

                case FIRST_TIME_INIT: {
                    initializeFirstTime();
                    break;
                }

                case SHOW_LOADING: {
					showDialog(DIALOG_LOADING);
					break;
				}
				case HIDE_LOADING: {
					try {
						dismissDialog(DIALOG_LOADING);
						removeDialog(DIALOG_LOADING);
					} catch (IllegalArgumentException e) {
					}
					break;
				}
			}
		}
	}

	public Handler getMessageHandler() {
		return this.mHandler;
	}
	
	
	
	private static final int DIALOG_LOADING = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_LOADING: {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage("Loading ...");
			// dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.getWindow().setFlags
				(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				 WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			return dialog;
		}
		default:
			return super.onCreateDialog(id);
		}
	}
	
}

