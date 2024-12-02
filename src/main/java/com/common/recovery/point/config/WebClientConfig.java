package com.common.recovery.point.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create(ConnectionProvider.builder("custom")
                .maxConnections(100) // 최대 연결 수
                .pendingAcquireMaxCount(1000) // 대기 중 연결 요청 수
                .build())
            .responseTimeout(Duration.ofSeconds(60)) // 응답 타임아웃 (확장 가능)
            .doOnConnected(conn ->
                conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(60)) // 읽기 타임아웃
                    .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(60)) // 쓰기 타임아웃
            );

        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .defaultHeader("Content-Type", "application/json") // JSON 요청 기본 설정
                .defaultHeader("Accept", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
