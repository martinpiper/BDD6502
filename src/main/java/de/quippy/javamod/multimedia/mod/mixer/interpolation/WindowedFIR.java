/*
 * @(#) WindowedFIR.java
 * 
 * Created on 15.06.2006 by Daniel Becker
 * 
 *-----------------------------------------------------------------------
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */
package de.quippy.javamod.multimedia.mod.mixer.interpolation;

import de.quippy.javamod.system.Helpers;

/**
 * @author Daniel Becker
 * @since 15.06.2006
 * This code is adopted from the great Mod Plug Tracker by Olivier Lapicque <olivierl@jps.net>
 * ------------------------------------------------------------------------------------------------
 *   fir interpolation doc,
 *	(derived from "an engineer's guide to fir digital filters", n.j. loy)
 *
 *	calculate coefficients for ideal lowpass filter (with cutoff = fc in 0..1 (mapped to 0..nyquist))
 *	  c[-N..N] = (i==0) ? fc : sin(fc*pi*i)/(pi*i)
 *
 *	then apply selected window to coefficients
 *	  c[-N..N] *= w(0..N)
 *	with n in 2*N and w(n) being a window function (see loy)
 *
 *	then calculate gain and scale filter coefs to have unity gain.
 * ------------------------------------------------------------------------------------------------
 */
public class WindowedFIR
{
	// quantizer scale of window coefs
	private static final int	WFIR_QUANTBITS		=	15;
	private static final int	WFIR_QUANTSCALE		=	1<<WFIR_QUANTBITS;
	public  static final int	WFIR_16BITSHIFT		=	(WFIR_QUANTBITS)-1;
	// log2(number)-1 of precalculated taps range is [4..12]
	private static final int	WFIR_FRACBITS		=	10;
	private static final int	WFIR_LUTLEN			=	(1<<(WFIR_FRACBITS+1))+1;
	// number of samples in window
	private static final int	WFIR_LOG2WIDTH		=	3;
	private static final int	WFIR_WIDTH			=	1<<WFIR_LOG2WIDTH;
	// cutoff (1.0 == pi/2)
	private static final float	WFIR_CUTOFF			=	0.90f;
	// wfir types plus default:
	private static final int	WFIR_HANN			=	0;
	private static final int	WFIR_HAMMING		=	1;
	private static final int	WFIR_BLACKMANEXACT	=	2;
	private static final int	WFIR_BLACKMAN3T61	=	3;
	private static final int	WFIR_BLACKMAN3T67	=	4;
	private static final int	WFIR_BLACKMAN4T92	=	5;
	private static final int	WFIR_BLACKMAN4T74	=	6;
	private static final int	WFIR_KAISER4T		=	7;
	private static final int	WFIR_TYPE			=	WFIR_BLACKMANEXACT;	

	private static final double	M_zEPS				=	1e-8;

	public  static final int	WFIR_POSFRACMASK	=	(1<<Helpers.SHIFT)-1;
	// shifting of calculated samples:
	public  static final int	WFIR_FRACSHIFT		=	Helpers.SHIFT - (WFIR_FRACBITS + 1 + WFIR_LOG2WIDTH);
	public  static final int	WFIR_FRACMASK		=	(((1<<((Helpers.SHIFT + 1) - WFIR_FRACSHIFT)) - 1) & ~ ((1<<WFIR_LOG2WIDTH)-1));
	public  static final int	WFIR_FRACHALVE		=	1<<(Helpers.SHIFT-(WFIR_FRACBITS+2));
	
	public static final int [] lut = new int [WFIR_LUTLEN*WFIR_WIDTH];
	
	static
	{
		initialize();
	}

	/**
	 * Constructor for WindowedFIR
	 * is not needed!
	 */
	private WindowedFIR()
	{
		super();
	}
	/**
	 * Get a coeff.
	 * @since 15.06.2006
	 * @param _PCnr
	 * @param _POfs
	 * @param _PCut
	 * @param _PWidth
	 * @param _PType
	 * @return
	 */
	private static double coef(int _PCnr, double _POfs, double _PCut, int _PWidth, int _PType) // float _PPos, float _PFc, int _PLen )
	{
		double _LWidthM1 = _PWidth - 1;
		double _LWidthM1Half = 0.5d * _LWidthM1;
		double _LPosU = ((double) _PCnr) - _POfs;
		double _LPos = _LPosU - _LWidthM1Half;
		double _LPIdl = 2.0d * Math.PI / _LWidthM1;
		double _LWc, _LSi;
		if (Math.abs(_LPos) < M_zEPS)
		{
			_LWc = 1.0;
			_LSi = _PCut;
		}
		else
		{
			switch (_PType)
			{
				case WFIR_HANN:
					_LWc = 0.50 - 0.50 * Math.cos(_LPIdl * _LPosU);
					break;
				case WFIR_HAMMING:
					_LWc = 0.54 - 0.46 * Math.cos(_LPIdl * _LPosU);
					break;
				case WFIR_BLACKMANEXACT:
					_LWc = 0.42 - 0.50 * Math.cos(_LPIdl * _LPosU) + 0.08 * Math.cos(2.0 * _LPIdl * _LPosU);
					break;
				case WFIR_BLACKMAN3T61:
					_LWc = 0.44959 - 0.49364 * Math.cos(_LPIdl * _LPosU) + 0.05677 * Math.cos(2.0 * _LPIdl * _LPosU);
					break;
				case WFIR_BLACKMAN3T67:
					_LWc = 0.42323 - 0.49755 * Math.cos(_LPIdl * _LPosU) + 0.07922 * Math.cos(2.0 * _LPIdl * _LPosU);
					break;
				case WFIR_BLACKMAN4T92:
					_LWc = 0.35875 - 0.48829 * Math.cos(_LPIdl * _LPosU) + 0.14128 * Math.cos(2.0 * _LPIdl * _LPosU) - 0.01168 * Math.cos(3.0 * _LPIdl * _LPosU);
					break;
				case WFIR_BLACKMAN4T74:
					_LWc = 0.40217 - 0.49703 * Math.cos(_LPIdl * _LPosU) + 0.09392 * Math.cos(2.0 * _LPIdl * _LPosU) - 0.00183 * Math.cos(3.0 * _LPIdl * _LPosU);
					break;
				case WFIR_KAISER4T:
					_LWc = 0.40243 - 0.49804 * Math.cos(_LPIdl * _LPosU) + 0.09831 * Math.cos(2.0 * _LPIdl * _LPosU) - 0.00122 * Math.cos(3.0 * _LPIdl * _LPosU);
					break;
				default:
					_LWc = 1.0;
					break;
			}
			_LPos *= Math.PI;
			_LSi = Math.sin(_PCut * _LPos) / _LPos;
		}
		return _LWc * _LSi;
	}
	/**
	 * Init the static params
	 * @since 15.06.2006
	 */
	private static void initialize()
	{
		double _LPcllen	= (double)(1L<<WFIR_FRACBITS);	// number of precalculated lines for 0..1 (-1..0)
		double _LNorm	= 1.0f / (double)(2.0d * _LPcllen);
		double _LCut	= WFIR_CUTOFF;
		double _LScale	= (double)WFIR_QUANTSCALE;
		for (int _LPcl=0; _LPcl<WFIR_LUTLEN; _LPcl++)
		{	
			double [] _LCoefs	= new double [WFIR_WIDTH];
			double _LOfs		= ((double)_LPcl-_LPcllen)*_LNorm;
			int _LIdx			= _LPcl<<WFIR_LOG2WIDTH;
			
			double _LGain = 0.0d;
			for (int _LCc=0; _LCc<WFIR_WIDTH; _LCc++)
				_LGain	+= (_LCoefs[_LCc] = coef(_LCc, _LOfs, _LCut, WFIR_WIDTH, WFIR_TYPE));
			
			_LGain = 1.0f / _LGain;
			for (int _LCc=0; _LCc<WFIR_WIDTH; _LCc++)
			{	
				double _LCoef = Math.floor( 0.5 + _LScale*_LCoefs[_LCc]*_LGain );
				lut[_LIdx+_LCc] = (int)( (_LCoef<-_LScale)?-_LScale:((_LCoef>_LScale)?_LScale:_LCoef) );
			}
		}
	}
}
