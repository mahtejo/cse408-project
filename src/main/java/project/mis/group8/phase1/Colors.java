package project.mis.group8.phase1;

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_highgui.cvShowImage;
import static org.bytedeco.javacpp.opencv_highgui.cvWaitKey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;

/**
 * @author kvivekanandan Sep 11, 2015 Test.java
 */

public class Colors {

	public static int NUMBER_OF_BITS = 10;
	public static ColorMap COLOR_MAP;
	public static ColorInstance one;
	public static ColorInstance two;
	public static ColorInstance three;
	public static COLOR_MODEL colorModel;

	enum COLOR_MODEL {
		RGB(8, 8, 8), XYZ(8, 8, 8), YUV(8, 4, 4), YCbCr(8, 2, 4), YIQ(8, 4, 2), HSL(1, 1, 1);
		COLOR_MODEL(int xbits, int ybits, int zbits) {
			this.x_bits = xbits;
			this.y_bits = ybits;
			this.z_bits = zbits;
		}

		private int x_bits;
		private int y_bits;
		private int z_bits;

		public int getXbits() {
			return x_bits;
		}

		public int getYbits() {
			return y_bits;
		}

		public int getZbits() {
			return z_bits;
		}
	}

	/* sample input */
	/*
	 * input format cmd args: colormodel b x1,y1,z1 x2,y2,z2 x3,y3,z3
	 * 
	 * rgb 9 134,34,123 180,60,155 230,90,220
	 * lab 9 33.2268,51.7395,-27.8162 46.0445,58.6107,-26.7900 60.1636,66.2940,-30.4777
	 * xyz 9 13.9789,7.6424,19.4772 26.3546,15.3015,32.5748 46.7146,28.3054,57.6449
	 * yuv 9 0.265,0.107,0.228 0.418,0.094,0.252 0.566,0.108,0.295
	 * hls 9 306.6,0.308,0.74626 312.5,0.47,0.50 313,0.628,0.737
	 * yiq 9 306,0.707,0.308 0.418,0.161,0.216 0.566,0.188,0.251
	 * ycbcr 9 74,155,170 108,152,174 140,156,182
	 * 
	 * */

	public static void main(String args[]) {
		Colors t = new Colors();
		COLOR_MAP = new ColorMap();

		if (args != null && args.length == 5) {
			convertToRGB(args, t);
		} else {
			System.out.println("Incorrect input parameters, continuing with default values:");
			colorModel = COLOR_MODEL.RGB;
			NUMBER_OF_BITS = 10;
			one = t.new ColorInstance(134, 34, 123);
			two = t.new ColorInstance(180, 60, 155);
			three = t.new ColorInstance(230, 90, 200);
		}
		printColorInstances();
		printColorModel();
		printNumberOfBits();

		/* will generate color map and color scale */
		colorMap(COLOR_MODEL.RGB, one, two, three, NUMBER_OF_BITS);

	}

	static HashMap<Double, Double> colorScale(double one, double two, int min, int max, HashMap<Double, Double> channel) {
		double cnx_range = (two - one);
		for (double i = one; i <= two; i += 1) {
			double num = (i - one);
			double x_std = num / cnx_range;
			double x_scaled = (x_std * (max - min)) + min;
			channel.put(i, x_scaled);
		}
		return channel;
	}

	static double colorFromScale(double x_scaled, double one, double two, int min, int max) {
		double cnx_range = (two - one);
		double x_std = (x_scaled - min) / (max - min);
		double num = cnx_range * x_std;
		double i = num + one;
		return i;
	}

	static HashMap<Integer, Double> calculateChannelBucket(double c, double l, int buckets, HashMap<Double, Double> channel, HashMap<Integer, Double> channelBucket) {
		Double i = new Double(c);
		int counter = 0;
		double value = 0;
		Integer prev_bucket = 0;
		Integer current_bucket = 0;
		while (i <= l) {
			int b = buckets;
			current_bucket = (counter / b);
			if (prev_bucket != current_bucket) {
				channelBucket.put(prev_bucket, (double) Math.round((value / b)));
				prev_bucket = current_bucket;
				value = 0;
				continue;
			} else {
				value = value + channel.get(i);
			}
			counter++;
			i++;
		}
		return channelBucket;
	}

	static void colorMap(COLOR_MODEL colorModel, ColorInstance one, ColorInstance two, ColorInstance three, int number_bits) {
		switch (colorModel) {
		case RGB: {
			splitBitsByColorChannel(colorModel);
			/*
			 * xChannel - color scale between [-1,1] xChannelBucket - color bins
			 * in color map
			 */
			HashMap<Double, Double> xChannel = new LinkedHashMap<Double, Double>();
			HashMap<Integer, Double> xChannelBucket = new HashMap<Integer, Double>();
			// System.out.println("X Channel: ");
			colorScale(one.x, two.x, -1, 0, xChannel);
			colorScale(two.x, three.x, 0, 1, xChannel);
			// printMap(xChannel);
			int x_range = three.x - one.x;
			COLOR_MAP.x_buckets = x_range / (int) Math.pow(2, COLOR_MAP.x_bits);
			calculateChannelBucket(one.x, three.x, COLOR_MAP.x_buckets, xChannel, xChannelBucket);
			// printMap(xChannelBucket);

			HashMap<Double, Double> yChannel = new HashMap<Double, Double>();
			HashMap<Integer, Double> yChannelBucket = new HashMap<Integer, Double>();
			// System.out.println("Y Channel: ");
			yChannel = colorScale(one.y, two.y, -1, 0, yChannel);
			yChannel = colorScale(two.y, three.y, 0, 1, yChannel);
			// printMap(yChannel);
			int y_range = three.y - one.y;
			COLOR_MAP.y_buckets = y_range / (int) Math.pow(2, COLOR_MAP.y_bits);
			calculateChannelBucket(one.y, three.y, COLOR_MAP.y_buckets, yChannel, yChannelBucket);
			// System.out.println("Y Color Channel: ");
			// printMap(yChannelBucket);

			HashMap<Double, Double> zChannel = new HashMap<Double, Double>();
			HashMap<Integer, Double> zChannelBucket = new HashMap<Integer, Double>();
			// System.out.println("Z Channel: ");
			zChannel = colorScale(one.z, two.z, -1, 0, zChannel);
			zChannel = colorScale(two.z, three.z, 0, 1, zChannel);
			// printMap(zChannel);
			int z_range = three.z - one.z;
			COLOR_MAP.z_buckets = z_range / (int) Math.pow(2, COLOR_MAP.z_bits);
			calculateChannelBucket(one.z, three.z, COLOR_MAP.z_buckets, zChannel, zChannelBucket);
			// System.out.println("Z Color Channel: ");
			// printMap(zChannelBucket);

			generateColorSets(xChannelBucket, yChannelBucket, zChannelBucket);
			generateAndVisualizeColorScale();

		}
		default:
			break;
		}

	}

	static void generateAndVisualizeColorScale() {
		ArrayList<Double> value_a = new ArrayList<Double>();
		ArrayList<Integer> color_x = new ArrayList<Integer>();
		ArrayList<Integer> color_y = new ArrayList<Integer>();
		ArrayList<Integer> color_z = new ArrayList<Integer>();
		double min_x1 = one.x, middle_x1 = two.x, min_y1 = one.y, middle_y1 = two.y, min_z1 = one.z, middle_z1 = two.z;

		double x_range1 = two.x - one.x, x_range2 = three.x - two.x;
		double y_range1 = two.y - one.y, y_range2 = three.y - two.y;
		double z_range1 = two.z - one.z, z_range2 = three.z - two.z;

		for (int i = 0; i <= Math.pow(2, NUMBER_OF_BITS); i++) {
			double value = -1.0 + 2.0 * i / (Math.pow(2, NUMBER_OF_BITS));
			value_a.add(value);

			if (value <= 0) {
				int x = (int) (i / Math.pow(2, NUMBER_OF_BITS - 1) * x_range1 + min_x1);
				int y = (int) (i / Math.pow(2, NUMBER_OF_BITS - 1) * y_range1 + min_y1);
				int z = (int) (i / Math.pow(2, NUMBER_OF_BITS - 1) * z_range1 + min_z1);
				color_x.add(x);
				color_y.add(y);
				color_z.add(z);
			} else {
				int j = (int) (i - Math.pow(2, NUMBER_OF_BITS - 1));
				int x = (int) (j / Math.pow(2, NUMBER_OF_BITS - 1) * x_range2 + middle_x1);
				int y = (int) (j / Math.pow(2, NUMBER_OF_BITS - 1) * y_range2 + middle_y1);
				int z = (int) (j / Math.pow(2, NUMBER_OF_BITS - 1) * z_range2 + middle_z1);
				color_x.add(x);
				color_y.add(y);
				color_z.add(z);
			}
		}

		StringBuffer cScale = new StringBuffer();
		System.out.println("Color Scale:" + "\r\n");
		for (int i = 0; i < value_a.size(); i++) {
			double d = value_a.get(i);
			String s = new DecimalFormat("#0.00000000").format(d) + "		";
			System.out.print(s);
			System.out.print(color_x.get(i) + ",");
			System.out.print(color_y.get(i) + ",");
			System.out.print(color_z.get(i) + "\r\n");
			cScale.append(s).append(color_x.get(i) + ",").append(color_y.get(i) + ",").append(color_z.get(i) + "\r\n");
		}

		saveFile("colorMap/rgb_color_scale", cScale);

		IplImage whiteImg = IplImage.create(100, (int) Math.pow(2, NUMBER_OF_BITS), IPL_DEPTH_8U, 3);
		for (int i = 0; i < Math.pow(2, NUMBER_OF_BITS); i++) {
			CvScalar Minc = cvScalar(color_x.get(i), color_y.get(i), color_z.get(i), 0);
			for (int j = 0; j < 100; j++) {
				opencv_core.cvSet2D(whiteImg, i, j, Minc);
			}
		}

		cvShowImage("", whiteImg);
		cvWaitKey(0);
	}

	/**
	 * @param xChannelBucket
	 * @param yChannelBucket
	 * @param zChannelBucket
	 */
	private static void generateColorSets(HashMap<Integer, Double> xChannelBucket, HashMap<Integer, Double> yChannelBucket, HashMap<Integer, Double> zChannelBucket) {
		StringBuffer b = new StringBuffer();
		StringBuffer colors = new StringBuffer();
		System.out.println("COLOR MAP: \r\n");
		colors.append("ColorMAP: RGB " + "number of bits: " + NUMBER_OF_BITS + "\r\n");
		int color_id = 0;
		for (int i = 0; i < xChannelBucket.size(); i++) {
			double x = xChannelBucket.get(i);
			double xcolor;
			if (x > 0) {
				xcolor = colorFromScale(x, two.x, three.x, 0, 1);
			} else {
				xcolor = colorFromScale(x, one.x, two.x, -1, 0);
			}
			for (int j = 0; j < yChannelBucket.size(); j++) {
				double y = yChannelBucket.get(j);
				double ycolor;
				if (x > 0) {
					ycolor = colorFromScale(y, two.y, three.y, 0, 1);
				} else {
					ycolor = colorFromScale(y, one.y, two.y, -1, 0);
				}
				for (int k = 0; k < zChannelBucket.size(); k++) {
					double z = zChannelBucket.get(k);
					double zcolor;
					if (x > 0) {
						zcolor = colorFromScale(z, two.z, three.z, 0, 1);
					} else {
						zcolor = colorFromScale(z, one.z, two.z, -1, 0);
					}
					b.append("colorID: " + color_id + "     " + x + " " + y + " " + z + "\r\n");
					colors.append("ColorID: " + color_id + "	" + xcolor + "  " + ycolor + "  " + zcolor + "\r\n");
					System.out.println("ColorID: " + color_id + "	" + xcolor + "  " + ycolor + "  " + zcolor);
					color_id++;
				}
			}
		}
		saveFile("colorMap/rgb_colors_", colors);
	}

	static void splitBitsByColorChannel(COLOR_MODEL colorModel) {

		int eachChannelBits = NUMBER_OF_BITS / 3;
		int excessChannelBit = NUMBER_OF_BITS % 3;

		COLOR_MAP.x_bits = eachChannelBits + excessChannelBit;
		COLOR_MAP.y_bits = eachChannelBits;
		COLOR_MAP.z_bits = eachChannelBits;

		int boxWidthX = (int) Math.pow(2, (colorModel.getXbits() - COLOR_MAP.x_bits));
		int boxWidthY = (int) Math.pow(2, (colorModel.getYbits() - COLOR_MAP.y_bits));
		int boxWidthZ = (int) Math.pow(2, (colorModel.getZbits() - COLOR_MAP.z_bits));

		COLOR_MAP.x_buckets = (int) (Math.pow(2, colorModel.getXbits()) / boxWidthX);
		COLOR_MAP.y_buckets = (int) (Math.pow(2, colorModel.getYbits()) / boxWidthY);
		COLOR_MAP.z_buckets = (int) (Math.pow(2, colorModel.getZbits()) / boxWidthZ);
	}

	static void saveFile(String fileName, StringBuffer b) {
		File cMap;
		BufferedWriter bWriter = null;
		try {
			if (fileName != null && !fileName.isEmpty()) {
				cMap = new File(fileName + System.currentTimeMillis() + ".txt");
				if (!cMap.exists()) {
					try {
						cMap.createNewFile();
						System.out.println("Created file: " + cMap.getAbsolutePath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					bWriter = new BufferedWriter(new FileWriter(cMap));
					bWriter.write(b.toString());
					bWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();

				}
			}
		} finally {
			if (bWriter != null)
				try {
					bWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	static void convertToRGB(String[] args, Colors t) {
		ConvertColors converter = new ConvertColors();
		String[] cn1 = args[2].split(",");
		String[] cn2 = args[3].split(",");
		String[] cn3 = args[4].split(",");
		NUMBER_OF_BITS = Integer.parseInt(args[1]);

		if ("rgb".equalsIgnoreCase(args[0])) {
			colorModel = COLOR_MODEL.RGB;

			one = t.new ColorInstance(Integer.parseInt(cn1[0]), Integer.parseInt(cn1[1]), Integer.parseInt(cn1[2]));
			two = t.new ColorInstance(Integer.parseInt(cn2[0]), Integer.parseInt(cn2[1]), Integer.parseInt(cn2[2]));
			three = t.new ColorInstance(Integer.parseInt(cn3[0]), Integer.parseInt(cn3[1]), Integer.parseInt(cn3[2]));
		} else if ("lab".equalsIgnoreCase(args[0])) {

			double dx = Double.parseDouble(cn1[0]);
			double dy = Double.parseDouble(cn1[1]);
			double dz = Double.parseDouble(cn1[2]);
			double[] d = { dx, dy, dz };
			int[] arr = converter.LABtoRGB(d);
			one = t.new ColorInstance(arr[0], arr[1], arr[2]);

			dx = Double.parseDouble(cn2[0]);
			dy = Double.parseDouble(cn2[1]);
			dz = Double.parseDouble(cn2[2]);
			double[] d2 = { dx, dy, dz };
			int[] arr2 = converter.LABtoRGB(d2);
			two = t.new ColorInstance(arr2[0], arr2[1], arr2[2]);

			dx = Double.parseDouble(cn3[0]);
			dy = Double.parseDouble(cn3[1]);
			dz = Double.parseDouble(cn3[2]);
			double[] d3 = { dx, dy, dz };
			int[] arr3 = converter.LABtoRGB(d3);
			three = t.new ColorInstance(arr3[0], arr3[1], arr3[2]);
			colorModel = COLOR_MODEL.RGB;
		} else if ("xyz".equalsIgnoreCase(args[0])) {
			double dx = Double.parseDouble(cn1[0]);
			double dy = Double.parseDouble(cn1[1]);
			double dz = Double.parseDouble(cn1[2]);
			double[] d = { dx, dy, dz };
			int[] arr = converter.XYZtoRGB(d);
			one = t.new ColorInstance(arr[0], arr[1], arr[2]);

			dx = Double.parseDouble(cn2[0]);
			dy = Double.parseDouble(cn2[1]);
			dz = Double.parseDouble(cn2[2]);
			double[] d2 = { dx, dy, dz };
			int[] arr2 = converter.XYZtoRGB(d2);
			two = t.new ColorInstance(arr2[0], arr2[1], arr2[2]);

			dx = Double.parseDouble(cn3[0]);
			dy = Double.parseDouble(cn3[1]);
			dz = Double.parseDouble(cn3[2]);
			double[] d3 = { dx, dy, dz };
			int[] arr3 = converter.XYZtoRGB(d3);
			three = t.new ColorInstance(arr3[0], arr3[1], arr3[2]);
			colorModel = COLOR_MODEL.RGB;
		} else if ("hls".equalsIgnoreCase(args[0])) {
			float hue = Float.parseFloat(cn1[0]);
			float luminence = Float.parseFloat(cn1[1]);
			float saturation = Float.parseFloat(cn1[2]);
			int[] arr = converter.HLStoRGB(hue, luminence, saturation);
			one = t.new ColorInstance(arr[0], arr[1], arr[2]);

			hue = Float.parseFloat(cn2[0]);
			saturation = Float.parseFloat(cn2[1]);
			luminence = Float.parseFloat(cn2[2]);
			int[] arr2 = converter.HLStoRGB(hue, luminence, saturation);
			two = t.new ColorInstance(arr2[0], arr2[1], arr2[2]);

			hue = Float.parseFloat(cn3[0]);
			saturation = Float.parseFloat(cn3[1]);
			luminence = Float.parseFloat(cn3[2]);
			int[] arr3 = converter.HLStoRGB(hue, luminence, saturation);
			three = t.new ColorInstance(arr3[0], arr3[1], arr3[2]);
			colorModel = COLOR_MODEL.RGB;
		} else if ("yuv".equalsIgnoreCase(args[0])) {
			double dx = Double.parseDouble(cn1[0]);
			double dy = Double.parseDouble(cn1[1]);
			double dz = Double.parseDouble(cn1[2]);
			int[] arr = converter.YUVtoRGB(dx, dy, dz);
			one = t.new ColorInstance(arr[0], arr[1], arr[2]);

			dx = Double.parseDouble(cn2[0]);
			dy = Double.parseDouble(cn2[1]);
			dz = Double.parseDouble(cn2[2]);
			double[] d2 = { dx, dy, dz };
			int[] arr2 = converter.YUVtoRGB(dx, dy, dz);
			two = t.new ColorInstance(arr2[0], arr2[1], arr2[2]);

			dx = Double.parseDouble(cn3[0]);
			dy = Double.parseDouble(cn3[1]);
			dz = Double.parseDouble(cn3[2]);
			double[] d3 = { dx, dy, dz };
			int[] arr3 = converter.YUVtoRGB(dx, dy, dz);
			three = t.new ColorInstance(arr3[0], arr3[1], arr3[2]);
			colorModel = COLOR_MODEL.RGB;
		} else if ("ycbcr".equalsIgnoreCase(args[0])) {
			double dx = Double.parseDouble(cn1[0]);
			double dy = Double.parseDouble(cn1[1]);
			double dz = Double.parseDouble(cn1[2]);
			int[] arr = converter.YCbCrtoRGB(dx, dy, dz);
			one = t.new ColorInstance(arr[0], arr[1], arr[2]);

			dx = Double.parseDouble(cn2[0]);
			dy = Double.parseDouble(cn2[1]);
			dz = Double.parseDouble(cn2[2]);
			double[] d2 = { dx, dy, dz };
			int[] arr2 = converter.YCbCrtoRGB(dx, dy, dz);
			two = t.new ColorInstance(arr2[0], arr2[1], arr2[2]);

			dx = Double.parseDouble(cn3[0]);
			dy = Double.parseDouble(cn3[1]);
			dz = Double.parseDouble(cn3[2]);
			double[] d3 = { dx, dy, dz };
			int[] arr3 = converter.YCbCrtoRGB(dx, dy, dz);
			three = t.new ColorInstance(arr3[0], arr3[1], arr3[2]);
			colorModel = COLOR_MODEL.RGB;
		} else if ("yiq".equalsIgnoreCase(args[0])) {
			double dx = Double.parseDouble(cn1[0]);
			double dy = Double.parseDouble(cn1[1]);
			double dz = Double.parseDouble(cn1[2]);
			int[] arr = converter.YIQtoRGB(dx, dy, dz);
			one = t.new ColorInstance(arr[0], arr[1], arr[2]);

			dx = Double.parseDouble(cn2[0]);
			dy = Double.parseDouble(cn2[1]);
			dz = Double.parseDouble(cn2[2]);
			double[] d2 = { dx, dy, dz };
			int[] arr2 = converter.YIQtoRGB(dx, dy, dz);
			two = t.new ColorInstance(arr2[0], arr2[1], arr2[2]);

			dx = Double.parseDouble(cn3[0]);
			dy = Double.parseDouble(cn3[1]);
			dz = Double.parseDouble(cn3[2]);
			double[] d3 = { dx, dy, dz };
			int[] arr3 = converter.YIQtoRGB(dx, dy, dz);
			three = t.new ColorInstance(arr3[0], arr3[1], arr3[2]);
			colorModel = COLOR_MODEL.RGB;
		}
	}

	/**
	 * @param xChannel
	 * @param yChannel
	 * @param zChannel
	 */
	private static void saveChannelIntensities(HashMap<Double, Double> xChannel, HashMap<Double, Double> yChannel, HashMap<Double, Double> zChannel) {
		StringBuffer x = new StringBuffer();
		StringBuffer y = new StringBuffer();
		StringBuffer z = new StringBuffer();
		for (Entry e : xChannel.entrySet()) {
			x.append(e.getValue() + " " + e.getKey() + "\r\n");
		}
		for (Entry e : yChannel.entrySet()) {
			y.append(e.getValue() + " " + e.getKey() + "\r\n");
		}
		for (Entry e : zChannel.entrySet()) {
			z.append(e.getValue() + " " + e.getKey() + "\r\n");
		}
		saveFile("colorMap/rgb_x_channel_scale", x);
		saveFile("colorMap/rgb_y_channel_scale", y);
		saveFile("colorMap/rgb_z_channel_scale", z);
	}

	class ColorInstance {
		int x;
		int y;
		int z;

		public ColorInstance(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	static void printColorInstances() {
		System.out.println("C-1: " + one.x + "," + one.y + "," + one.z);
		System.out.println("C0: " + two.x + "," + two.y + "," + two.z);
		System.out.println("C+1: " + three.x + "," + three.y + "," + three.z);
	}

	static void printColorModel() {
		System.out.println("Color Model" + colorModel);
	}

	static void printNumberOfBits() {
		System.out.println("Number of bits for color map: " + NUMBER_OF_BITS);
	}

	static void printMap(Map<?, ?> map) {
		for (Entry e : map.entrySet()) {
			System.out.println(e.getKey() + " " + e.getValue());
		}
	}

	// static void visualizeColorScale(HashMap<Double, Double> xChannel,
	// HashMap<Double, Double> yChannel, HashMap<Double, Double> zChannel) {
	// ColorScale grid = new ColorScale();
	// JFrame window = new JFrame();
	// window.setSize(840,250);
	// window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// window.getContentPane().add(grid);
	// window.setVisible(true);
	// grid.fillCell(0, 0, new Color(134, 34, 123));
	// grid.fillCell(1, 0 , new Color(230, 60, 200));
	//
	// }

	// class ScaleRect{
	//
	// }
	// static class ColorScale extends JPanel {
	// private List<Point> fillRects;
	// Color color;
	// public ColorScale() {
	// fillRects = new ArrayList<Point>();
	// }
	//
	// @Override
	// protected void paintComponent(Graphics g) {
	// super.paintComponent(g);
	// for (Point fillRect : fillRects) {
	// int cellX = 10 + (fillRect.x * 10);
	// int cellY = 10 + (fillRect.y * 10);
	// g.setColor(color);
	//
	// g.fillRect(cellX, cellY, 10, 200);
	// g.setColor(new Color(230, 60, 200));
	// }
	// g.setColor(Color.BLACK);
	// g.drawRect(10, 10, 800, 200);
	//
	// for (int i = 10; i <= 800; i += 10) {
	// g.drawLine(i, 10, i, 210);
	// }
	// }
	//
	// public void setColorGrid(Color c){
	// this.color = c;
	// }
	// public void fillCell(int x, int y, Color c) {
	// this.color = c;
	// fillRects.add(new Point(x, y));
	// repaint();
	// }
	// }

	/* PART 4 */

	// static void captureFrame() {
	// FrameGrabber frameGrabber = new
	// OpenCVFrameGrabber("/Users/kvivekanandan/Desktop/ASU/CSE_598_Multimedia_Information_Systems/sampleDataP1/1.mp4");
	//
	// try {
	// frameGrabber.start();
	// OpenCVFrameConverter.ToIplImage converter = new
	// OpenCVFrameConverter.ToIplImage();
	// frameGrabber.setFrameRate(30);
	// int length = frameGrabber.getLengthInFrames();
	// int frame_one = 8;
	// int frame_two = 220;
	// Frame f;
	// Frame f1 = null;
	// Frame f2 = null;
	// Frame g1, g2;
	// CanvasFrame canvas = null;
	// while ((f = frameGrabber.grab()) != null) {
	// if (frameGrabber.getFrameNumber() == frame_one) {
	// f1 = f;
	// canvas = new CanvasFrame("" + frameGrabber.getFrameNumber());
	// canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	// canvas.setCanvasSize(frameGrabber.getImageWidth(),
	// frameGrabber.getImageHeight());
	// canvas.showImage(f);
	//
	// } else if (frameGrabber.getFrameNumber() == frame_two) {
	// f2 = f;
	// canvas = new CanvasFrame("" + frameGrabber.getFrameNumber());
	// canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	// canvas.setCanvasSize(frameGrabber.getImageWidth(),
	// frameGrabber.getImageHeight());
	// canvas.showImage(f);
	// break;
	// }
	// }
	//
	// frameGrabber.stop();
	// Mat mf1 = converter.convertToMat(f1);
	// Mat mf2 = converter.convertToMat(f2);
	// Mat mg1 = new Mat();
	// Mat mg2 = new Mat();
	// cvtColor(mf1, mg1, COLOR_BGR2GRAY);
	// Frame gray = converter.convert(mg1);
	// canvas.showImage(gray);
	//
	// cvtColor(mf2, mg2, COLOR_BGR2GRAY);
	// Frame gray2 = converter.convert(mg2);
	// canvas.showImage(gray2);
	//
	// IplImage diffGray = IplImage.create(converter.convert(gray).width(),
	// converter.convert(gray).height(), IPL_DEPTH_8U, 1);
	// IplImage iplGray = converter.convertToIplImage(gray);
	// IplImage iplGray2 = converter.convertToIplImage(gray2);
	//
	// cvAbsDiff(iplGray, iplGray2, diffGray);
	//
	// CanvasFrame s = new CanvasFrame("" + frameGrabber.getFrameNumber());
	// s.showImage(converter.convert(diffGray));
	// Mat diffMatGray = converter.convertToMat(converter.convert(diffGray));
	// Mat diffDestGray = new Mat();
	// applyColorMap(diffMatGray, diffDestGray, COLORMAP_SPRING);
	//
	// Frame finalColorMapDiff = converter.convert(diffDestGray);
	// s.showImage(finalColorMapDiff);
	// canvas.dispose();
	// s.dispose();
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	// class Scale extends JPanel {
	// public void paint(Graphics g) {
	// Graphics2D g2 = (Graphics2D) g;
	// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	// RenderingHints.VALUE_ANTIALIAS_ON);
	// Font font = new Font("Serif", Font.PLAIN, 96);
	// g2.setFont(font);
	// g2.drawString("Text", 40, 120);
	// }
	// }
	//
	// static void convertColorScale() {
	// float[] hsbValues = new float[3];
	//
	// hsbValues = Color.RGBtoHSB(one.x, one.y, one.z, hsbValues);
	//
	// float hue, saturation, brightness;
	// hue = hsbValues[0];
	// saturation = hsbValues[1];
	// brightness = hsbValues[2];
	//
	// JFrame f = new JFrame();
	// f.getContentPane().add(new Colors().new Scale());
	// f.setSize(300, 200);
	// f.setVisible(true);
	// f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//
	// Color.HSBtoRGB(hue, saturation, brightness);
	//
	// }
}
