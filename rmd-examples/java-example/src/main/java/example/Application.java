package example;

import java.util.concurrent.ExecutionException;

import static net.uoit.rmd.Rmd.*;

public class Application {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("6! " + delegate(Functions::factorial, 6));

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            delegate((a) -> a * 2, i, n -> System.out.println(finalI + " * 2 = " + n));
        }

        waitForAsyncJobs();
        System.exit(0);
    }
}
