package com.intifix.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ai-service: asistente de IntiFix (GPT-4o con Spring AI) que recomienda
 * técnicos y estima presupuestos. Segundo microservicio extraído del monolito.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
