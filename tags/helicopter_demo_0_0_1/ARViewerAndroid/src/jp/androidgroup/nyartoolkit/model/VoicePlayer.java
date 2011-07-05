/* 
 * PROJECT: NyARToolkit for Android SDK
 * --------------------------------------------------------------------------------
 * This work is based on the original ARToolKit developed by
 *   Hirokazu Kato
 *   Mark Billinghurst
 *   HITLab, University of Washington, Seattle
 * http://www.hitl.washington.edu/artoolkit/
 *
 * NyARToolkit for Android SDK
 *   Copyright (C)2010 NyARToolkit for Android team
 *   Copyright (C)2010 R.Iizuka(nyatla)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For further information please contact.
 *  http://sourceforge.jp/projects/nyartoolkit-and/
 *  
 * This work is based on the NyARToolKit developed by
 *  R.Iizuka (nyatla)
 *    http://nyatla.jp/nyatoolkit/
 * 
 * contributor(s)
 *  noritsuna
 */

package jp.androidgroup.nyartoolkit.model;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * 
 * 
 * @author noritsuna
 */
public class VoicePlayer {
	
	MediaPlayer mVoiceSound;
	
	public void initVoice(AssetFileDescriptor afd) {
		try {
			mVoiceSound = new MediaPlayer();
			
			mVoiceSound.setDataSource(afd.getFileDescriptor(),
									  afd.getStartOffset(),
									  afd.getLength());
			mVoiceSound.setLooping(true);

			if (mVoiceSound != null) {
				mVoiceSound.setAudioStreamType(AudioManager.STREAM_SYSTEM);
				mVoiceSound.prepare();
			}
			mVoiceSound.seekTo(0);
			mVoiceSound.start();
			mVoiceSound.pause();
		} catch (Exception ex) {
			Log.w("VoicePlayer", "Couldn't create click sound", ex);
		}
	}

	public void startVoice() {
		if (!mVoiceSound.isPlaying()) {
			mVoiceSound.start();
		}
	}

	public void stopVoice() {
		if (mVoiceSound.isPlaying()) {
			mVoiceSound.pause();
		}
	}

}
