package dev.agustin;

import android.app.Activity;
import android.content.res.Resources;

import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

public interface I3DModel {

	void loadModel();

	void setWorld(World world);

	void setActivity(Activity act);

	
	Object3D getMasterObject();

	void drawFrame();
	
	void setModelVisibility(boolean b);

	void onStop();

	void translatedTo(SimpleVector target);

	void muteSound();

}
