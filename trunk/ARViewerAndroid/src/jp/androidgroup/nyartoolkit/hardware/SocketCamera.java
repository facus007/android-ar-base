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

package jp.androidgroup.nyartoolkit.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Message;
import android.util.Log;

/**
 * It is implementing of CameraIF for Socket. 
 * 
 * @author noritsuna
 */
public class SocketCamera implements CameraIF {
	
	private static final int SOCKET_TIMEOUT = 5000;

	private String address;
	private int port;
	
	// for Buf size.
	private int width = -1;
	private int height = -1;
	
	private PreviewCallback callback = null;

	private CaptureThread mCaptureThread = null;
	
	public SocketCamera(String address, int port) {
		this.address = address;
		this.port = port;
		this.width = 320;
		this.height = 240;
	}
	public SocketCamera(String address, int port, int width, int height) {
		this.address = address;
		this.port = port;
		this.width = width;
		this.height = height;
	}
	
	@Override
	public Parameters getParameters() {
		return null;
	}


	@Override
	public void onDestroy() {
		this.onStop();
		this.mCaptureThread = null;
	}


	@Override
	public void setParameters(Parameters params) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setPreviewCallback(PreviewCallback callback) {
		this.callback = callback;
	}

	@Override
	public void onStart() {
		Log.d("SocketCamera", "call onStart");
		if(this.mCaptureThread == null) {
			this.mCaptureThread = new CaptureThread();
		}
		if(this.mCaptureThread.mDone) {
			this.mCaptureThread.mDone = false;
			this.mCaptureThread.start();
		}
	}

	@Override
	public void onStop() {
		Log.d("AttachedCamera", "call onStop");
		this.mCaptureThread.mDone = true;
	}		
	
	@Override
	public void onPause() {
		this.onStop();
	}
	@Override
	public void onResume() {
		this.onStart();
	}



	@Override
	public void resetPreviewSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	

	@Override
	public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
		
	}


	

	
	private class CaptureThread extends Thread {
		private boolean mDone = true;

		private int buf_size = width * height * 3 * 3;

		private byte[] buf = new byte[buf_size];
		private byte[] zeroBuf = new byte[buf_size];

		private InetSocketAddress iSock = new InetSocketAddress(address, port);
		
		
		public CaptureThread() {
			super();
			Log.d("CaptureThread", "new");
		}

		@Override
		public void run() {
			Log.d("CaptureThread", "in capture thread");
			//buf_size = width * height * 3 * 3;
			while (!mDone) {
				Log.d("CaptureThread", "in capture");
				if(buf == null) {
					buf = new byte[buf_size];
					zeroBuf = new byte[buf_size];
				} else {
					// Zero clear.
					System.arraycopy(zeroBuf, 0, buf, 0, buf_size);
				}
				
				Socket socket = null;
				try {
					socket = new Socket();
					socket.connect(iSock, SOCKET_TIMEOUT);
					// obtain the bitmap
					InputStream in = socket.getInputStream();
					int offset = 0;
					int back_size = buf_size;
					int res = 0;
					while((res = in.read(buf, offset, back_size)) != -1) {
						offset = offset + res;
						back_size = back_size - res;
						if(offset+1600 > buf_size){
							break;
						}
					}
					
				} catch (Exception e) {
					Log.i("SocketCamera", "Failed to obtain image over network", e);
				} finally {
					try {
						if (socket != null) {
							socket.close();
							socket = null;
						}
					} catch (IOException e) {
						/* ignore */
					}
				}
				callback.onPreviewFrame(buf, null);
				Log.d("SocketCamera", "end SocketCamera.");
			}
		}		
	}

}
