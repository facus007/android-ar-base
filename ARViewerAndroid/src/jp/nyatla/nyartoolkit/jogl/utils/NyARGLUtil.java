/* 
 * PROJECT: NyARToolkit JOGL utilities.
 * --------------------------------------------------------------------------------
 * This work is based on the original ARToolKit developed by
 *   Hirokazu Kato
 *   Mark Billinghurst
 *   HITLab, University of Washington, Seattle
 * http://www.hitl.washington.edu/artoolkit/
 *
 * The NyARToolkit is Java edition ARToolKit class library.
 * Copyright (C)2008-2009 Ryo Iizuka
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
 *	http://nyatla.jp/nyatoolkit/
 *	<airmail(at)ebony.plala.or.jp> or <nyatla(at)nyatla.jp>
 * 
 */
package jp.nyatla.nyartoolkit.jogl.utils;

import jp.nyatla.nyartoolkit.NyARException;
import jp.nyatla.nyartoolkit.core.*;
import jp.nyatla.nyartoolkit.core.raster.rgb.*;
import jp.nyatla.nyartoolkit.core.param.NyARParam;
import jp.nyatla.nyartoolkit.core.transmat.NyARTransMatResult;
import jp.nyatla.nyartoolkit.core.types.*;
/**
 * NyARToolkit用のJOGL支援関数群
 */
public class NyARGLUtil
{
	private double view_scale_factor = 0.025; //original value: 0.025
	private double view_distance_min = 0.1;//#define VIEW_DISTANCE_MIN		0.1			// Objects closer to the camera than this will not be displayed.
	private double view_distance_max = 100.0;//#define VIEW_DISTANCE_MAX		100.0		// Objects further away from the camera than this will not be displayed.

	public void setScaleFactor(double i_new_value)
	{
		this.view_scale_factor = i_new_value;
	}

	public void setViewDistanceMin(double i_new_value)
	{
		this.view_distance_min = i_new_value;
	}

	public void setViewDistanceMax(double i_new_value)
	{
		this.view_distance_max = i_new_value;
	}

	/**
	 * void arglCameraFrustumRH(const ARParam *cparam, const double focalmin, const double focalmax, GLdouble m_projection[16])
	 * 関数の置き換え
	 * NyARParamからOpenGLのProjectionを作成します。
	 * @param i_arparam
	 * @param o_gl_projection
	 * double[16]を指定して下さい。
	 */
	public void toCameraFrustumRH(NyARParam i_arparam,double[] o_gl_projection)
	{
		NyARMat trans_mat = new NyARMat(3, 4);
		NyARMat icpara_mat = new NyARMat(3, 4);
		double[][] p = new double[3][3], q = new double[4][4];
		int i, j;

		final NyARIntSize size=i_arparam.getScreenSize();
		final int width = size.w;
		final int height = size.h;
		
		i_arparam.getPerspectiveProjectionMatrix().decompMat(icpara_mat, trans_mat);

		double[][] icpara = icpara_mat.getArray();
		double[][] trans = trans_mat.getArray();
		for (i = 0; i < 4; i++) {
			icpara[1][i] = (height - 1) * (icpara[2][i]) - icpara[1][i];
		}

		for (i = 0; i < 3; i++) {
			for (j = 0; j < 3; j++) {
				p[i][j] = icpara[i][j] / icpara[2][2];
			}
		}
		q[0][0] = (2.0 * p[0][0] / (width - 1));
		q[0][1] = (2.0 * p[0][1] / (width - 1));
		q[0][2] = -((2.0 * p[0][2] / (width - 1)) - 1.0);
		q[0][3] = 0.0;

		q[1][0] = 0.0;
		q[1][1] = -(2.0 * p[1][1] / (height - 1));
		q[1][2] = -((2.0 * p[1][2] / (height - 1)) - 1.0);
		q[1][3] = 0.0;

		q[2][0] = 0.0;
		q[2][1] = 0.0;
		q[2][2] = (view_distance_max + view_distance_min) / (view_distance_min - view_distance_max);
		q[2][3] = 2.0 * view_distance_max * view_distance_min / (view_distance_min - view_distance_max);

		q[3][0] = 0.0;
		q[3][1] = 0.0;
		q[3][2] = -1.0;
		q[3][3] = 0.0;

		for (i = 0; i < 4; i++) { // Row.
			// First 3 columns of the current row.
			for (j = 0; j < 3; j++) { // Column.
				o_gl_projection[i + j * 4] = q[i][0] * trans[0][j] + q[i][1] * trans[1][j] + q[i][2] * trans[2][j];
			}
			// Fourth column of the current row.
			o_gl_projection[i + 3 * 4] = q[i][0] * trans[0][3] + q[i][1] * trans[1][3] + q[i][2] * trans[2][3] + q[i][3];
		}
		return;
	}
	
	/**
	 * void arglCameraFrustumRHf(const ARParam *cparam, const double focalmin, const double focalmax, GLdouble m_projection[16])
	 * 関数の置き換え
	 * NyARParamからOpenGLのProjectionを作成します。
	 * @param i_arparam
	 * @param o_gl_projection
	 * float[16]を指定して下さい。
	 */
	public void toCameraFrustumRHf(NyARParam i_arparam, float[] o_gl_projection) 
	{
		double[] mf = new double[16];
		toCameraFrustumRH(i_arparam, mf);
		
		for (int i = 0; i < mf.length; i++) {
			o_gl_projection[i] = (float) mf[i];
		}
	}
	
	
	
	/**
	 * NyARTransMatResultをOpenGLの行列へ変換します。
	 * @param i_ny_result
	 * @param o_gl_result
	 * @throws NyARException
	 */
	public void toCameraViewRH(NyARTransMatResult i_ny_result, double[] o_gl_result) throws NyARException
	{
		o_gl_result[0 + 0 * 4] = i_ny_result.m00; //0 gl right x
		o_gl_result[0 + 1 * 4] = i_ny_result.m01; //4 gl right y
		o_gl_result[0 + 2 * 4] = i_ny_result.m02; //8 gl right z
		o_gl_result[0 + 3 * 4] = i_ny_result.m03; //12 gl camera center x
		o_gl_result[1 + 0 * 4] = -i_ny_result.m10; //1 gl up x
 		o_gl_result[1 + 1 * 4] = -i_ny_result.m11; //5 gl up y
		o_gl_result[1 + 2 * 4] = -i_ny_result.m12; //9 gl up z
		o_gl_result[1 + 3 * 4] = -i_ny_result.m13; //13 gl camera center y
		o_gl_result[2 + 0 * 4] = -i_ny_result.m20; //2 gl back x
		o_gl_result[2 + 1 * 4] = -i_ny_result.m21; //6 gl back y
		o_gl_result[2 + 2 * 4] = -i_ny_result.m22; //10 gl back z
		o_gl_result[2 + 3 * 4] = -i_ny_result.m23; //14 gl camera center z
		o_gl_result[3 + 0 * 4] = 0.0; //3
		o_gl_result[3 + 1 * 4] = 0.0; //7
		o_gl_result[3 + 2 * 4] = 0.0; //11
		o_gl_result[3 + 3 * 4] = 1.0; //15 gl matrix lower right corner
		if (view_scale_factor != 0.0) {
			o_gl_result[12] *= view_scale_factor; //camera center x
			o_gl_result[13] *= view_scale_factor; //camera center y
			o_gl_result[14] *= view_scale_factor; //camera center z
		}
		return;
	}	
	/**
	 * NyARTransMatResultをOpenGLの行列へ変換します。
	 * @param i_ny_result
	 * @param o_gl_result
	 * @throws NyARException
	 */
	public void toCameraViewRHf(NyARTransMatResult i_ny_result, float[] o_gl_result) throws NyARException
	{
		double[] mf = new double[16];
		toCameraViewRH(i_ny_result, mf);
		
		for (int i = 0; i < mf.length; i++) {
			o_gl_result[i] = (float) mf[i];
		}
	}
	
}
