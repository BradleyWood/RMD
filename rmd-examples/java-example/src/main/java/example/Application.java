package example;

import java.util.concurrent.ExecutionException;

import static net.uoit.rmd.Rmd.*;

public class Application {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("6! " + delegate(Functions::factorial, 6));

        for (int i = 0; i < 10; i++) {
            delegate((a) -> a * 2, i, System.out::println);
        }

        waitForAsyncJobs();
        System.exit(0);
    }
}
