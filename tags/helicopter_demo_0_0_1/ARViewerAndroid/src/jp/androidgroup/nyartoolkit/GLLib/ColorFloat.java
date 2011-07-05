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
 *  
 */

package jp.androidgroup.nyartoolkit.GLLib;

import android.graphics.Color;

/**
 * 
 */
public class ColorFloat {
	public float a;
	public float r;
	public float g;
	public float b;

	public ColorFloat() {
		a = r = g = b = 1.0f;
	}
	public ColorFloat(int argb) {
		a = ((float)((argb & 0xff000000) >> 24) / 255.0f);
		r = ((float)((argb & 0x00ff0000) >> 16) / 255.0f);
		g = ((float)((argb & 0x0000ff00) >> 8) / 255.0f);
		b = ((float)( argb & 0x000000ff) / 255.0f);
	}
	public ColorFloat(float r, float g, float b, float a) {
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public int toARGB() {
		return Color.argb((int)(255*a), (int)(255*r), 
						  (int)(255*g), (int)(255*b));	
	}
	public int toABGR() {
		return Color.argb((int)(255*a), (int)(255*b), 
						  (int)(255*g), (int)(255*r));
	}
}
