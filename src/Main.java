import java.awt.image.BufferedImage;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("  PROIECT APD - Procesare de Imagini: Secvential vs. Paralel");
        System.out.println("=============================================================");
        System.out.println();

        int numProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("Numar de procesoare disponibile: " + numProcessors);
        System.out.println("Numar de fire folosite (paralel): " + ParallelImageProcessor.getParallelism());
        System.out.println();

        int width = 4000;
        int height = 3000;

        BufferedImage testImage = null;
        try {
            if (args.length > 0 && new File(args[0]).exists()) {
                System.out.println("Se incarca imaginea: " + args[0]);
                testImage = ImageUtils.loadImage(args[0]);
                width = testImage.getWidth();
                height = testImage.getHeight();
            }
        } catch (Exception e) {
            System.out.println("Eroare la incarcarea imaginii: " + e.getMessage());
        }

        if (testImage == null) {
            System.out.println("Se genereaza o imagine de test de " + width + "x" + height + " pixeli...");
            testImage = ImageUtils.generateTestImage(width, height);
        }

        int totalPixels = width * height;
        System.out.println("Dimensiune imagine: " + width + " x " + height +
                " = " + String.format("%,d", totalPixels) + " pixeli");
        System.out.println();

        int numRuns = 3;

        System.out.println("-------------------------------------------------------------");
        System.out.println("  TESTUL 1: Gaussian Blur (Filtru de netezire 3x3)");
        System.out.println("-------------------------------------------------------------");

        runBenchmark("Gaussian Blur", testImage, numRuns, numProcessors,
                SequentialImageProcessor::applyGaussianBlur,
                ParallelImageProcessor::applyGaussianBlur);

        System.out.println("-------------------------------------------------------------");
        System.out.println("  TESTUL 2: Conversie la Grayscale");
        System.out.println("-------------------------------------------------------------");

        runBenchmark("Grayscale", testImage, numRuns, numProcessors,
                SequentialImageProcessor::convertToGrayscale,
                ParallelImageProcessor::convertToGrayscale);

        System.out.println("-------------------------------------------------------------");
        System.out.println("  TESTUL 3: Detectie de margini (Sobel)");
        System.out.println("-------------------------------------------------------------");

        runBenchmark("Sobel", testImage, numRuns, numProcessors,
                SequentialImageProcessor::applySobelEdgeDetection,
                ParallelImageProcessor::applySobelEdgeDetection);

        System.out.println("-------------------------------------------------------------");
        System.out.println("  Salvarea rezultatelor in fisiere...");
        System.out.println("-------------------------------------------------------------");

        try {
            String outputDir = "output";
            new File(outputDir).mkdirs();

            ImageUtils.saveImage(
                    SequentialImageProcessor.applyGaussianBlur(testImage),
                    outputDir + "/blur_secvential.png", "png");
            ImageUtils.saveImage(
                    ParallelImageProcessor.applyGaussianBlur(testImage),
                    outputDir + "/blur_paralel.png", "png");
            ImageUtils.saveImage(
                    SequentialImageProcessor.convertToGrayscale(testImage),
                    outputDir + "/grayscale_secvential.png", "png");
            ImageUtils.saveImage(
                    SequentialImageProcessor.applySobelEdgeDetection(testImage),
                    outputDir + "/sobel_secvential.png", "png");
            ImageUtils.saveImage(
                    ParallelImageProcessor.applySobelEdgeDetection(testImage),
                    outputDir + "/sobel_paralel.png", "png");

            System.out.println("Rezultatele au fost salvate in directorul '" + outputDir + "/'");
        } catch (Exception e) {
            System.out.println("Eroare la salvarea rezultatelor: " + e.getMessage());
        }

        System.out.println();
        System.out.println("-------------------------------------------------------------");
        System.out.println("  Validare: Rezultatele secventiale si paralele sunt identice?");
        System.out.println("-------------------------------------------------------------");

        BufferedImage seqBlur = SequentialImageProcessor.applyGaussianBlur(testImage);
        BufferedImage parBlur = ParallelImageProcessor.applyGaussianBlur(testImage);
        System.out.println("  Gaussian Blur:  " +
                (ImageUtils.imagesEqual(seqBlur, parBlur) ? "DA (identice)" : "NU (difera!)"));

        BufferedImage seqGray = SequentialImageProcessor.convertToGrayscale(testImage);
        BufferedImage parGray = ParallelImageProcessor.convertToGrayscale(testImage);
        System.out.println("  Grayscale:      " +
                (ImageUtils.imagesEqual(seqGray, parGray) ? "DA (identice)" : "NU (difera!)"));

        BufferedImage seqSobel = SequentialImageProcessor.applySobelEdgeDetection(testImage);
        BufferedImage parSobel = ParallelImageProcessor.applySobelEdgeDetection(testImage);
        System.out.println("  Sobel:          " +
                (ImageUtils.imagesEqual(seqSobel, parSobel) ? "DA (identice)" : "NU (difera!)"));

        System.out.println();
        System.out.println("=============================================================");
        System.out.println("  Program finalizat cu succes!");
        System.out.println("=============================================================");
    }

    @FunctionalInterface
    interface ImageProcessor {
        BufferedImage process(BufferedImage source);
    }

    private static void runBenchmark(String name, BufferedImage image,
                                     int numRuns, int numProcessors,
                                     ImageProcessor seqProcessor,
                                     ImageProcessor parProcessor) {
        seqProcessor.process(image);
        parProcessor.process(image);

        long totalSeq = 0;
        for (int i = 0; i < numRuns; i++) {
            long start = System.nanoTime();
            seqProcessor.process(image);
            long end = System.nanoTime();
            totalSeq += (end - start);
        }
        double avgSeqMs = (totalSeq / numRuns) / 1_000_000.0;

        long totalPar = 0;
        for (int i = 0; i < numRuns; i++) {
            long start = System.nanoTime();
            parProcessor.process(image);
            long end = System.nanoTime();
            totalPar += (end - start);
        }
        double avgParMs = (totalPar / numRuns) / 1_000_000.0;

        double speedup = avgSeqMs / avgParMs;
        double efficiency = speedup / numProcessors;
        double overheadMs = numProcessors * avgParMs - avgSeqMs;

        System.out.println();
        System.out.printf("  Timp secvential (Ts):     %10.2f ms%n", avgSeqMs);
        System.out.printf("  Timp paralel    (Tp):     %10.2f ms%n", avgParMs);
        System.out.printf("  Accelerare  (Ts/Tp):      %10.2fx%n", speedup);
        System.out.printf("  Eficienta   (S/p):        %10.2f  (ideal = 1.0)%n", efficiency);
        System.out.printf("  Cost suplimentar (To):    %10.2f ms%n", overheadMs);
        System.out.printf("  Procesoare (p):           %10d%n", numProcessors);

        if (speedup < 1.0) {
            System.out.println("  [!] Varianta paralela este mai LENTA decat cea secventiala!");
            System.out.println("      Costurile suplimentare de paralelizare depasesc beneficiile.");
        } else if (speedup > numProcessors) {
            System.out.println("  [!] Accelerare SUPRA-LINIARA detectata!");
            System.out.println("      Posibila cauza: efecte de cache (conform cursului).");
        }
        System.out.println();
    }
}
