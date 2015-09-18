package project.mis.group8.phase1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_videoio.cvCaptureFromFile;

import org.bytedeco.javacpp.FlyCapture2.Image;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/**
 * @author kvivekanandan Sep 11, 2015 Test.java
 */

public class Colors {

	public static int NUMBER_OF_BITS = 4;
	public static ColorMap COLOR_MAP;
	public static ColorInstance one;
	public static ColorInstance two;
	public static ColorInstance three;

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

	public static void main(String args[]) {
		captureFrame();

		 Colors t = new Colors();
		 COLOR_MAP = new ColorMap();
		 one = t.new ColorInstance(134, 34, 123);
		 two = t.new ColorInstance(180, 60, 155);
		 three = t.new ColorInstance(230, 90, 200);

		// one = t.new ColorInstance(0, 0, 0);
		// two = t.new ColorInstance(50, 50, 50);
		// three = t.new ColorInstance(100, 100, 100);

		 colorMap(COLOR_MODEL.RGB, one, two, three, NUMBER_OF_BITS);
	}

	static void captureFrame() {
		FrameGrabber frameGrabber = new OpenCVFrameGrabber("/Users/kvivekanandan/Desktop/ASU/CSE_598_Multimedia_Information_Systems/sampleDataP1/1.mp4");

		try {
			frameGrabber.start();
			OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
			frameGrabber.setFrameRate(30);
			int length = frameGrabber.getLengthInFrames();
			int frame_one = 8;
			int frame_two = 220;
			Frame f;
			Frame f1 = null;
			Frame f2 = null;
			Frame g1, g2;
			CanvasFrame canvas = null;
			while ((f = frameGrabber.grab()) != null) {
				if (frameGrabber.getFrameNumber() == frame_one ) {
					f1 = f;
					canvas = new CanvasFrame("" + frameGrabber.getFrameNumber());
					canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
					canvas.setCanvasSize(frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
					canvas.showImage(f);
					
				} else if(frameGrabber.getFrameNumber() == frame_two){
					f2 = f;
					canvas = new CanvasFrame("" + frameGrabber.getFrameNumber());
					canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
					canvas.setCanvasSize(frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
					canvas.showImage(f);
					break;
				}
			}
			
			frameGrabber.stop();
			Mat mf1 = converter.convertToMat(f1);
			Mat mf2 = converter.convertToMat(f2);
			Mat mg1 = new Mat(); Mat mg2 = 	new Mat();
			cvtColor(mf1,mg1,COLOR_BGR2GRAY);
			Frame gray = converter.convert(mg1);
			canvas.showImage(gray);
			
			cvtColor(mf2,mg2,COLOR_BGR2GRAY);
			Frame gray2 = converter.convert(mg2);
			canvas.showImage(gray2);
			
			IplImage diffGray = IplImage.create(converter.convert(gray).width(), converter.convert(gray).height(), IPL_DEPTH_8U, 1);
			IplImage iplGray = converter.convertToIplImage(gray);
			IplImage iplGray2 = converter.convertToIplImage(gray2);
			
			cvAbsDiff(iplGray, iplGray2, diffGray);
			
			CanvasFrame s = new CanvasFrame("" + frameGrabber.getFrameNumber());
			s.showImage(converter.convert(diffGray));
			Mat diffMatGray = converter.convertToMat(converter.convert(diffGray));
			Mat diffDestGray = new Mat();
			applyColorMap(diffMatGray, diffDestGray, COLORMAP_SPRING);
			
			Frame finalColorMapDiff = converter.convert(diffDestGray);
			s.showImage(finalColorMapDiff);
			canvas.dispose();
			s.dispose();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		int prev_bucket = 0;
		int current_bucket = 0;
		while (i <= l) {
			int b = buckets;
			current_bucket = (counter / b);
			if (prev_bucket != current_bucket) {
				channelBucket.put(prev_bucket, (value / b));
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

			HashMap<Double, Double> xChannel = new LinkedHashMap<Double, Double>();
			HashMap<Integer, Double> xChannelBucket = new HashMap<Integer, Double>();
			System.out.println("X Channel: ");
			colorScale(one.x, two.x, -1, 0, xChannel);
			colorScale(two.x, three.x, 0, 1, xChannel);
			printMap(xChannel);
			int x_range = three.x - one.x;
			COLOR_MAP.x_buckets = x_range / (int) Math.pow(2, COLOR_MAP.x_bits);
			calculateChannelBucket(one.x, three.x, COLOR_MAP.x_buckets, xChannel, xChannelBucket);
			printMap(xChannelBucket);

			HashMap<Double, Double> yChannel = new HashMap<Double, Double>();
			HashMap<Integer, Double> yChannelBucket = new HashMap<Integer, Double>();
			System.out.println("Y Channel: ");
			yChannel = colorScale(one.y, two.y, -1, 0, yChannel);
			yChannel = colorScale(two.y, three.y, 0, 1, yChannel);
			printMap(yChannel);
			int y_range = three.y - one.y;
			COLOR_MAP.y_buckets = y_range / (int) Math.pow(2, COLOR_MAP.y_bits);
			calculateChannelBucket(one.y, three.y, COLOR_MAP.y_buckets, yChannel, yChannelBucket);
			System.out.println("Y Color Channel: ");
			printMap(yChannelBucket);

			HashMap<Double, Double> zChannel = new HashMap<Double, Double>();
			HashMap<Integer, Double> zChannelBucket = new HashMap<Integer, Double>();
			System.out.println("Z Channel: ");
			zChannel = colorScale(one.z, two.z, -1, 0, zChannel);
			zChannel = colorScale(two.z, three.z, 0, 1, zChannel);
			printMap(zChannel);
			int z_range = three.z - one.z;
			COLOR_MAP.z_buckets = z_range / (int) Math.pow(2, COLOR_MAP.z_bits);
			calculateChannelBucket(one.z, three.z, COLOR_MAP.z_buckets, zChannel, zChannelBucket);
			System.out.println("Z Color Channel: ");
			printMap(zChannelBucket);

			generateColorSets(xChannelBucket, yChannelBucket, zChannelBucket);
		}
		default:
			break;
		}

	}

	/**
	 * @param xChannelBucket
	 * @param yChannelBucket
	 * @param zChannelBucket
	 */
	private static void generateColorSets(HashMap<Integer, Double> xChannelBucket, HashMap<Integer, Double> yChannelBucket, HashMap<Integer, Double> zChannelBucket) {
		System.out.println("Color Sets: ");
		StringBuffer b = new StringBuffer();
		StringBuffer colors = new StringBuffer();
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
		File cMap = new File("colorMap/rgb_" + System.currentTimeMillis() + ".txt");
		File colorsMap = new File("colorMap/rgb_colors_" + System.currentTimeMillis() + ".txt");
		if (!cMap.exists()) {
			try {
				cMap.createNewFile();
				System.out.println("Created file");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (!colorsMap.exists()) {
			try {
				cMap.createNewFile();
				System.out.println("Created colors file");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		BufferedWriter bWriter = null;
		try {
			bWriter = new BufferedWriter(new FileWriter(cMap));
			bWriter.write(b.toString());
			bWriter.flush();

			bWriter = new BufferedWriter(new FileWriter(colorsMap));
			bWriter.write(colors.toString());
			bWriter.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bWriter != null)
				try {
					bWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	static void printMap(Map<?, ?> map) {
		for (Entry e : map.entrySet()) {
			System.out.println(e.getKey() + " " + e.getValue());
		}
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

}
