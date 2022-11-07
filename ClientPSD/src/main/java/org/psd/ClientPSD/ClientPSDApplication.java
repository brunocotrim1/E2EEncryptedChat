package org.psd.ClientPSD;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClientPSDApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ClientPSDApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

}

