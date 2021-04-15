import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {

        // path to image
        String path = "./images/input_images/head.jpg";

        // number of generations
        int numberOfEpoch = 6000;

        EvolutionaryAlgorithm image_1 = new EvolutionaryAlgorithm(path, numberOfEpoch);

        // start algorithm
        image_1.startAlgorithm();
    }
}
