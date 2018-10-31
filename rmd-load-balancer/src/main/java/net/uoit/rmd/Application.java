package net.uoit.rmd;

import org.nustaq.kson.Kson;

import java.io.File;

public class Application {

    private static Kson kson = new Kson().map("config", RmdConfig.class);

    public static void main(String[] args) throws Exception {
        final RmdConfig config = (RmdConfig) kson.readObject(new File("config.kson"));

        final LoadBalancer loadBalancer = new LoadBalancer(config);
        final Server server = new Server(config, loadBalancer);

        loadBalancer.init();
        server.init();
    }
}
