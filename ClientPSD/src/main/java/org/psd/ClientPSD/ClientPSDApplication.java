package org.psd.ClientPSD;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@SpringBootApplication
@EnableScheduling
public class ClientPSDApplication {

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        SpringApplication app = new SpringApplication(ClientPSDApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

}

