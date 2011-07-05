package dev.agustin.models;

import java.io.InputStream;

import android.app.Activity;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.Log;

import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import dev.agustin.I3DModel;
import dev.agustin.R;
import dev.agustin.sound.SpatialSound;

public class Helicopter implements I3DModel {
	
	private World world;
	private Light sun;
	private Resources res;
	
	private Object3D helicopter; //represents the helicopter as a whole
	private Object3D blade;
	private Object3D frame;
	private Object3D glass;
	private Object3D tailRotor;
	
	private long lastTimerTick;
	private Activity activity;
	private SpatialSound sound;

	@Override
	public void loadModel() {
		initialize();
	}

	private void initialize() {
		this.res = this.activity.getResources();
		setLights();
		loadMeshes();
		loadTextures();
		world.buildAllObjects();
		sound = new SpatialSound(this.activity.getBaseContext(),1,world);
	}
	
	private void loadTextures()
	{
		TextureManager mgr = TextureManager.getInstance();
		mgr.flush();
		Texture bodyTexture = new Texture(res.openRawResource(R.raw.body));
		Texture bladeTexture = new Texture(res.openRawResource(R.raw.blade));
		mgr.addTexture("blade.jpg", bladeTexture);
		mgr.addTexture("Body.jpg", bodyTexture);
		blade.setTexture("blade.jpg");
		tailRotor.setTexture("blade.jpg");
		frame.setTexture("Body.jpg");
	}
	
	private void loadMeshes() {
		
		if (helicopter != null)
			return;
		try
		{
			InputStream stream = res.openRawResource(R.raw.helicopterblade);
			
			blade = Loader.loadSerializedObject(stream);
			blade.setName("blade");
			stream.close();
			
			stream = res.openRawResource(R.raw.helicopterframe);
			frame = Loader.loadSerializedObject(stream);
			frame.setName("frame");
			stream.close();
			
			stream = res.openRawResource(R.raw.helicopterglass);
			glass = Loader.loadSerializedObject(stream);
			glass.setName("glass");
			stream.close();
			
			stream = res.openRawResource(R.raw.helicoptertailrotor);
			tailRotor = Loader.loadSerializedObject(stream);
			tailRotor.setName("tailRotor");
			stream.close();
			
			int triangles = frame.getMesh().getTriangleCount() + blade.getMesh().getTriangleCount() + 
							glass.getMesh().getTriangleCount() + tailRotor.getMesh().getTriangleCount();
			helicopter = new Object3D(triangles);
			helicopter.addChild(glass);
			helicopter.addChild(tailRotor);
			helicopter.addChild(blade);
			helicopter.addChild(frame);
			helicopter.calcBoundingBox();
			
			world.addObject(glass);
			world.addObject(tailRotor);
			world.addObject(blade);
			world.addObject(frame);
			world.addObject(helicopter);
		}
		catch (Exception ex)
		{
			Log.e("Helicopter", "Exception", ex);
		}
	}

	private void setLights() {
		if (sun == null)
		{
			world.setAmbientLight(20, 20, 20);
			sun = new Light(world);
			sun.setIntensity(250, 250, 250);
			SimpleVector sv = new SimpleVector();
			sv.set(0, 0, 0);
			sun.setPosition(sv); //elevate the sun
		}
	}

	@Override
	public void setWorld(World world) {
		this.world = world;
		
	}

	@Override
	public void setActivity(Activity act) {
		this.activity = act;
	}

	@Override
	public Object3D getMasterObject() {
		if (this.helicopter == null)
			initialize();
		return this.helicopter;
	}

	@Override
	public void drawFrame() {
		/* rotate 2pi (360 degrees) in 1000 ms, calculate the rotation angle 
		 * given the time interval from the last frame 
		 */
		long now = SystemClock.uptimeMillis();
		long frameTime = now - lastTimerTick;
		float angle = (frameTime * ((float)Math.PI * 2))/500f;
		blade.rotateZ(angle);
		tailRotor.rotateY(angle);
		lastTimerTick = now;
	}

	public void setModelVisibility(boolean b) {
		this.blade.setVisibility(b);
		this.frame.setVisibility(b);
		this.glass.setVisibility(b);
		this.tailRotor.setVisibility(b);
	}

	public void onStop()
	{
		//stop any playing sounds
		sound.stop(R.raw.choppersound);
		//dome some cleanup when closing the Android activity
		this.blade = null;
		this.frame = null;
		this.glass = null;
		this.tailRotor = null;
	}
	public void translatedTo(SimpleVector target)
	{
		//TODO: enable sound when correcting the bug that keeps playing when app ended
		//Log.d("Helicopter", "playing 3d position at " + target.toString());
		//sound.play3DPosition(R.raw.choppersound, true, target, this.world.getCamera().getDirection());
	}
	
	public void muteSound()
	{
		//this method is called when the 3d object is now drawn on the screen, mute the sound
		//the sound will get re-enabled next time the object is drawn by the translatedTo(...) method
		sound.pause(R.raw.choppersound);
	}

}

