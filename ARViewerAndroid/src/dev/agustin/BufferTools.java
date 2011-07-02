package dev.agustin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import android.graphics.Bitmap;

public class BufferTools {
	/**
	 * Make a direct NIO FloatBuffer from an array of floats
	 * @param arr The array
	 * @return The newly created FloatBuffer
	 */
	protected static FloatBuffer makeFloatBuffer(float[] arr) {
		ByteBuffer bb = ByteBuffer.allocateDirect(arr.length*4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(arr);
		fb.position(0);
		return fb;
	}

	/**
	 * Make a direct NIO IntBuffer from an array of ints
	 * @param arr The array
	 * @return The newly created IntBuffer
	 */
	protected static IntBuffer makeFloatBuffer(int[] arr) {
		ByteBuffer bb = ByteBuffer.allocateDirect(arr.length*4);
		bb.order(ByteOrder.nativeOrder());
		IntBuffer ib = bb.asIntBuffer();
		ib.put(arr);
		ib.position(0);
		return ib;
	}

	protected ByteBuffer makeByteBuffer(Bitmap bmp) {
		ByteBuffer bb = ByteBuffer.allocateDirect(bmp.getHeight()*bmp.getWidth()*4);
		bb.order(ByteOrder.BIG_ENDIAN);
		IntBuffer ib = bb.asIntBuffer();

		for (int y = 0; y < bmp.getHeight(); y++)
			for (int x=0;x<bmp.getWidth();x++) {
				int pix = bmp.getPixel(x, bmp.getHeight()-y-1);
				// Convert ARGB -> RGBA
				byte alpha = (byte)((pix >> 24)&0xFF);
				byte red = (byte)((pix >> 16)&0xFF);
				byte green = (byte)((pix >> 8)&0xFF);
				byte blue = (byte)((pix)&0xFF);

				ib.put(((red&0xFF) << 24) | 
						((green&0xFF) << 16) |
						((blue&0xFF) << 8) |
						((alpha&0xFF)));
			}
		ib.position(0);
		bb.position(0);
		return bb;
	}

}
