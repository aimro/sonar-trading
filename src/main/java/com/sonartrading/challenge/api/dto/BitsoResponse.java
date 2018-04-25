package com.sonartrading.challenge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BitsoResponse<T> {

	private Boolean success;
	private T payload;
}
