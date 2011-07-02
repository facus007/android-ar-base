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

import android.graphics.Bitmap;
import android.os.Handler;

public abstract class BaseRenderer {
	public abstract void setMainHandler(Handler mHandler);

	public abstract void setBgBitmap(Bitmap bitmap);

	public abstract void objectPointChanged(int foundMarkers, int[] arCodeIndex,
			float[][] resultf, float[] cameraRHf);

	public abstract void objectClear() ;

	public void onStop() {
		//no default behaviour implemented. Override this method if needed. 
	}

	public void onPause() {
		//no default behaviour implemented. Override this method if needed.
	}
}
