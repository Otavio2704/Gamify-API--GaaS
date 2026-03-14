                                                                                                                                                                                                                                                                                                                                                                                                     package com.gamifyapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Classe principal da GamifyAPI.
 * Gamificação como Serviço (GaaS) — API REST multi-tenant.
 */
@SpringBootApplication
@EnableAsync
public class GamifyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamifyApiApplication.class, args);
    }
}
