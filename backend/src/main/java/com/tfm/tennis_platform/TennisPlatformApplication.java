package com.tfm.tennis_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class TennisPlatformApplication {

	public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TennisPlatformApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080"); // Default to 8080 if not set
        System.out.println("Application started at: http://localhost:" + port + "------------------------------------------------");

    }

}
