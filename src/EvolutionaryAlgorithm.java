import java.awt.*;
import java.io.File;
import java.util.Random;
import java.util.HashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

public class EvolutionaryAlgorithm {

    // The size of output images
    private final static int WIDHT = 512;
    private final static int HEIGHT = 512;

    // Path to save images
    private final static String savePath = "./images/output_image/";

    // How many images we choose from the population for next generation
    private final static int NUMBER_OF_BEST = 5;
    // How many images created by 1 parent image
    private final static int NUMBER_OF_CHILDREN = 10;
    // How many figures we draw for one time
    private final static int NUMBER_OF_FIGURE = 5;

    // Period through generations when we save the result
    private final static int SAVE_NUMBER = 100;

    // For parallelism of the program (for more quickly execution)
    private final static int CORES = Runtime.getRuntime().availableProcessors();

    // input image
    private static BufferedImage inputImage;

    // path to input image and count of generations
    private final String path;
    private final int numberOfEpoch;

    EvolutionaryAlgorithm(String path, int numberOfEpoch) {
        this.path = path;
        this.numberOfEpoch = numberOfEpoch;
    }

    // this method will start the execution of algorithm
    public void startAlgorithm() throws IOException, InterruptedException {

        ArrayList<BufferedImage> images;
        ArrayList<BufferedImage> theBestGen;
        inputImage = ImageIO.read(new File(path));
        long[][] inputImagePixels = imageToArray(inputImage);


        theBestGen = createFirstGeneration();
        System.out.println(theBestGen.size());

        System.out.format("TOTAL NUMBER OF EPOCH: %s\n", numberOfEpoch);
        for (int i = 0; i < numberOfEpoch; i++) {
            if (i % 5 == 0) {
                System.out.format("EPOCH: %s\n", i);
            }
            if (i % SAVE_NUMBER == 0) {
                savePictures(theBestGen, i);
            }
            images = createNewGeneration(theBestGen);
            images.addAll(theBestGen);
            theBestGen = selectionImages(images, inputImagePixels);
        }

        System.out.println(theBestGen.size());
        savePictures(theBestGen, numberOfEpoch);


    }

    // this method convert image to array
    private static long[][] imageToArray(BufferedImage image) {
        long[][] pixels = new long[image.getWidth()][image.getHeight()];

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                pixels[i][j] = image.getRGB(i, j);
            }
        }
        return pixels;
    }

    // create white images for 1st generation
    private static BufferedImage getWhite() {
        BufferedImage img = new BufferedImage(WIDHT, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDHT, HEIGHT);
        g2d.dispose();
        return img;
    }

    // mutate our image and add new figures
    private static BufferedImage mutate(BufferedImage image) {
        BufferedImage img = deepCopy(image);
        Random random = new Random();

        for (int i = 0; i < NUMBER_OF_FIGURE; i++) {
            int x = random.nextInt(WIDHT);
            int y = random.nextInt(HEIGHT);

            int width = random.nextInt(100) + 1;
            int height = random.nextInt(100) + 1;

            Color color = new Color(inputImage.getRGB(x, y));

            Graphics2D g2d = img.createGraphics();

            int opacity = random.nextInt(255);

            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() - opacity));

//            g2d.setColor(new Color(random.nextInt(255),
//                    random.nextInt(255), random.nextInt(255), opacity));

            g2d.fillRoundRect(x, y, width, height, 15, 15);

            g2d.dispose();
        }


        return img;
    }

    // method to create first 5 (by default=NUMBER_OF_BEST=5) images
    private static ArrayList<BufferedImage> createFirstGeneration() {
        ArrayList<BufferedImage> imageArrayList = new ArrayList<>();
        BufferedImage img, white;
        white = getWhite();

        for (int i = 0; i < NUMBER_OF_BEST; i++) {
            img = mutate(white);
            imageArrayList.add(img);
        }

        return imageArrayList;
    }

    // copy image to work with it without influence on previous image
    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    // create new generation from previous images
    private static ArrayList<BufferedImage> createNewGeneration(ArrayList<BufferedImage> oldGeneration)
            throws InterruptedException {
        ArrayList<BufferedImage> result = new ArrayList<>(oldGeneration.size() * NUMBER_OF_CHILDREN);

        final ExecutorService pool = Executors.newFixedThreadPool(CORES);
        final ExecutorCompletionService<BufferedImage> completionService = new ExecutorCompletionService<>(pool);
        for (final BufferedImage img : oldGeneration) {
            for (int i = 0; i < NUMBER_OF_CHILDREN; i++) {
                completionService.submit(() -> mutate(img));
            }
        }

        for (int i = 0; i < oldGeneration.size() * NUMBER_OF_CHILDREN; ++i) {
            try {
                result.add(completionService.take().get());
            } catch (ExecutionException e) {
                System.out.println("Error while mutating" + e.getCause());
            }
        }

        return result;
    }

    // save the result
    private static void savePictures(ArrayList<BufferedImage> images, int i) throws IOException {
        BufferedImage image;

        image = images.get(0);

        File createDirectory = new File(savePath);
        createDirectory.mkdirs();

        String imageName = "epoch_" + i + ".png";

        ImageIO.write(image, "PNG",
                new File(savePath + imageName));

    }

    // selection among the population
    private static ArrayList<BufferedImage> selectionImages(ArrayList<BufferedImage> images,
                                                            long[][] inputImagePixelsSum) {

        HashMap<Integer, Double> bestChoiceHashMap = new HashMap<>();
        ArrayList<BufferedImage> bestImages = new ArrayList<>();

        // key - number of image and value - average error
        HashMap<Integer, Double> imageAndErrorHashMap = new HashMap<Integer, Double>();
        ArrayList<Double> errorList = new ArrayList<>();

        for (int k = 0; k < images.size(); k++) {
            long[][] createdImagePixels = imageToArray(images.get(k));
            double averageError = 0;
            for (int i = 0; i < WIDHT; i++) {
                for (int j = 0; j < HEIGHT; j++) {

                    long createdPixelSum = createdImagePixels[i][j];

                    averageError = Math.pow(Math.abs(inputImagePixelsSum[i][j] - createdPixelSum), 2) + averageError;
                }
            }
            averageError = Math.sqrt(averageError) / (WIDHT * HEIGHT);
            imageAndErrorHashMap.put(k, averageError);
            errorList.add(averageError);
        }

        Collections.sort(errorList);

        for (int i = 0; i < NUMBER_OF_BEST; i++) {
            for (Integer key :
                    imageAndErrorHashMap.keySet()) {
                if (imageAndErrorHashMap.get(key).equals(errorList.get(i))) {
                    bestChoiceHashMap.put(key, errorList.get(i));
                    break;
                }
            }
        }
        for (Integer key :
                bestChoiceHashMap.keySet()) {
            bestImages.add(images.get(key));
        }
        System.out.println("Min error: " + errorList.get(0));
        return bestImages;
    }

}
