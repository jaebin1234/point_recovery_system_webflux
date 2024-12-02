package com.common.recovery.point.service;

import com.common.recovery.point.model.dto.PointChargeAndUseRequest;
import com.common.recovery.point.model.dto.PointUseFailMessage;
import com.common.recovery.point.util.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointRecoveryServiceTest {

    @Autowired
    private PointRecoveryService pointRecoveryService;

    @Autowired
    private Utils utils;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Test
    void testTransformMessage_Success() throws Exception {
        // 실제 JSON 메시지
        String message = "{\"companyNo\":35,\"userNo\":34,\"updateTimestamp\":1701234567890,\"pointGroupKey\":\"test-key\",\"pointActionType\":\"U\",\"description\":\"Test\"}";

        // 기대하는 결과 객체
        PointUseFailMessage expected =
                PointUseFailMessage.builder()
                        .companyNo(35)
                        .userNo(34)
                        .updateTimestamp(1701234567890L)
                        .pointGroupKey("test-key")
                        .pointActionType("U")
                        .description("Test")
                        .build();

        // 실제 테스트
        Mono<PointUseFailMessage> result = pointRecoveryService.transformMessage(message);

        // 결과 검증
        StepVerifier.create(result)
                .expectNextMatches(actual ->
                        actual.getCompanyNo().equals(expected.getCompanyNo()) &&
                        actual.getUserNo().equals(expected.getUserNo()) &&
                        actual.getPointGroupKey().equals(expected.getPointGroupKey()))
                .verifyComplete();
    }

    @Test
    void testTransformMessage_Failure() throws Exception {
        // 잘못된 JSON 메시지
        String message = "invalid-message";

        // 실제 테스트
        Mono<PointUseFailMessage> result = pointRecoveryService.transformMessage(message);

        // 결과 검증
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }


    @Test
    void testCheckPointHistory_Exists() {
        String dateTimeString = "2024-11-27 18:17:39.929273";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        long updateTimestamp = utils.localDateTimeToLong(LocalDateTime.parse(dateTimeString, formatter));

        // 실제 메시지 객체
        PointUseFailMessage message = PointUseFailMessage.builder()
                .companyNo(35)
                .userNo(34)
                .updateTimestamp(updateTimestamp)
//                .pointGroupKey("test-key")
                .pointGroupKey("7d2e8e4908fb4996aa08")
                .pointActionType("U")
                .build();

        // 실제 테스트
        Mono<PointUseFailMessage> result = pointRecoveryService.checkPointHistory(message);

        System.out.println();
    }

    @Test
    void testRetryPointUse_Success() {

        String dateTimeString = "2024-11-27 18:17:39.929273";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        // 실제 요청 객체
        PointChargeAndUseRequest request = PointChargeAndUseRequest.builder()
                .companyNo(35)
                .userNo(33)
                .point(100)
                .pointActionType("U")
                .description("[KAFKA ACTION] " + "사용자 33 동시 100 포인트 차감 테스트")
                .pointGroupKey("7d2e8e4908fb4996aa07")
                .currentTimeStamp(LocalDateTime.parse(dateTimeString, formatter))
                .build();

        // 실제 테스트
        Mono<Boolean> result = pointRecoveryService.retryPointUse(request);

        System.out.println();
//        // 결과 검증
//        StepVerifier.create(result)
//                .expectNext("Success")
//                .verifyComplete();
    }
}
