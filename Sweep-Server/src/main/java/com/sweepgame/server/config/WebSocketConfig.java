package com.sweepgame.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
// TODO: Enable when WebSocket handlers are implemented
// @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // TODO: Configure message broker
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO: Register STOMP endpoints
    }
}
