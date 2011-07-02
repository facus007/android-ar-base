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
package dev.agustin.models;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.Log;

import com.threed.jpct.Config;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import dev.agustin.I3DModel;
import dev.agustin.R;

public class Beamer implements I3DModel {
	private World world;
	private Light sun;
	private Resources res;
	
	private Object3D beamer; //represents the model as a whole for positioning, etc
	
	private Object3D[] wheels;
	
	private Activity activity;
	
	private long lastTimerTick;
	
	@Override
	public void loadModel() {
		initialize();
	}

	private void initialize() {
		Config.maxPolysVisible = 50000; 
		this.res = this.activity.getResources();
		setLights();
		loadTextures();
		loadMeshes();
		world.buildAllObjects();
	}
	
	private void loadTextures()
	{
		/*
		TextureManager mgr = TextureManager.getInstance();
		mgr.flush();
		InputStream is = res.openRawResource(R.raw.cube2);
		Texture cubeTexture = new Texture(is); //do not use alpha
		cubeTexture.removeAlpha();
		mgr.addTexture("cube2.png", cubeTexture);
		*/
	}
	
	private void loadMeshes() 
	{	
		if (beamer != null)
			return;
		try
		{
			Log.d("Beamer", "Loading beamer model");
			int triangleCount = 0;
			
			Object3D[] beamerMesh = loadObjectArray(R.raw.carbody);
			//calculate total triangle count
			int length = 0;
			length = beamerMesh.length;
			for (int i=0; i<length; i++)
			{
				//find the wheels for animation
				Object3D obj = beamerMesh[i];
				triangleCount += obj.getMesh().getTriangleCount();
			}
			
			//load the wheels separately
			
			wheels = loadObjectArray(R.raw.wheels);
			length = wheels.length;
			for (int i=0; i<length; i++)
			{
				Object3D obj = wheels[i];
				triangleCount += obj.getMesh().getTriangleCount();
			}
			
			
			Log.d("Beamer", "Model has " + Integer.valueOf(triangleCount).toString() + " triangles");
			beamer = new Object3D(triangleCount);
			
			length = beamerMesh.length;
			for (int i=0; i<length; i++)
			{
				Object3D obj = beamerMesh[i];
				Log.d("Beamer", "loading " + obj.getName());
				beamer.addChild(obj);
			}
			
			length = wheels.length;
			for (int i=0; i<length;i++)
			{
				Object3D obj = wheels[i];
				beamer.addChild(obj);
			}
			
			beamer.calcBoundingBox();
			
			world.addObjects(beamerMesh);
			world.addObjects(wheels);
			world.addObject(beamer);
			
			Log.d("Beamer", "fully loaded!");
		}
		catch (Exception ex)
		{
			Log.e("Beamer", "Exception", ex);
		}
	}
	/*
	private Object3D[] loadZippedObjectArray(int resId) throws IOException
	{
		ZipInputStream stream = new ZipInputStream(res.openRawResource(resId));
		stream.getNextEntry();
		Object3D[] array = Loader.loadSerializedObjectArray(stream);
		stream.close();
		return array;
	}
	*/
	private Object3D[] loadObjectArray(int resId) throws IOException {
		InputStream stream = res.openRawResource(resId);
		Object3D[] array = Loader.loadSerializedObjectArray(stream);
		stream.close();
		return array;
	}

	/*
	private Object3D loadObject(int resId, String name) throws IOException {
		InputStream stream = res.openRawResource(resId);
		Object3D mesh = Loader.loadSerializedObject(stream);
		mesh.setName(name);
		stream.close();
		world.addObject(mesh);
		return mesh;
	}
	*/
	private void setLights() {
		if (sun == null)
		{
			//world.setAmbientLight(20, 20, 20);
			sun = new Light(world);
			sun.setIntensity(128f, 128f, 128f);
			SimpleVector sv = new SimpleVector();
			sv.set(0, 15, 0);
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
		if (this.beamer == null)
			initialize();
		return this.beamer;
	}

	@Override
	public void drawFrame() {
		/* rotate 2pi (360 degrees) in 1000 ms, calculate the rotation angle 
		 * given the time interval from the last frame 
		 */
		
		long now = SystemClock.uptimeMillis();
		long frameTime = now - lastTimerTick;
		float angle = (frameTime * ((float)Math.PI * 2))/1000f;
		
		for (int i=0;i<wheels.length;i++)
		{
			wheels[i].rotateX(-angle);
		}
		lastTimerTick = now;
		
	}

	public void setModelVisibility(boolean b) {
		this.beamer.setVisibility(b);
	}

	public void onStop()
	{
		//done some cleanup when closing the Android activity
		this.beamer = null;
	}
	public void translatedTo(SimpleVector target)
	{
	}
	
	public void muteSound()
	{
	}
	
}
