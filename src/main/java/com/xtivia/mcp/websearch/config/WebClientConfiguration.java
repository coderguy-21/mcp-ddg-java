package com.xtivia.mcp.websearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient configuration with proper settings for web scraping.
 */
@Configuration
public class WebClientConfiguration {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure Netty HTTP client with redirect following
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .followRedirect(true);  // Enable automatic redirect following
        
        // Configure exchange strategies with increased buffer size for large HTML responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                ClientCodecConfigurer.ClientDefaultCodecs codecs = configurer.defaultCodecs();
                codecs.maxInMemorySize(16 * 1024 * 1024); // 16MB buffer for large HTML pages
                codecs.enableLoggingRequestDetails(true);  // Enable request logging
            })
            .build();
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies);
    }
}
