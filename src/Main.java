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
        System.out.println("Numar de fire folosite - Varianta #1 (ForkJoin): " +
            ParallelImageProcessor.getParallelism());
        System.out.println("Numar de fire folosite - Varianta #2 (ThreadPool): " +
            ParallelImageProcessorV2.getParallelism());
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
            ParallelImageProcessor::applyGaussianBlur,
            ParallelImageProcessorV2::applyGaussianBlur);

        System.out.println("-------------------------------------------------------------");
        System.out.println("  TESTUL 2: Conversie la Grayscale");
        System.out.println("-------------------------------------------------------------");

        runBenchmark("Grayscale", testImage, numRuns, numProcessors,
                SequentialImageProcessor::convertToGrayscale,
            ParallelImageProcessor::convertToGrayscale,
            ParallelImageProcessorV2::convertToGrayscale);

        System.out.println("-------------------------------------------------------------");
        System.out.println("  TESTUL 3: Detectie de margini (Sobel)");
        System.out.println("-------------------------------------------------------------");

        runBenchmark("Sobel", testImage, numRuns, numProcessors,
                SequentialImageProcessor::applySobelEdgeDetection,
            ParallelImageProcessor::applySobelEdgeDetection,
            ParallelImageProcessorV2::applySobelEdgeDetection);

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
                    outputDir + "/blur_paralel_v1.png", "png");
                ImageUtils.saveImage(
                    ParallelImageProcessorV2.applyGaussianBlur(testImage),
                    outputDir + "/blur_paralel_v2.png", "png");
            ImageUtils.saveImage(
                    SequentialImageProcessor.convertToGrayscale(testImage),
                    outputDir + "/grayscale_secvential.png", "png");
            ImageUtils.saveImage(
                    ParallelImageProcessor.convertToGrayscale(testImage),
                    outputDir + "/grayscale_paralel_v1.png", "png");
                ImageUtils.saveImage(
                    ParallelImageProcessorV2.convertToGrayscale(testImage),
                    outputDir + "/grayscale_paralel_v2.png", "png");
            ImageUtils.saveImage(
                    SequentialImageProcessor.applySobelEdgeDetection(testImage),
                    outputDir + "/sobel_secvential.png", "png");
            ImageUtils.saveImage(
                    ParallelImageProcessor.applySobelEdgeDetection(testImage),
                    outputDir + "/sobel_paralel_v1.png", "png");
                ImageUtils.saveImage(
                    ParallelImageProcessorV2.applySobelEdgeDetection(testImage),
                    outputDir + "/sobel_paralel_v2.png", "png");

            System.out.println("Rezultatele au fost salvate in directorul '" + outputDir + "/'");
        } catch (Exception e) {
            System.out.println("Eroare la salvarea rezultatelor: " + e.getMessage());
        }

        System.out.println();
        System.out.println("-------------------------------------------------------------");
        System.out.println("  Validare: Rezultatele secventiale si paralele sunt identice?");
        System.out.println("-------------------------------------------------------------");

        BufferedImage seqBlur = SequentialImageProcessor.applyGaussianBlur(testImage);
        BufferedImage parBlurV1 = ParallelImageProcessor.applyGaussianBlur(testImage);
        BufferedImage parBlurV2 = ParallelImageProcessorV2.applyGaussianBlur(testImage);
        System.out.println("  Gaussian Blur - Varianta #1:  " +
            (ImageUtils.imagesEqual(seqBlur, parBlurV1) ? "DA (identice)" : "NU (difera!)"));
        System.out.println("  Gaussian Blur - Varianta #2:  " +
            (ImageUtils.imagesEqual(seqBlur, parBlurV2) ? "DA (identice)" : "NU (difera!)"));

        BufferedImage seqGray = SequentialImageProcessor.convertToGrayscale(testImage);
        BufferedImage parGrayV1 = ParallelImageProcessor.convertToGrayscale(testImage);
        BufferedImage parGrayV2 = ParallelImageProcessorV2.convertToGrayscale(testImage);
        System.out.println("  Grayscale - Varianta #1:      " +
            (ImageUtils.imagesEqual(seqGray, parGrayV1) ? "DA (identice)" : "NU (difera!)"));
        System.out.println("  Grayscale - Varianta #2:      " +
            (ImageUtils.imagesEqual(seqGray, parGrayV2) ? "DA (identice)" : "NU (difera!)"));

        BufferedImage seqSobel = SequentialImageProcessor.applySobelEdgeDetection(testImage);
        BufferedImage parSobelV1 = ParallelImageProcessor.applySobelEdgeDetection(testImage);
        BufferedImage parSobelV2 = ParallelImageProcessorV2.applySobelEdgeDetection(testImage);
        System.out.println("  Sobel - Varianta #1:          " +
            (ImageUtils.imagesEqual(seqSobel, parSobelV1) ? "DA (identice)" : "NU (difera!)"));
        System.out.println("  Sobel - Varianta #2:          " +
            (ImageUtils.imagesEqual(seqSobel, parSobelV2) ? "DA (identice)" : "NU (difera!)"));

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
                                     ImageProcessor parProcessorV1,
                                     ImageProcessor parProcessorV2) {
        seqProcessor.process(image);
        parProcessorV1.process(image);
        parProcessorV2.process(image);

        long totalSeq = 0;
        for (int i = 0; i < numRuns; i++) {
            long start = System.nanoTime();
            seqProcessor.process(image);
            long end = System.nanoTime();
            totalSeq += (end - start);
        }
        double avgSeqMs = (totalSeq / numRuns) / 1_000_000.0;

        long totalParV1 = 0;
        for (int i = 0; i < numRuns; i++) {
            long start = System.nanoTime();
            parProcessorV1.process(image);
            long end = System.nanoTime();
            totalParV1 += (end - start);
        }
        double avgParV1Ms = (totalParV1 / numRuns) / 1_000_000.0;

        long totalParV2 = 0;
        for (int i = 0; i < numRuns; i++) {
            long start = System.nanoTime();
            parProcessorV2.process(image);
            long end = System.nanoTime();
            totalParV2 += (end - start);
        }
        double avgParV2Ms = (totalParV2 / numRuns) / 1_000_000.0;

        double speedupV1 = avgSeqMs / avgParV1Ms;
        double speedupV2 = avgSeqMs / avgParV2Ms;
        double efficiencyV1 = speedupV1 / numProcessors;
        double efficiencyV2 = speedupV2 / numProcessors;
        double overheadV1Ms = numProcessors * avgParV1Ms - avgSeqMs;
        double overheadV2Ms = numProcessors * avgParV2Ms - avgSeqMs;

        System.out.println();
        System.out.println("  Operatie: " + name);
        System.out.printf("  Timp secvential (Ts):     %10.2f ms%n", avgSeqMs);
        System.out.printf("  Timp paralel V1 (Tp1):    %10.2f ms%n", avgParV1Ms);
        System.out.printf("  Timp paralel V2 (Tp2):    %10.2f ms%n", avgParV2Ms);
        System.out.printf("  Accelerare V1 (Ts/Tp1):   %10.2fx%n", speedupV1);
        System.out.printf("  Accelerare V2 (Ts/Tp2):   %10.2fx%n", speedupV2);
        System.out.printf("  Eficienta V1 (S1/p):      %10.2f  (ideal = 1.0)%n", efficiencyV1);
        System.out.printf("  Eficienta V2 (S2/p):      %10.2f  (ideal = 1.0)%n", efficiencyV2);
        System.out.printf("  Cost supl. V1 (To1):      %10.2f ms%n", overheadV1Ms);
        System.out.printf("  Cost supl. V2 (To2):      %10.2f ms%n", overheadV2Ms);
        System.out.printf("  Procesoare (p):           %10d%n", numProcessors);

        if (speedupV1 < 1.0) {
            System.out.println("  [!] Varianta paralela #1 este mai LENTA decat cea secventiala!");
            System.out.println("      Costurile suplimentare de paralelizare depasesc beneficiile.");
        } else if (speedupV1 > numProcessors) {
            System.out.println("  [!] Accelerare SUPRA-LINIARA detectata pentru varianta #1!");
            System.out.println("      Posibila cauza: efecte de cache (conform cursului).");
        }

        if (speedupV2 < 1.0) {
            System.out.println("  [!] Varianta paralela #2 este mai LENTA decat cea secventiala!");
            System.out.println("      Costurile suplimentare de paralelizare depasesc beneficiile.");
        } else if (speedupV2 > numProcessors) {
            System.out.println("  [!] Accelerare SUPRA-LINIARA detectata pentru varianta #2!");
            System.out.println("      Posibila cauza: efecte de cache (conform cursului).");
        }
        System.out.println();
    }
}
