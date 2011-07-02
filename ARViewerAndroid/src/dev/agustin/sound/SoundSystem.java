package dev.agustin.sound;

import java.util.Hashtable;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;



public class SoundSystem {
	
	private  SoundPool mSoundPool;  
	private  AudioManager  mAudioManager;
	private  Context mContext;
	private float leftVolume;
	private float rightVolume;
	private Hashtable<Integer, Sound> soundIds;
	
	private class Sound
	{
		public int soundId;
		public int streamId = -1;
		public Sound(int soundId)
		{
			this.soundId = soundId;
		}
	}
		
	public void initSounds(Context theContext, int numSounds) {
		soundIds = new Hashtable<Integer, Sound>(numSounds);
		 mContext = theContext;
	     mSoundPool = new SoundPool(numSounds, AudioManager.STREAM_MUSIC, 0); 
	     mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
	} 
	
	public int addSound(int soundResourceID)
	{
		//if the soundResourceID is already loaded return its corresponding soundid
		if (soundIds.containsKey(Integer.valueOf(soundResourceID)))
		{
			Sound sound = soundIds.get(Integer.valueOf(soundResourceID));
			return sound.soundId;
		}
		else
		{
			Log.d("SoundSystem", "Adding sound " + Integer.valueOf(soundResourceID).toString());
			//otherwise load the sound and store the id
			int soundId = mSoundPool.load(mContext, soundResourceID, 1);
			soundIds.put(Integer.valueOf(soundResourceID), new Sound(soundId));
			//HACK: we must wait a couple of seconds until the sound loads, since API level 7 doesnt support onloadcomplete
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return soundId;
		}
	}
	
	public void playSound(int resourceId) {
		//this function is intended for short sound effects
		Sound sound = soundIds.get(Integer.valueOf(resourceId));
		mSoundPool.play(sound.soundId, leftVolume, rightVolume, 1, 0, 1f);
	}
	
	public void playLoopedSound(int resourceId) {
		Sound sound = soundIds.get(Integer.valueOf(resourceId));
		//if there's already a stream id, do nothing, otherwise play
		if (sound.streamId == -1)
		{
			//update the sound object with the current stream id (useful for stopping the loop)
			Log.d("SoundSystem", "Play looped sound " + Integer.valueOf(resourceId).toString() + " volume L,R: " + Float.valueOf(leftVolume).toString() + "," + Float.valueOf(rightVolume).toString());
			sound.streamId = mSoundPool.play(sound.soundId, leftVolume, rightVolume, 1, -1, 1f);
			soundIds.put(Integer.valueOf(resourceId), sound);
		}
	}

	public void setMixerVolume(int resourceId, int leftVolumeFactor, int rightVolumeFactor) {
		float streamVolume = (float)mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		float streamMaxVolume = (float)mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		//media volume is a value from 0 to 1.0
		float mediaVolume = streamVolume / streamMaxVolume;
		//HACK: http://stackoverflow.com/questions/1394694/why-is-my-soundpool-mute
		this.leftVolume = ((mediaVolume * leftVolumeFactor) / 100)*0.99f;
		this.rightVolume = ((mediaVolume * rightVolumeFactor) / 100)*0.99f;
		Sound sound = soundIds.get(Integer.valueOf(resourceId));
		if (sound != null)
		{
			if (sound.streamId > -1)
			{
				Log.d("SoundSystem","Setting R,L volume to " + Float.valueOf(this.leftVolume).toString() + " , " + Float.valueOf(this.rightVolume).toString());
				mSoundPool.setVolume(sound.streamId, leftVolume, rightVolume);
				//resume a paused sound
				mSoundPool.resume(sound.streamId);
			}
		}
	}
	
	public void stop(int resourceID)
	{
		Sound sound = soundIds.get(Integer.valueOf(resourceID));
		if (sound != null)
		{
			if (sound.streamId > -1)
			{
				Log.d("SoundSystem", "Stoping sound " + Integer.valueOf(resourceID).toString());
				mSoundPool.stop(sound.streamId);
			}
		}
	}

	public void pause(int resourceID) {
		Sound sound = soundIds.get(Integer.valueOf(resourceID));
		if (sound != null)
		{
			if (sound.streamId > -1)
			{
				Log.d("SoundSystem", "Pausing sound " + Integer.valueOf(resourceID).toString());
				mSoundPool.pause(sound.streamId);
			}
		}
	}
	
}