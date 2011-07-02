/*
 * This file is part of android-ar-base.

    android-ar-base is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    android-ar-base is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with android-ar-base.  If not, see <http://www.gnu.org/licenses/>.

 */
package dev.agustin;

import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.androidgroup.nyartoolkit.NyARToolkitAndroidActivity;
import jp.androidgroup.nyartoolkit.view.GLSurfaceView;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.threed.jpct.Animation;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.World;


public class JPCTARManager extends BaseRenderer implements
		GLSurfaceView.Renderer {

	private static final int PATT_MAX = 2;
	private static final int MARKER_MAX = 8;

	private Bitmap bgBitmap = null;

	private int bgTextureName = -1;
	private int found_markers;
	private int[] ar_code_index = new int[MARKER_MAX];
	private float[][] resultf = new float[MARKER_MAX][16];
	private boolean useRHfp = false;

	private boolean bgChangep = false;
	private boolean drawp = false;

	private boolean reloadTexturep;
	private boolean modelChangep;

	private String[] modelName = new String[PATT_MAX];
	private float[] modelScale = new float[PATT_MAX];

	public int mWidth;
	public int mHeight;

	public static final int MODEL_FLAG = 0x01;
	public static final int BG_FLAG = 0x02;
	public static final int ALL_FLAG = MODEL_FLAG | BG_FLAG;
	public int deleteFlags = MODEL_FLAG | BG_FLAG;

	/* Begin 3D engine JPCT variables */
	private World world;
	private Activity master;
	private FrameBuffer fb = null;
	// the objects initialized here are created once and updated on every frame
	// to avoid excessive GC
	private SimpleVector objectPosition = new SimpleVector();
	private com.threed.jpct.Matrix rotMat = new com.threed.jpct.Matrix();
	private int currentAnimFrame = 0;
	private boolean mTranslucentBackground;
	private I3DModel model;
	
	private int objClearCounter = 0; //when this counter reaches objClearThreshold the 3d object is not drawn gets reset when reaches top or by objecPointChanged
	private static final int objClearThreshold = 4; //maximum frames without detection
	
	private static SimpleVector coordOffsetVector = new SimpleVector(0f, 1.4f, -2f);
	
	private static Texture bulbTexture = null;
	private static Texture hudScanningTexture = null;
	
	/* End 3D engine JPCT variables */

	public boolean checkDeleteAll() {
		return deleteFlags == ALL_FLAG;
	}

	public JPCTARManager(boolean useTranslucentBackground, AssetManager am,
			String[] modelName, float[] modelScale, Context context,
			Activity master) {
		this.master = master;
		InitEngine();
		for (int i = 0; i < PATT_MAX; i++) {
			this.modelName[i] = modelName[i];
			this.modelScale[i] = modelScale[i];
		}
		// TODO: turn this into dynamic when implementing QR tag code
		/*
		 * loadModel(modelName[0], modelScale[0]);
		 */
		Resources res = master.getResources();
		String modelClassName = res.getString(R.string.model_class);
		model = createModelInstance(modelClassName);
		if (!mTranslucentBackground)
			initBg();
		cameraReset();
	}

	private void loadDynamicModel() {
		/* Loads the model class by reading model_class from string.xml */
		modelChangep = true;
		// Load the mesh from resource
		if (world == null)
			world = new World();
		if (model == null)
			return;
		model.setWorld(world);
		model.setActivity(master);
		model.loadModel();
		float fov = world.getCamera().convertDEGAngleIntoFOV(45.66f);
		world.getCamera().setFOV(fov);
	}

	private I3DModel createModelInstance(String modelClassName) {
		try {
			Class<?> c = Class.forName(modelClassName);
			Object t = c.newInstance();
			return (I3DModel) t;
		} catch (Exception ex) {
			Log.e("JPCTARManager", "Exception", ex);
		}
		return null;
	}

	public void reloadTexture() {
		modelChangep = true;
		// Load the mesh from resource
		loadDynamicModel();
		//world.getCamera().setFOV(world.getCamera().convertDEGAngleIntoFOV(45));
		world.buildAllObjects();
	}

	private Handler mainHandler;

	@Override
	public void setMainHandler(Handler handler) {
		mainHandler = handler;
	}

	public void initModel(GL10 gl) {
		if (mainHandler != null) {
			mainHandler.sendMessage(mainHandler
					.obtainMessage(NyARToolkitAndroidActivity.SHOW_LOADING));
		}
		// TODO: Implement the deleteFlags when models disapear from scene
		if (mainHandler != null) {
			mainHandler.sendMessage(mainHandler
					.obtainMessage(NyARToolkitAndroidActivity.HIDE_LOADING));
		}
		modelChangep = false;
	}

	// BG
	@Override
	public void setBgBitmap(Bitmap bm) {
		if (mTranslucentBackground)
			return;
		synchronized (this) {
			bgBitmap = bm;
		}
		bgChangep = true;
	}

	@Override
	public void objectClear() {
		objClearCounter++;
		Log.d("objectClear", "Lost " + Integer.toString(objClearCounter) + " frames");
		if (objClearCounter > objClearThreshold)
		{
			drawp = false;
			objClearCounter = 0;
			Log.d("objectClear", "too many missing frames, destroying object");
		}
	}

	private void loadBitmap(GL10 gl) {
		if (bgTextureName != -1) {
			// bg.deleteTexture(gl, bgTextureName);
			// TODO: delete background texture (if ever any) here
			bgTextureName = -1;
			deleteFlags |= BG_FLAG;
		}
		if (bgBitmap != null) {
			// TODO: create a background texture (if ever any) here
			deleteFlags &= ~BG_FLAG;
		}
		bgChangep = false;
	}

	private void initBg() {
		// TODO: Initialize background texture (if any) here
		bgChangep = true;
	}

	public float[] zoomV = new float[4];
	public float[] upOriV = { 0.0f, 1.0f, 0.0f, 0.0f };
	public float[] lookV = new float[4];
	public float[] camRmtx = new float[16];

	public float[] camV = new float[4];
	public float[] upV = new float[4];
	public float ratio;

	private void InitEngine() {
		// Config.farPlane = Float.MAX_VALUE;
		Config.glTransparencyMul = 0.1f;
		Config.glTransparencyOffset = 0.1f;
		Config.useVBO = true;

		Texture.defaultToMipmapping(true);
		Texture.defaultTo4bpp(true);
	}

	public void cameraReset() {
		zoomV[0] = zoomV[1] = camV[0] = camV[1] = 0.0f;
		zoomV[2] = camV[2] = -500.0f;
		lookV[0] = lookV[1] = lookV[2] = 0.0f;
		upV[0] = upV[2] = 0.0f;
		upV[1] = 1.0f;
		Matrix.setIdentityM(camRmtx, 0);
	}

	@Override
	public void objectPointChanged(int found_markers, int[] ar_code_index,
			float[][] resultf, float[] cameraRHf) {
		synchronized (this) {
			this.found_markers = found_markers;
			for (int i = 0; i < MARKER_MAX; i++) {
				this.ar_code_index[i] = ar_code_index[i];
				System.arraycopy(resultf[i], 0, this.resultf[i], 0, 16);
			}
		}
		/*
		 * on each new position captured by the camera (several times slower
		 * than openGL) create an animation object with x keyframes where x is
		 * glframerate/cameraframerate pass this animation as an animation
		 * sequence for the target object
		 */
		currentAnimFrame = 0;
		SimpleVector transformTo = GetObjectPosition(this.resultf[0]);
		if (!transformTo.equals(SimpleVector.ORIGIN))
		{
			// set value for each step that object3D.animate() needs
			Animation anim = new Animation(1);
			anim.setInterpolationMethod(Animation.LINEAR);
			anim.setClampingMode(Animation.USE_CLAMPING);
			// set the initial key frame for the animation with the current
			// object position
			anim.createSubSequence("translation");
			// created a cloned object of the target and apply the translation
			// to it
			Object3D target = model.getMasterObject().cloneObject();
			target.clearTranslation(); 
			target.translate(transformTo);
			Log.d("objectPointChanged", "Translating object to " + transformTo.toString());
			rotMat.setDump(this.resultf[0]);
			rotMat.transformToGL();
			target.setRotationMatrix(rotMat);
			// build the object to calc the bounding box an normals, then get
			// the mesh and add it as a keyframe
			target.build();
			anim.addKeyFrame(target.getMesh());
			model.getMasterObject().setAnimationSequence(anim);
			useRHfp = true;
			drawp = true;
			objClearCounter = 0; //upon frame detection, reset counter
		}
		else
		{
			Log.d("objectPointChanged", "Null vector received, discarding...");
		}
	}

	private SimpleVector GetObjectPosition(float[] modelView) {
		/*
		 * The modelview matrix contains the 3x3 rotation matrix for the object
		 * and a translation in camera space where 0.0f;0.0f is the center of
		 * the screen and z is the relative distance to the marker see
		 * http://3dengine.org/Right-up-back_from_modelview
		 */
		float p = modelView[12];
		float q = modelView[13];
		float r = modelView[14];
		// return a 3D vector pointing at the center of the screen rotated 180
		// degrees on X
		// SimpleVector output = new SimpleVector(p,q,r);
		// recycle the same object to avoid excessive GC or heap growth
		objectPosition.set(p, q, r);
		objectPosition.rotateX((float) Math.PI);
		
		return centerBlenderCoords(objectPosition);
	}

	private SimpleVector centerBlenderCoords(SimpleVector objectPosition) {
		/* When using blender (http://www.blender.org) to import the objects as .obj format, some coordinates appear shifted from the center
		 * of the scene from blender to JPCT. (http://localhost/bugzilla3/show_bug.cgi?id=1)
		 * FIX: translate the marker position to match scene centers, this is easier than translating every object on the scene.
		 * NOTE: do not create a new object for the vector since this function will be called A LOT creating GC 
		 */
		objectPosition.add(coordOffsetVector);
		return objectPosition;
	}

	public void onDrawFrame(GL10 gl) {
		if (modelChangep) {
			initModel(gl);
			reloadTexturep = false;
		} else if (reloadTexturep) {
			reloadAllModelTextures();
		}
		if (bgChangep) {
			//Log.d("JPCTARManager", "in loadBitmap:");
			loadBitmap(gl);
		}

		fb.clear();

		drawBackgroundBitmap();
		if (drawp) {
			drawModels(gl);
			world.renderScene(fb);
			world.draw(fb);
		} else {
			Log.d("onDrawFrame", Long.valueOf(SystemClock.uptimeMillis()).toString() + " onDrawFrame: Frame skipped!");
			blitHUDScanning();
			// TODO: do some stuff if model must not be drawn on this frame, if
			// nothing, the model should remain on its last-known position for
			// this frame
			model.muteSound();
		}
		fb.display();
		makeFramerate();
	}
	
	private void blitHUDScanning()
	{
		if (hudScanningTexture == null)
		{
			InputStream is = master.getResources().openRawResource(R.raw.hudscanning);
			hudScanningTexture = new Texture(is);
		}
		fb.blit(hudScanningTexture, 0, 0, 200, 10, 512, 512, FrameBuffer.TRANSPARENT_BLITTING);
	}
	
	private void blitLightIcon()
	{
		//load the bulb texture, use a static variable to minimize GC
		if (bulbTexture == null)
		{
			InputStream is = master.getResources().openRawResource(R.raw.checklight);
			bulbTexture = new Texture(is);
		}
		fb.blit(bulbTexture, 0, 0, 0, 0, 128, 128, FrameBuffer.TRANSPARENT_BLITTING);
	}

	private void drawModels(GL10 gl) {
		int patt[] = new int[PATT_MAX];
		//TODO: use patt[] to differentiate which model to draw
		for (int i = 0; i < found_markers; i++) {
			if (useRHfp) {
				Log.d("JPCTARManager", "model is at " + model.getMasterObject().getTransformedCenter().toString());
				//prevent the model from showing at 0,0,0 when the application starts and there are no coordinates from camera yet
				/*
				if (model.getMasterObject().getTransformedCenter().equals(SimpleVector.ORIGIN))
				{
					Log.d("drawModels", "setModelVisibility:FALSE");
					model.setModelVisibility(false);
					//blit an icon to alert the user
					blitLightIcon();
				}
				else
				{
				*/
					model.setModelVisibility(true);
					// note the camera stays still and objects move in front of it
					// (when tagged)
					this.currentAnimFrame++;
					//animates x fixed frames
					//TOIMPROVE: do something dynamic here
					float framesToAnimate = 15f;
					float index = this.currentAnimFrame * (1f / framesToAnimate); 
					
					//if (index > 1) {
						setTransMatFromCamera();
						//Log.w("JPCT",Long.valueOf(SystemClock.uptimeMillis()).toString() + " Animation has reached last keyframe before new camera frame.");
						/*
					} else {
						//Log.d("JPCT",Long.valueOf(SystemClock.uptimeMillis()).toString() + " Moving object to " + model.getMasterObject().getTransformedCenter().toString());
						model.getMasterObject().animate(index);
						model.translatedTo(model.getMasterObject().getTransformedCenter());
					}
					*/
					model.drawFrame();
				//}
			} 
			if (patt[ar_code_index[i]] != -1) {
				//Log.d("JPCTARManager", "onDrawFrame: " + i + ",model: " + ar_code_index[i]);
				patt[ar_code_index[i]] = -1;
				// TODO: do some stuff to models before rendering if needed
			} else {
				//Log.d("JPCTARManager", "onDrawFrame: " + i + ", " + ar_code_index[i]);
				// TODO: handle behavior if found no model for this tag
			}
		}
	}

	private void setTransMatFromCamera() {
		model.getMasterObject().clearTranslation();
		model.getMasterObject().translate(GetObjectPosition(resultf[0]));
		rotMat.setDump(this.resultf[0]);
		rotMat.transformToGL();
		model.getMasterObject().setRotationMatrix(rotMat);
	}

	private void drawBackgroundBitmap() {
		// Bg
		if (bgBitmap != null && bgTextureName != -1) {
			// TODO: draw background bitmap (if any)
		}
	}

	private void reloadAllModelTextures() {
		//Log.d("JPCTARManager", "in reloadTexturep:");
		// TODO: Tell all models to reload textures here
		reloadTexturep = false;
	}

	// Frame counters for JPCT
	private int mFrames = 0;
	private float mFramerate;
	private long mStartTime;
	// frame counters for ARToolkit
	private int mCameraFrames = 0;
	private float mCameraFramerate;
	private long mCameraStartTime;

	private void makeCameraFramerate() {
		long time = SystemClock.uptimeMillis();

		synchronized (this) {
			mCameraFrames++;
			if (mCameraStartTime == 0) {
				mCameraStartTime = time;
			}
			if (time - mCameraStartTime >= 1) {
				mCameraFramerate = (float) (1000 * mCameraFrames)
						/ (float) (time - mCameraStartTime);
				Log.d("ARToolkit", "Framerate: " + mCameraFramerate + " ("
						+ (time - mCameraStartTime) + "ms)");
				mCameraFrames = 0;
				mCameraStartTime = time;
			}
		}
	}

	private void makeFramerate() {
		long time = SystemClock.uptimeMillis();

		synchronized (this) {
			mFrames++;
			if (mStartTime == 0) {
				mStartTime = time;
			}
			if (time - mStartTime >= 1) {
				mFramerate = (float) (1000 * mFrames)
						/ (float) (time - mStartTime);
				Log.d("Framerate", "Framerate: " + mFramerate + " ("
						+ (time - mStartTime) + "ms)");
				mFrames = 0;
				mStartTime = time;
			}
		}
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		// gl.glViewport(0, 0, width, height);
		if (fb != null) {
			fb.dispose();
		}
		fb = new FrameBuffer(gl, width, height);

		ratio = (float) width / height;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO: reset all model textures in here
		bgTextureName = -1;
		reloadTexture();
	}
	
	@Override
	public void onStop() {
		this.model.onStop();
		this.world.dispose();
		this.world = null;
	}
	@Override
	public void onPause()
	{
		this.model.onStop();
	}
}
