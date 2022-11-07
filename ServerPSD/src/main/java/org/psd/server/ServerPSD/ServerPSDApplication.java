package org.psd.server.ServerPSD;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class ServerPSDApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ServerPSDApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

}

