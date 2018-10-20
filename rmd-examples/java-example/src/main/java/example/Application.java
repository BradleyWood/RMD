package example;

import static net.uoit.rmd.Rmd.*;

public class Application {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("6! " + delegate(Functions::factorial, 6));

        for (int i = 0; i < 10; i++) {
//            System.out.println("6! " + delegate(Functions::factorial, 6));
            int finalI = i;
            delegate(Functions::factorial, finalI, gg -> {
                System.out.println("Callback " + gg);
            });
        }

        Thread.sleep(10000);
        System.exit(0);
    }
}
