
import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	static BufferedImage imgOne;

	// Modify the height and width values here to read and display an image with
	// different dimensions.
	int width = 512;
	int height = 512;

	/**
	 * Read Image RGB
	 * Reads the image of given width and height at the given imgPath into the
	 * provided BufferedImage.
	 */
	private static void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
		try {
			int frameLength = width * height * 3;

			File file = new File(imgPath);
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
				raf.seek(0);

				long len = frameLength;
				byte[] bytes = new byte[(int) len];

				raf.read(bytes);

				int ind = 0;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						byte a = 0;
						byte r = bytes[ind];
						byte g = bytes[ind + height * width];
						byte b = bytes[ind + height * width * 2];

						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						img.setRGB(x, y, pix);
						ind++;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static BufferedImage filter(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		BufferedImage filteredImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[][] kernel = { { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } };
		int kernelSum = 9;

		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				int rSum = 0, gSum = 0, bSum = 0;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						int pixel = img.getRGB(x + i, y + j);
						rSum += ((pixel >> 16) & 0xFF) * kernel[j + 1][i + 1];
						gSum += ((pixel >> 8) & 0xFF) * kernel[j + 1][i + 1];
						bSum += (pixel & 0xFF) * kernel[j + 1][i + 1];
					}
				}
				int rNew = Math.min(255, rSum / kernelSum);
				int gNew = Math.min(255, gSum / kernelSum);
				int bNew = Math.min(255, bSum / kernelSum);
				filteredImg.setRGB(x, y, (0xFF << 24) | (rNew << 16) | (gNew << 8) | bNew);
			}
		}
		return filteredImg;
	}

	private static BufferedImage scaleImage(BufferedImage img, float scale) {
		int newWidth = (int) (img.getWidth() * scale);
		int newHeight = (int) (img.getHeight() * scale);
		BufferedImage scaledImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

		for (int i = 0; i < newHeight; i++) {
			for (int j = 0; j < newWidth; j++) {
				int srcX = (int) (j / scale);
				int srcY = (int) (i / scale);
				scaledImg.setRGB(j, i, img.getRGB(srcX, srcY));
			}
		}
		return scaledImg;
	}

	private static BufferedImage quantizeImage(BufferedImage img, int Q, int mode) {
		int levels = (int) Math.pow(2, Q);
		int stepSize = 256 / levels;
		BufferedImage quantizedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = img.getRGB(x, y);
				int r = (pixel >> 16) & 0xFF;
				int g = (pixel >> 8) & 0xFF;
				int b = pixel & 0xFF;

				if (mode == -1) { // Uniform quantization
					r = (r / stepSize) * stepSize;
					g = (g / stepSize) * stepSize;
					b = (b / stepSize) * stepSize;
				} else { // Logarithmic quantization
					r = quantizeLogarithmic(r, mode, levels);
					g = quantizeLogarithmic(g, mode, levels);
					b = quantizeLogarithmic(b, mode, levels);
				}

				quantizedImg.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
			}
		}
		return quantizedImg;
	}

	private static int quantizeLogarithmic(int value, int pivot, int levels) {
		int stepSize = 256 / levels;
		if (value < pivot) {
			return (value / stepSize) * stepSize + stepSize / 2;
		} else {
			return pivot + ((value - pivot) / stepSize) * stepSize + stepSize / 2;
		}
	}

	public void showIms(BufferedImage imgOne) {

		// Read in the specified image
		// imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		// readImageRGB(width, height, args[0], imgOne);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();

		//
		String imgPath = args[0];
		float scale = Float.parseFloat(args[1]);
		int Q = Integer.parseInt(args[2]);
		int mode = Integer.parseInt(args[3]);
		int width = 512, height = 512;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		readImageRGB(width, height, imgPath, img);

		// Filter image with 3x3 kernel
		BufferedImage filteredImg = filter(img);

		// Scale image
		BufferedImage scaledImg = scaleImage(filteredImg, scale);

		// Quantize image
		BufferedImage quantizedImg = quantizeImage(scaledImg, Q, mode);

		// Save output
		// saveImage(quantizedImg, "outputUni.png");
		ren.showIms(quantizedImg);
	}

}
