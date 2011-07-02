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

import java.io.InputStream;

import android.app.Activity;
import android.content.res.Resources;
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

public class TestCube implements I3DModel {
	
	private World world;
	private Light sun;
	private Resources res;
	
	private Object3D cube; //represents the character as a whole
	
	private Activity activity;

	@Override
	public void loadModel() {
		initialize();
	}

	private void initialize() {
		Config.maxPolysVisible = 500; 
		this.res = this.activity.getResources();
		setLights();
		loadTextures();
		loadMeshes();
		world.buildAllObjects();
	}
	
	private void loadTextures()
	{
		TextureManager mgr = TextureManager.getInstance();
		mgr.flush();
		InputStream is = res.openRawResource(R.raw.cube2);
		Texture cubeTexture = new Texture(is); //do not use alpha
		cubeTexture.removeAlpha();
		mgr.addTexture("cube2.png", cubeTexture);
	}
	
	private void loadMeshes() {
		
		if (cube != null)
			return;
		try
		{
			InputStream stream = res.openRawResource(R.raw.cube);
			cube = Loader.loadSerializedObject(stream);
			cube.setName("cube");
			stream.close();
			world.addObject(cube);
		}
		catch (Exception ex)
		{
			Log.e("TestCube", "Exception", ex);
		}
	}

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
		if (this.cube == null)
			initialize();
		return this.cube;
	}

	@Override
	public void drawFrame() {
		
	}

	public void setModelVisibility(boolean b) {
		this.cube.setVisibility(b);
	}

	public void onStop()
	{
		//done some cleanup when closing the Android activity
		this.cube = null;
	}
	public void translatedTo(SimpleVector target)
	{
	}
	
	public void muteSound()
	{
	}
}
