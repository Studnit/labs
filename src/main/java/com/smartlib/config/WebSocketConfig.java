package com.smartlib.config;

import com.smartlib.ws.RealtimeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket

public class WebSocketConfig implements WebSocketConfigurer {
    private final RealtimeHandler handler;


    public WebSocketConfig(RealtimeHandler handler) {
        this.handler = handler;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Same-origin only (Spring default); session cookie authenticates the handshake via Spring Security.
        registry.addHandler(handler, "/ws");
    }

    @Bean
    ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
        c.setMaxTextMessageBufferSize(8 * 1024);
        c.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return c;
    }
}
