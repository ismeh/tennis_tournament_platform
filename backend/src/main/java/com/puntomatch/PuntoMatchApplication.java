package com.puntomatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PuntoMatchApplication {

	 public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PuntoMatchApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080"); // Default to 8080 if not set
        System.out.println("Application started at: http://localhost:" + port + " ----------------------------------------------------------");
    }
}
