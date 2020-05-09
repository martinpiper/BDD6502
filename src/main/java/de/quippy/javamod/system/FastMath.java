/*
 * @(#) FastMath.java
 * Created on 29.01.2012 by Daniel Becker
 * -----------------------------------------------------------------------
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * ----------------------------------------------------------------------
 */
package de.quippy.javamod.system;

/**
 * This class provides fast mathematical calculations which are good
 * enough for some audio calculations.
 * @author Daniel Becker
 * @since 29.01.2012
 */
public class FastMath
{
	private static final double B = 1.2732395447351628D;
	private static final double C = -0.4052847345693511D;
	private static final double P = 0.218D;
	private static final double hPI = Math.PI / 2D;
	private static final double PI2 = Math.PI * 2D;
	private static final double sPI = Math.PI + hPI;
	private static final double atan2_coeff_1 = 0.78539816339744828D;
	private static final double atan2_coeff_2 = 2.3561944901923448D;

	public FastMath()
	{
	}

	public static double wrap(double angle)
	{
		angle %= PI2;
		if (angle > 3.1415926535897931D)
			angle -= PI2;
		else if (angle < -3.1415926535897931D) angle += PI2;
		if (angle < -3.1415926535897931D || angle > 3.1415926535897931D)
			throw new IllegalArgumentException("Wrong angel : " + angle);
		else
			return angle;
	}

	public static double fastSin(double theta)
	{
		return fastSin0(wrap(theta));
	}

	public static double fastCos(double theta)
	{
		return fastSin0(wrap(theta + hPI));
	}

	public static double fastSin0(double theta)
	{
		double y = B * theta + C * theta * Math.abs(theta);
		y = P * (y * Math.abs(y) - y) + y;
		return y;
	}

	public static double fastCos0(double theta)
	{
		if (theta > hPI)
			theta -= sPI;
		else
			theta += hPI;
		return fastSin0(theta);
	}

	public static double exp(double val)
	{
		long tmp = (long) (1512775D * val + 1072632447D);
		return Double.longBitsToDouble(tmp << 32);
	}

	public static double log(double x)
	{
		return (6D * (x - 1.0D)) / (x + 1.0D + 4D * Math.sqrt(x));
	}

	public static double pow(double a, double b)
	{
		int x = (int) (Double.doubleToLongBits(a) >> 32);
		int y = (int) (b * (double) (x - 1072632447) + 1072632447D);
		return Double.longBitsToDouble((long) y << 32);
	}

	public static float floor(float value)
	{
		if (value < 0.0F)
			throw new IllegalArgumentException("Wrong value : " + value);
		else
			return (float) (int) value;
	}

	public static double floor(double value)
	{
		if (value < 0.0D)
			throw new IllegalArgumentException("Wrong value : " + value);
		else
			return (double) (long) value;
	}

	public static double atan2(double y, double x)
	{
        if (y == 0.0D) return 0.0D;
        if (x == 0.0D) return (y>0.0D)?hPI:-hPI;

		double abs_y = Math.abs(y);
		double angle;
		if (x > 0.0D)
		{
			double r = (x - abs_y) / (x + abs_y);
			angle = atan2_coeff_1 - atan2_coeff_1 * r;
		}
		else
		{
			double r = (x + abs_y) / (abs_y - x);
			angle = atan2_coeff_2 - atan2_coeff_1 * r;
		}
		return y >= 0.0D ? angle : -angle;
	}

	public static double fastSqrt(double a)
	{
		long x = Double.doubleToLongBits(a) >> 32;
		double y = Double.longBitsToDouble(x + 1072632448L << 31);
		return y;
	}

	public static double sqrt(double a)
	{
		long x = Double.doubleToLongBits(a) >> 32;
		double y = Double.longBitsToDouble(x + 1072632448L << 31);
		y = (y + a / y) * 0.5D;
		return y;
	}
}
