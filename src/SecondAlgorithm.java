import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class SecondAlgorithm {

    final static int WIDHT = 512;
    final static int HEIGHT = 512;

    final static int NUMBER_OF_BEST = 5;
    final static int NUMBER_OF_CHILDREN = 10;
    final static int NUMBER_OF_EPOCH = 5000;
    final static int NUMBER_OF_BASE_PIC = 37;
    private static BufferedImage inputImage;

    final static int SAVE = 100;

    // Do not forget to write the path to image
    final static String path = "./images/input_images/telegram.jpg";

    // Path for save image
    private final static String savePath = "./images/output_image/2nd_algorithm/";

    final static String base = "./base/15/";

    private static ArrayList<BufferedImage> baseImages = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ArrayList<BufferedImage> theBestGen;
        ArrayList<BufferedImage> images;

        for (int i = 1; i <= NUMBER_OF_BASE_PIC; i++) {
            baseImages.add(ImageIO.read(new File(base + i + ".png")));
        }

        inputImage = ImageIO.read(new File(path));

        long[][] inputImagePixels = imageToArray(inputImage);


        theBestGen = createFirstGeneration();

        for (int i = 0; i < NUMBER_OF_EPOCH; i++) {
            if (i % 5 == 0) {
                System.out.format("EPOCH: %s\n", i);
            }
            if (i % SAVE == 0) {
                savePictures(theBestGen, i);
            }
            images = createNewGeneration(theBestGen);
            images.addAll(theBestGen);
            theBestGen = chooseTheBestFromCreatedImages(images, inputImagePixels);
        }

        savePictures(theBestGen, NUMBER_OF_EPOCH);


    }

    private static long[][] imageToArray(BufferedImage image) {
        long[][] pixels = new long[image.getWidth()][image.getHeight()];

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                pixels[i][j] = image.getRGB(i, j);
            }
        }
        return pixels;
    }

    private static BufferedImage getWhite() {
        BufferedImage img = new BufferedImage(WIDHT, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDHT, HEIGHT);
        g2d.dispose();
        return img;
    }

    private static void mutate(BufferedImage image) {
        Random random = new Random();

        for (int i = 0; i < 1; i++) {
            int x = random.nextInt(WIDHT);
            int y = random.nextInt(HEIGHT);

            Graphics2D g2d = image.createGraphics();

            g2d.drawImage(baseImages.get(random.nextInt(NUMBER_OF_BASE_PIC)), x, y, null);


            g2d.dispose();
        }

    }

    private static ArrayList<BufferedImage> createFirstGeneration() {
        ArrayList<BufferedImage> imageArrayList = new ArrayList<>();
        BufferedImage img;

        for (int i = 0; i < NUMBER_OF_BEST; i++) {
            img = getWhite();
            mutate(img);
            imageArrayList.add(img);
        }

        return imageArrayList;
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private static ArrayList<BufferedImage> createNewGeneration(ArrayList<BufferedImage> oldGeneration) {
        ArrayList<BufferedImage> imageArrayList = new ArrayList<>();
        BufferedImage mutated;

        for (BufferedImage img : oldGeneration) {


            for (int i = 0; i < NUMBER_OF_CHILDREN; ++i) {
                mutated = deepCopy(img);
                mutate(mutated);
                imageArrayList.add(mutated);
            }
        }

        return imageArrayList;
    }

    private static void savePictures(ArrayList<BufferedImage> images, int i) throws IOException {
        BufferedImage image;

        image = images.get(0);

        File createDirectory = new File(savePath);
        createDirectory.mkdirs();

        String imageName = "epoch_" + i + ".png";

        ImageIO.write(image, "PNG",
                new File(savePath + imageName));

    }

    private static ArrayList<BufferedImage> chooseTheBestFromCreatedImages(ArrayList<BufferedImage> images,
                                                                           long[][] inputImagePixelsSum) {

        HashMap<Integer, Double> bestChoiceHashMap = new HashMap<>();
        ArrayList<BufferedImage> bestImages = new ArrayList<>();

        // key - number of image and value - average error
        HashMap<Integer, Double> imageAndErrorHashMap = new HashMap<Integer, Double>();
        ArrayList<Double> errorList = new ArrayList<>();

        final int size = images.size();

        for (int k = 0; k < size; k++) {
            long[][] createdImagePixels = imageToArray(images.get(k));
            double averageError = 0;
            for (int i = 0; i < WIDHT; i++) {
                for (int j = 0; j < HEIGHT; j++) {
                    averageError = Math.pow(Math.abs(inputImagePixelsSum[i][j] - createdImagePixels[i][j]), 2) + averageError;
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
