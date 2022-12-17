package org.psd.CloudPSD;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.security.Security;

@SpringBootApplication
@EnableJpaRepositories
public class CloudApp {
    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        SpringApplication app = new SpringApplication(CloudApp.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

}
