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
public class ColorABGR {
	public byte a;
	public byte r;
	public byte g;
	public byte b;

	public ColorABGR() {
		a = r = g = b = (byte)0xff;
	}
	public ColorABGR(int agbr) {
		a = (byte)Color.alpha(agbr);
		r = (byte)Color.red(agbr);
		g = (byte)Color.blue(agbr);
		b = (byte)Color.blue(agbr);
	}
	public ColorABGR(byte a, byte r, byte g, byte b) {
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public void set(byte a, byte r, byte g, byte b) {
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public int toARGB() {
		return Color.argb(a, r, g, b);
	}
	public int toABGR() {
		return Color.argb(a, b, g, r);
	}
}
