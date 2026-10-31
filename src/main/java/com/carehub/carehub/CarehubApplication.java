package com.carehub.carehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarehubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarehubApplication.class, args);
    }

}
