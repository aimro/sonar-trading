package com.sonartrading.challenge.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsocketMessage<T> {

	private String type;
	private String book;
	private Long sequence;
	private T payload;
}
