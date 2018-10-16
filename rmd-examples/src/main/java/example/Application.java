package example;

import static net.uoit.rmd.Rmd.*;

public class Application {

    public static void main(String[] args) {
        System.out.println("6! " + delegate(Functions::factorial, 6));

        System.exit(0);
    }
}
