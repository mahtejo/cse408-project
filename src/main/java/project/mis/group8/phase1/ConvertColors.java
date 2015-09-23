/**
 * @author kvivekanandan
 * Sep 20, 2015
 * ConvertColors.java
 * modified from http://rsb.info.nih.gov/ij/plugins/download/Color_Space_Converter.java
 */

package project.mis.group8.phase1;

import java.awt.Color;

public class ConvertColors {
	/**
	 * reference white in XYZ coordinates
	 */

	public double[] D65 = { 95.0429, 100.0, 108.8900 };
	public double[] whitePoint = D65;

	/**
	 * sRGB to XYZ conversion matrix
	 */
	public double[][] M = { { 0.4124, 0.3576, 0.1805 }, { 0.2126, 0.7152, 0.0722 }, { 0.0193, 0.1192, 0.9505 } };

	/**
	 * XYZ to sRGB conversion matrix
	 */
	public double[][] Mi = { { 3.2406, -1.5372, -0.4986 }, { -0.9689, 1.8758, 0.0415 }, { 0.0557, -0.2040, 1.0570 } };

	/**
	 * default constructor, uses D65 for the white point
	 */
	public ConvertColors() {
		whitePoint = D65;
	}

	/**
	 * @param H
	 *            Hue angle/360 (0..1)
	 * @param S
	 *            Saturation (0..1)
	 * @param B
	 *            Value (0..1)
	 * @return RGB values
	 */
	public int[] HSBtoRGB(double H, double S, double B) {
		int[] result = new int[3];
		int rgb = Color.HSBtoRGB((float) H, (float) S, (float) B);
		result[0] = (rgb >> 16) & 0xff;
		result[1] = (rgb >> 8) & 0xff;
		result[2] = (rgb >> 0) & 0xff;
		return result;
	}

	public int[] HSBtoRGB(double[] HSB) {
		return HSBtoRGB(HSB[0], HSB[1], HSB[2]);
	}

	/**
	 * Convert LAB to RGB.
	 * 
	 * @param L
	 * @param a
	 * @param b
	 * @return RGB values
	 */
	public int[] LABtoRGB(double L, double a, double b) {
		return XYZtoRGB(LABtoXYZ(L, a, b));
	}

	/**
	 * @param Lab
	 * @return RGB values
	 */
	public int[] LABtoRGB(double[] Lab) {
		return XYZtoRGB(LABtoXYZ(Lab));
	}

	/**
	 * Convert LAB to XYZ.
	 * 
	 * @param L
	 * @param a
	 * @param b
	 * @return XYZ values
	 */
	public double[] LABtoXYZ(double L, double a, double b) {
		double[] result = new double[3];

		double y = (L + 16.0) / 116.0;
		double y3 = Math.pow(y, 3.0);
		double x = (a / 500.0) + y;
		double x3 = Math.pow(x, 3.0);
		double z = y - (b / 200.0);
		double z3 = Math.pow(z, 3.0);

		if (y3 > 0.008856) {
			y = y3;
		} else {
			y = (y - (16.0 / 116.0)) / 7.787;
		}
		if (x3 > 0.008856) {
			x = x3;
		} else {
			x = (x - (16.0 / 116.0)) / 7.787;
		}
		if (z3 > 0.008856) {
			z = z3;
		} else {
			z = (z - (16.0 / 116.0)) / 7.787;
		}

		result[0] = x * whitePoint[0];
		result[1] = y * whitePoint[1];
		result[2] = z * whitePoint[2];

		return result;
	}

	/**
	 * Convert LAB to XYZ.
	 * 
	 * @param Lab
	 * @return XYZ values
	 */
	public double[] LABtoXYZ(double[] Lab) {
		return LABtoXYZ(Lab[0], Lab[1], Lab[2]);
	}

	/**
	 * @param R
	 *            Red in range 0..255
	 * @param G
	 *            Green in range 0..255
	 * @param B
	 *            Blue in range 0..255
	 * @return HSB values: H is 0..360 degrees / 360 (0..1), S is 0..1, B is
	 *         0..1
	 */
	public double[] RGBtoHSB(int R, int G, int B) {
		double[] result = new double[3];
		float[] hsb = new float[3];
		Color.RGBtoHSB(R, G, B, hsb);
		result[0] = hsb[0];
		result[1] = hsb[1];
		result[2] = hsb[2];
		return result;
	}

	public double[] RGBtoHSB(int[] RGB) {
		return RGBtoHSB(RGB[0], RGB[1], RGB[2]);
	}

	/**
	 * @param R
	 * @param G
	 * @param B
	 * @return Lab values
	 */
	public double[] RGBtoLAB(int R, int G, int B) {
		return XYZtoLAB(RGBtoXYZ(R, G, B));
	}

	/**
	 * @param RGB
	 * @return Lab values
	 */
	public double[] RGBtoLAB(int[] RGB) {
		return XYZtoLAB(RGBtoXYZ(RGB));
	}

	/**
	 * Convert RGB to XYZ
	 * 
	 * @param R
	 * @param G
	 * @param B
	 * @return XYZ in double array.
	 */
	public double[] RGBtoXYZ(int R, int G, int B) {
		double[] result = new double[3];

		// convert 0..255 into 0..1
		double r = R / 255.0;
		double g = G / 255.0;
		double b = B / 255.0;

		// assume sRGB
		if (r <= 0.04045) {
			r = r / 12.92;
		} else {
			r = Math.pow(((r + 0.055) / 1.055), 2.4);
		}
		if (g <= 0.04045) {
			g = g / 12.92;
		} else {
			g = Math.pow(((g + 0.055) / 1.055), 2.4);
		}
		if (b <= 0.04045) {
			b = b / 12.92;
		} else {
			b = Math.pow(((b + 0.055) / 1.055), 2.4);
		}

		r *= 100.0;
		g *= 100.0;
		b *= 100.0;

		// [X Y Z] = [r g b][M]
		result[0] = (r * M[0][0]) + (g * M[0][1]) + (b * M[0][2]);
		result[1] = (r * M[1][0]) + (g * M[1][1]) + (b * M[1][2]);
		result[2] = (r * M[2][0]) + (g * M[2][1]) + (b * M[2][2]);

		return result;
	}

	/**
	 * Convert RGB to XYZ
	 * 
	 * @param RGB
	 * @return XYZ in double array.
	 */
	public double[] RGBtoXYZ(int[] RGB) {
		return RGBtoXYZ(RGB[0], RGB[1], RGB[2]);
	}

	/**
	 * @param x
	 * @param y
	 * @param Y
	 * @return XYZ values
	 */
	public double[] xyYtoXYZ(double x, double y, double Y) {
		double[] result = new double[3];
		if (y == 0) {
			result[0] = 0;
			result[1] = 0;
			result[2] = 0;
		} else {
			result[0] = (x * Y) / y;
			result[1] = Y;
			result[2] = ((1 - x - y) * Y) / y;
		}
		return result;
	}

	/**
	 * @param xyY
	 * @return XYZ values
	 */
	public double[] xyYtoXYZ(double[] xyY) {
		return xyYtoXYZ(xyY[0], xyY[1], xyY[2]);
	}

	/**
	 * Convert XYZ to LAB.
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return Lab values
	 */
	public double[] XYZtoLAB(double X, double Y, double Z) {

		double x = X / whitePoint[0];
		double y = Y / whitePoint[1];
		double z = Z / whitePoint[2];

		if (x > 0.008856) {
			x = Math.pow(x, 1.0 / 3.0);
		} else {
			x = (7.787 * x) + (16.0 / 116.0);
		}
		if (y > 0.008856) {
			y = Math.pow(y, 1.0 / 3.0);
		} else {
			y = (7.787 * y) + (16.0 / 116.0);
		}
		if (z > 0.008856) {
			z = Math.pow(z, 1.0 / 3.0);
		} else {
			z = (7.787 * z) + (16.0 / 116.0);
		}

		double[] result = new double[3];

		result[0] = (116.0 * y) - 16.0;
		result[1] = 500.0 * (x - y);
		result[2] = 200.0 * (y - z);

		return result;
	}

	/**
	 * Convert XYZ to LAB.
	 * 
	 * @param XYZ
	 * @return Lab values
	 */
	public double[] XYZtoLAB(double[] XYZ) {
		return XYZtoLAB(XYZ[0], XYZ[1], XYZ[2]);
	}

	/**
	 * Convert XYZ to RGB.
	 * 
	 * @param X
	 * @param Y
	 * @param Z
	 * @return RGB in int array.
	 */
	public int[] XYZtoRGB(double X, double Y, double Z) {
		int[] result = new int[3];

		double x = X / 100.0;
		double y = Y / 100.0;
		double z = Z / 100.0;

		// [r g b] = [X Y Z][Mi]
		double r = (x * Mi[0][0]) + (y * Mi[0][1]) + (z * Mi[0][2]);
		double g = (x * Mi[1][0]) + (y * Mi[1][1]) + (z * Mi[1][2]);
		double b = (x * Mi[2][0]) + (y * Mi[2][1]) + (z * Mi[2][2]);

		// assume sRGB
		if (r > 0.0031308) {
			r = ((1.055 * Math.pow(r, 1.0 / 2.4)) - 0.055);
		} else {
			r = (r * 12.92);
		}
		if (g > 0.0031308) {
			g = ((1.055 * Math.pow(g, 1.0 / 2.4)) - 0.055);
		} else {
			g = (g * 12.92);
		}
		if (b > 0.0031308) {
			b = ((1.055 * Math.pow(b, 1.0 / 2.4)) - 0.055);
		} else {
			b = (b * 12.92);
		}

		r = (r < 0) ? 0 : r;
		g = (g < 0) ? 0 : g;
		b = (b < 0) ? 0 : b;

		// convert 0..1 into 0..255
		result[0] = (int) Math.round(r * 255);
		result[1] = (int) Math.round(g * 255);
		result[2] = (int) Math.round(b * 255);

		return result;
	}

	/**
	 * Convert XYZ to RGB
	 * 
	 * @param XYZ
	 *            in a double array.
	 * @return RGB in int array.
	 */
	public int[] XYZtoRGB(double[] XYZ) {
		return XYZtoRGB(XYZ[0], XYZ[1], XYZ[2]);
	}

	/**
	 * Convert HSL values to an ARGB Color.
	 *
	 * @param h
	 *            Hue is specified as degrees in the range 0 - 1.
	 * @param s
	 *            Saturation is specified as a percentage in the range 0 - 1.
	 * @param l
	 *            Luminance is specified as a percentage in the range 0 - 1.
	 * @param alpha
	 *            the alpha value between 0 - 1
	 * @return the ARGB value of this color
	 */
	public int[] HLStoRGB(double h, double l, double s) {
		Double r = 0.0, g = 0.0, b = 0.0;
		Double r1 = 0.0, g1 = 0.0, b1 = 0.0;
		if (s == 0) {
			r = g = b = l; // achromatic
		} else {
			if (h > 1) {
				h = (h / 360.0);
			}
			if (s > 1) {
				s = (s / 100.0);
			}
			if (l > 1) {
				l = (l / 100.0);
			}
			double q = (l <= 0.5 ? l * (1.0 + s) : l + s - l * s);
			double p = 2.0 * l - q;
			r1 = hue2rgb(p, q, h + 1.0 / 3.0);
			g1 = hue2rgb(p, q, h);
			Double one_third = 1.0 / 3.0;
			b1 = hue2rgb(p, q, h - one_third);
			r = r1 * 255;
			g = 255 * g1;
			b = 255 * b1;
		}
		return new int[] { (int) Math.round(r1 * 255), (int) Math.round(g1 * 255), (int) Math.round(b1 * 255) };
	}

	public double hue2rgb(double p, double q, double t) {
		if (t < 0)
			t += 1;
		if (t > 1)
			t -= 1;
		if (t < 1.0 / 6.0)
			return p + (q - p) * 6.0 * t;
		if (t < 1.0 / 2.0)
			return q;
		if (t < 2.0 / 3.0)
			return p + (q - p) * ((2.0 / 3.0) - t) * 6.0;

		return p;

	}

	public int[] YUVtoRGB(double y, double u, double v) {
		int[] result = new int[3];

		int r = (int) ((y + 1.403 * v) * 256);
		int g = (int) ((y - 0.344 * u - 0.714 * v) * 256);
		int b = (int) ((y + 1.770 * u) * 256);

		result[0] = r;
		result[1] = g;
		result[2] = b;
		return result;
	}

	public int[] YCbCrtoRGB(double y, double cb, double cr) {
		int[] result = new int[3];

		Double r = y + 1.402 * (cr - 128);
		Double g = y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128);
		Double b = y + 1.772 * (cb - 128);
		result[0] = r.intValue();
		result[1] = g.intValue();
		result[2] = b.intValue();
		return result;
	}

	public int[] YIQtoRGB(double y, double i, double q) {
		int[] result = new int[3];
		Double r = (y + 0.948262 * i + 0.624013 * q);
		Double g = (y - 0.276066 * i - 0.639810 * q);
		Double b = (y - 1.105450 * i + 1.729860 * q);
		if (r < 0.0)
			r = 0.0;
		if (g < 0.0)
			g = 0.0;
		if (b < 0.0)
			b = 0.0;
		if (r > 1.0)
			r = 1.0;
		if (g > 1.0)
			g = 1.0;
		if (b > 1.0)
			b = 1.0;

		r = r * 256;
		g = g * 256;
		b = b * 256;
		result[0] = r.intValue();
		result[1] = g.intValue();
		result[2] = b.intValue();
		return result;
	}

	public static void main(String args[]) {
		ConvertColors c = new ConvertColors();

		int[] rgb = c.HLStoRGB(60, 0.42156862774316004, 0.1627906982546693);
		System.out.println("hls to rgb: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

		rgb = c.HLStoRGB(129, 0.67, 0.75);
		System.out.println("hls to rgb 108,234,127: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

		rgb = c.YUVtoRGB(0.475, -0.059, 0.014);
		System.out.println("yuv to rgb: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

		rgb = c.YIQtoRGB(0.475, 0.044, -0.043);
		System.out.println("YIQ to rgb: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

		rgb = c.YCbCrtoRGB(120, 113, 130);
		System.out.println("YCbCr to rgb: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

		rgb = c.LABtoRGB(51.571, -6.068, 19.147);
		System.out.println("lab to rgb: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

		rgb = c.XYZtoRGB(17.636525984294632, 19.765390475189697, 12.55841722902992);
		System.out.println("xyz to rgb: " + rgb[0] + "," + rgb[1] + "," + rgb[2]);

	}
}
