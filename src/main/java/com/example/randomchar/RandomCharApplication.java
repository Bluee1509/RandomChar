package com.example.randomchar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RandomCharApplication {

    public static void main(String[] args) {
        SpringApplication.run(RandomCharApplication.class, args);
    }

}
