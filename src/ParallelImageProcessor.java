import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelImageProcessor {

    private static final int THRESHOLD = 64;

    private static final ForkJoinPool pool = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors()
    );

    private static class GaussianBlurTask extends RecursiveAction {
        private final BufferedImage source;
        private final BufferedImage result;
        private final int startRow;
        private final int endRow;
        private final double[][] kernel;

        GaussianBlurTask(BufferedImage source, BufferedImage result,
                         int startRow, int endRow, double[][] kernel) {
            this.source = source;
            this.result = result;
            this.startRow = startRow;
            this.endRow = endRow;
            this.kernel = kernel;
        }

        @Override
        protected void compute() {
            int numRows = endRow - startRow;

            // Daca numarul de linii este sub prag, procesam secvential
            // (cazul de baza al recursivitatii - granularitate fina)
            if (numRows <= THRESHOLD) {
                computeDirectly();
                return;
            }

            int mid = startRow + numRows / 2;

            GaussianBlurTask upper = new GaussianBlurTask(source, result, startRow, mid, kernel);
            GaussianBlurTask lower = new GaussianBlurTask(source, result, mid, endRow, kernel);

            invokeAll(upper, lower);
        }

        private void computeDirectly() {
            int width = source.getWidth();
            int height = source.getHeight();

            for (int y = startRow; y < endRow; y++) {
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
        }
    }

    public static BufferedImage applyGaussianBlur(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double[][] kernel = {
            {1.0 / 16, 2.0 / 16, 1.0 / 16},
            {2.0 / 16, 4.0 / 16, 2.0 / 16},
            {1.0 / 16, 2.0 / 16, 1.0 / 16}
        };

        pool.invoke(new GaussianBlurTask(source, result, 0, height, kernel));
        return result;
    }

    private static class GrayscaleTask extends RecursiveAction {
        private final BufferedImage source;
        private final BufferedImage result;
        private final int startRow;
        private final int endRow;

        GrayscaleTask(BufferedImage source, BufferedImage result,
                      int startRow, int endRow) {
            this.source = source;
            this.result = result;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        protected void compute() {
            int numRows = endRow - startRow;

            if (numRows <= THRESHOLD) {
                computeDirectly();
                return;
            }

            int mid = startRow + numRows / 2;
            GrayscaleTask upper = new GrayscaleTask(source, result, startRow, mid);
            GrayscaleTask lower = new GrayscaleTask(source, result, mid, endRow);

            upper.fork();
            lower.compute();
            upper.join();
        }

        private void computeDirectly() {
            int width = source.getWidth();
            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = source.getRGB(x, y);
                    int r = ImageUtils.getRed(rgb);
                    int g = ImageUtils.getGreen(rgb);
                    int b = ImageUtils.getBlue(rgb);

                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    result.setRGB(x, y, ImageUtils.toRGB(gray, gray, gray));
                }
            }
        }
    }

    public static BufferedImage convertToGrayscale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        pool.invoke(new GrayscaleTask(source, result, 0, height));
        return result;
    }

    private static class SobelTask extends RecursiveAction {
        private final BufferedImage graySource;
        private final BufferedImage result;
        private final int startRow;
        private final int endRow;

        SobelTask(BufferedImage graySource, BufferedImage result,
                  int startRow, int endRow) {
            this.graySource = graySource;
            this.result = result;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        protected void compute() {
            int numRows = endRow - startRow;

            if (numRows <= THRESHOLD) {
                computeDirectly();
                return;
            }

            int mid = startRow + numRows / 2;
            SobelTask upper = new SobelTask(graySource, result, startRow, mid);
            SobelTask lower = new SobelTask(graySource, result, mid, endRow);

            invokeAll(upper, lower);
        }

        private void computeDirectly() {
            int width = graySource.getWidth();
            int height = graySource.getHeight();

            int[][] sobelX = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
            int[][] sobelY = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < width; x++) {
                    int gx = 0, gy = 0;

                    for (int ky = -1; ky <= 1; ky++) {
                        for (int kx = -1; kx <= 1; kx++) {
                            int px = Math.max(0, Math.min(width - 1, x + kx));
                            int py = Math.max(0, Math.min(height - 1, y + ky));

                            int intensity = ImageUtils.getRed(graySource.getRGB(px, py));
                            gx += intensity * sobelX[ky + 1][kx + 1];
                            gy += intensity * sobelY[ky + 1][kx + 1];
                        }
                    }

                    int magnitude = (int) Math.min(255, Math.sqrt(gx * gx + gy * gy));
                    result.setRGB(x, y, ImageUtils.toRGB(magnitude, magnitude, magnitude));
                }
            }
        }
    }

    public static BufferedImage applySobelEdgeDetection(BufferedImage source) {
        BufferedImage gray = convertToGrayscale(source);

        int width = gray.getWidth();
        int height = gray.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        pool.invoke(new SobelTask(gray, result, 0, height));
        return result;
    }

    public static int getParallelism() {
        return pool.getParallelism();
    }
}
