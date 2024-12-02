package com.common.recovery.point.service;

import com.common.recovery.point.model.dto.*;
import com.common.recovery.point.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PointRecoveryService {
	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final Utils utils;

	public PointRecoveryService(WebClient webClient, ObjectMapper objectMapper, Utils utils) {
		this.webClient = webClient;
		this.objectMapper = objectMapper;
		this.utils = utils;
	}


    public Mono<PointUseFailMessage> transformMessage(String message) {
        try {
            PointUseFailMessage failMessage = objectMapper.readValue(message, PointUseFailMessage.class);
            log.info("Transformed message: {}", failMessage);
            return Mono.just(failMessage);
        } catch (Exception e) {
            log.error("Failed to transform message: {}", message, e);
            return Mono.error(new RuntimeException("Message transformation failed", e));
        }
    }

	public Mono<PointUseFailMessage> checkPointHistory(PointUseFailMessage pointUseFailMessage) {
		log.info("Checking point history for companyNo: {}", pointUseFailMessage.getCompanyNo());

		return Mono.defer(() -> {
			PointHistoryRequest pointHistoryRequest = PointHistoryRequest.builder()
					.companyNo(pointUseFailMessage.getCompanyNo())
					.userNo(pointUseFailMessage.getUserNo())
					.pointActionType("U")
					.pointGroupKey(pointUseFailMessage.getPointGroupKey())
					.insertTimestamp(utils.longToLocalDateTime(pointUseFailMessage.getUpdateTimestamp()))
					.build();

			log.info("Sending request: {}", pointHistoryRequest);

			return webClient.post()
					.uri("/point/history/exists")
					.bodyValue(pointHistoryRequest)
					.retrieve()
					.bodyToMono(new ParameterizedTypeReference<Response<Boolean>>() {
					})
					.doOnNext(response -> log.info("Received response: {}", response))
					.flatMap(response -> {
						if (response.getData() == null || response.getData()) {
							log.warn("Point history already exists for companyNo: {}", pointUseFailMessage.getCompanyNo());
							return Mono.error(new IllegalStateException("Point history already exists"));
						}
						return Mono.just(pointUseFailMessage);
					});
		}).onErrorResume(e -> {
			log.error("Failed to check point history for message: {}. Error: {}", pointUseFailMessage, e);
			return Mono.error(new RuntimeException("check point history failed", e));
		});
	}



    public Mono<PointChargeAndUseRequest> mapToRequest(PointUseFailMessage pointUseFailMessage) {
        PointChargeAndUseRequest pointChargeAndUseRequest = PointChargeAndUseRequest.builder()
                .companyNo(pointUseFailMessage.getCompanyNo())
                .userNo(pointUseFailMessage.getUserNo())
                .point(pointUseFailMessage.getPoint())
                .pointActionType(pointUseFailMessage.getPointActionType())
				.pointGroupKey(pointUseFailMessage.getPointGroupKey())
                .description("[KAFKA ACTION] 포인트 차감 실패 RETRY pointGroupKey=" + pointUseFailMessage.getPointGroupKey() + " updateTimestamp=" + utils.longToLocalDateTime(pointUseFailMessage.getUpdateTimestamp()))
				.currentTimeStamp(utils.longToLocalDateTime(pointUseFailMessage.getUpdateTimestamp()))
                .build();

        return Mono.just(pointChargeAndUseRequest);
    }

	public Mono<Boolean> retryPointUse(PointChargeAndUseRequest pointChargeAndUseRequest) {
		log.info("Retrying point update with message: {}", pointChargeAndUseRequest);

		return webClient.post()
				.uri("/point/use")
				.bodyValue(pointChargeAndUseRequest) // 실패 데이터 전송
				.retrieve()
				.bodyToMono(Response.class)
				.doOnNext(response -> log.info("Retrying point update response: {}", response))
				.map(response -> {
					// 응답 상태 코드와 메시지 검증
					if (response.getStatus() == 200 && "Success".equals(response.getMessage())) {
						log.info("Point update successful: {}", response.getData());
						return true; // 성공 여부 반환
					} else {
						log.warn("Point update failed with status: {}, message: {}", response.getStatus(), response.getMessage());
						return false; // 실패 시 false 반환
					}
				})
				.doOnError(error -> log.error("API Retry failed", error))
				.onErrorReturn(false); // 에러 발생 시 기본값 false 반환
	}
}
