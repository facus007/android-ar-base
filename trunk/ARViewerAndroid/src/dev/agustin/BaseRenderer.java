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
