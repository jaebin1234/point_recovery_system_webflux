package com.common.recovery.point.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointHistoryRequest {

	@NotNull
	private Integer companyNo;

	@NotNull
	private Integer userNo;

	@NotNull
	private String pointActionType;

	private Integer point;

	@NotNull
	private String pointGroupKey;

	@NotNull
	private LocalDateTime insertTimestamp;

}
