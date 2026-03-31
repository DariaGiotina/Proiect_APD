import java.awt.image.BufferedImage;

public class SequentialImageProcessor {

    public static BufferedImage applyGaussianBlur(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double[][] kernel = {
            {1.0 / 16, 2.0 / 16, 1.0 / 16},
            {2.0 / 16, 4.0 / 16, 2.0 / 16},
            {1.0 / 16, 2.0 / 16, 1.0 / 16}
        };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sumR = 0, sumG = 0, sumB = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int px = Math.max(0, Math.min(width - 1, x + kx));
                        int py = Math.max(0, Math.min(height - 1, y + ky));

                        int rgb = source.getRGB(px, py);
                        double weight = kernel[ky + 1][kx + 1];

                        sumR += ImageUtils.getRed(rgb) * weight;
                        sumG += ImageUtils.getGreen(rgb) * weight;
                        sumB += ImageUtils.getBlue(rgb) * weight;
                    }
                }

                result.setRGB(x, y, ImageUtils.toRGB((int) sumR, (int) sumG, (int) sumB));
            }
        }

        return result;
    }

    public static BufferedImage convertToGrayscale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = ImageUtils.getRed(rgb);
                int g = ImageUtils.getGreen(rgb);
                int b = ImageUtils.getBlue(rgb);

                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                result.setRGB(x, y, ImageUtils.toRGB(gray, gray, gray));
            }
        }

        return result;
    }

    public static BufferedImage applySobelEdgeDetection(BufferedImage source) {
        BufferedImage gray = convertToGrayscale(source);
        int width = gray.getWidth();
        int height = gray.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[][] sobelX = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
        };
        int[][] sobelY = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
        };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gx = 0, gy = 0;

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int px = Math.max(0, Math.min(width - 1, x + kx));
                        int py = Math.max(0, Math.min(height - 1, y + ky));

                        int intensity = ImageUtils.getRed(gray.getRGB(px, py));

                        gx += intensity * sobelX[ky + 1][kx + 1];
                        gy += intensity * sobelY[ky + 1][kx + 1];
                    }
                }

                // Magnitudinea gradientului
                int magnitude = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));
                result.setRGB(x, y, ImageUtils.toRGB(magnitude, magnitude, magnitude));
            }
        }

        return result;
    }
}
