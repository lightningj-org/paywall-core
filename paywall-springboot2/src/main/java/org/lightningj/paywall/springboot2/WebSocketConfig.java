package org.lightningj.paywall.springboot2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(getCheckSettlementWebSocketHandler(), "/paywall/socket/checkSettlement");
    }

    @Bean
    public WebSocketHandler getCheckSettlementWebSocketHandler() {
        return new CheckSettlementWebSocketHandler();
    }
}
