package com.intifix.chatnotif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * chat-notif-service: chat en tiempo real (STOMP sobre WebSocket) y
 * notificaciones de IntiFix. Primer microservicio extraído del monolito.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatNotifApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatNotifApplication.class, args);
    }
}
