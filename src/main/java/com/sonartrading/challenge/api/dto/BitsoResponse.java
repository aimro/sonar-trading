package com.sonartrading.challenge.api.dto;

import lombok.Data;

@Data
public class BitsoResponse<T> {

	private Boolean success;
	private T payload;
}
