package com.common.recovery.point.service;

import com.common.recovery.point.model.dto.PointUseFailMessage;
import com.common.recovery.point.util.Utils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KafkaConsumerServiceTest {

	@Autowired
	private PointRecoveryService pointRecoveryService;

	@Autowired
	private KafkaConsumerService kafkaConsumerService;

	@Autowired
	private Utils utils;

	@Test
	void testIsMessageRecent() {
		PointUseFailMessage message = PointUseFailMessage.builder()
				.companyNo(35)
				.userNo(34)
				.updateTimestamp(System.currentTimeMillis())
				.pointGroupKey("test-key")
				.pointActionType("U")
				.description("Test")
				.build();

		boolean result = kafkaConsumerService.isMessageRecent(message);

	}

	@Test
	void testConsumeFailedMessage() {

		Acknowledgment acknowledgment = Mockito.mock(Acknowledgment.class);

		String dateTimeString = "2024-12-02 18:29:11.329903";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

        long updateTimestamp = utils.localDateTimeToLong(LocalDateTime.parse(dateTimeString, formatter));
		
		// ConsumerRecord 생성
		ConsumerRecord<String, String> record = new ConsumerRecord<>(
				"point-system", 0, 0L, null,
				"{\"companyNo\":35,\"userNo\":33,\"updateTimestamp\":" + updateTimestamp + ",\"pointGroupKey\":\"e8d1a8d8cd874bbfa778\",\"pointActionType\":\"U\",\"description\":\"사용자 33 동시 100 포인트 차감 테스트\",\"point\":100}"
		);


		kafkaConsumerService.consumeFailedMessage(record, acknowledgment);

		System.out.println();
	}
}
