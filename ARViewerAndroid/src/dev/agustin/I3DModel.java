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
