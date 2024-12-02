package com.common.recovery.point.service;

import com.common.recovery.point.model.dto.PointUseFailMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

	private final PointRecoveryService pointRecoveryService;
	private final ObjectMapper objectMapper;

	@KafkaListener(
			topics = "point-system",
			groupId = "recovery-group",
			concurrency = "1",
			containerFactory = "kafkaListenerContainerFactory"
	)
	public void consumeFailedMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
		String message = record.value();
		log.info("Received failed message: {}", message);

		Mono.just(message)
				.flatMap(pointRecoveryService::transformMessage) // 메시지 변환 (deserialize 포함)
				.filter(this::isMessageRecent) // 최근 24시간 이내 메시지만 처리
				.flatMap(pointRecoveryService::checkPointHistory) // 1. 히스토리 확인
				.flatMap(pointRecoveryService::mapToRequest) // 2. 요청 매핑
				.flatMap(pointRecoveryService::retryPointUse) // 3. 포인트 차감 API 호출
				.doOnSuccess(response -> {
					log.info("Retry successful: {}", response);
					if (acknowledgment != null) {
						commitOffset(acknowledgment);
					} else {
						log.warn("Acknowledgment is null. Skipping commitOffset.");
					}
				})
				.doOnError(error ->
						log.error("Retry failed: {}", error.getMessage())
				)
				.onErrorResume(error -> {
					log.warn("Skipping message due to error: {}", error.getMessage());
					return Mono.empty();
				})
				.subscribeOn(Schedulers.boundedElastic())
				.block();
	}


	public boolean isMessageRecent(PointUseFailMessage message) {
		long now = System.currentTimeMillis();
		long messageTimestamp = message.getUpdateTimestamp(); // 메시지의 타임스탬프 (밀리초 단위)

		boolean isRecent = (now - messageTimestamp) <= TimeUnit.DAYS.toMillis(1); // 24시간 이내 체크
		if (!isRecent) {
			log.info("Skipping old message with timestamp: {}", messageTimestamp);
		}
		return isRecent;
	}


	private void commitOffset(Acknowledgment acknowledgment) {
		try {
			acknowledgment.acknowledge();
			log.info("Offset committed successfully.");
		} catch (Exception e) {
			log.error("Failed to commit offset.", e);
		}
	}

}

