package dev.agustin.sound;

import android.content.Context;
import com.threed.jpct.*;
import android.util.Log;

import com.threed.jpct.SimpleVector;

public class SpatialSound {
	
	private World world;
	private class VolumeBalance
	{
		public float left;
		public float right;
	}
	
	private SoundSystem mSoundSystem;
	private boolean looping = false;
	
	public SpatialSound(Context context, int numSounds, World world)
	{
		mSoundSystem = new SoundSystem();
        mSoundSystem.initSounds(context, numSounds);
        this.world = world;
	}
	
	public void play3DPosition(int resourceId, boolean loop, SimpleVector objectPosition, SimpleVector cameraDirection)
	{
		//set volume properties depending on object angle relative to the camera direction
		int leftVolumeFactor;
		int rightVolumeFactor;
		//obtain the angle
		objectPosition.y = 0;
		cameraDirection.y = 0;
		//TODO: translate this angle into a coefficient for sound amplification
		VolumeBalance volume = Calculate3dVolumeLevel(objectPosition, cameraDirection);
		leftVolumeFactor = (int)(volume.left * 100);
		rightVolumeFactor = (int)(volume.right * 100);
		Log.d("SpatialSound", "leftVolumeFactor->" + leftVolumeFactor + " rightVolumeFactor->" + rightVolumeFactor);
		mSoundSystem.setMixerVolume(resourceId, leftVolumeFactor,rightVolumeFactor);
		//play the sound now
		mSoundSystem.addSound(resourceId);
		if (loop)
		{
			if (!looping)
			{
				mSoundSystem.playLoopedSound(resourceId);
				looping = true;
			}
		}
		else
		{
			mSoundSystem.playSound(resourceId);
		}
	}
	
	private VolumeBalance Calculate3dVolumeLevel(SimpleVector objectPosition, SimpleVector cameraDirection) {
		/*
		 * Calculate the angle from the camera center to the object using trig. 
		 * the angle is expressed in radians
		 */
		double angle = Math.atan2(objectPosition.normalize().z,objectPosition.normalize().x)-Math.atan2(cameraDirection.normalize().z,cameraDirection.normalize().x);
		/* 
		 * Apply this equation to obtain the reverse channel amplification quotient thats how to attenuate the opposite channel to the object position
		 * i.e if the object is closer to the right channel, the left channel should be attenuated and vice versa 
		 * min(max(sqrt(f-X^2/(((fov/2)*(PI/180)*t)^2),lo),1)
		 * min(max(root(f-x^2/(((45/2)*(3.14/180))*1.2)^2,2),lo),1)(KAlgebra format)
		 * adjust t to vary the angle that will result in a audio amplification, bigger values of t will result in a wider angle that will match the min amp.
		 * t widens or narrows the curve
		 * f moves the function curve up and down on the y axis being the top and down parts trimmed down to 1 and lo (max and min)
		 * lo is the lowest amplification level possible, decreasing this value widens the stereo separation
		 */
		//i.e: x->min(max(root(abs(1-x^2/((47.465378/2*3.14)/180*1)^2), 2), 0.25), 1)
		float fov = (float) (this.world.getCamera().getFOV()*(180/Math.PI));
		//some magic numbers here (see above)
		float t = 1f;
		double f = 1.04d;
		double lo = 0.25d;
		//(((fov/2)*(PI/180)*t)^2)
		double lowerTerm = Math.pow(((fov/2)*(Math.PI/180))*t,2d);
		//sqrt(1-X^2/(((fov/2)*(PI/180)*t)^2)
		double elipse = Math.sqrt(Math.abs(f-(Math.pow(angle,2d) / lowerTerm)));
		double invertVolumeLevel = Math.min(Math.max(elipse,lo),1d);
		Log.d("SpatialSound", "Angle: " + Double.toString(angle) + " Volume: " + Double.toString(invertVolumeLevel));
		//max(min(-0.04*Z+1.3, 1), 0.5)
		VolumeBalance balance = new VolumeBalance();
		if (angle > 0)
		{
			//if angle > 0 the object is on the left of the screen, must attenuate the right channel
			balance.left = (float)invertVolumeLevel;
			//the left channel responds to the distance factor
			balance.right = 1f; //max level
		}
		else
		{
			//attenuate left channel
			balance.right = (float)invertVolumeLevel;
			balance.left = 1f; //max level
		}
		Log.d("SpatialSound", "Balance: R->" + balance.right + " L->" + balance.left);
		return balance;
	}

	public void stop(int resourceID){
		mSoundSystem.stop(resourceID);
		looping = false;
	}
	
	public void pause(int resourceID) {
		mSoundSystem.pause(resourceID);
	}
}
