package com.sonartrading.challenge.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscribe {

	private final String action = "subscribe";
	private String book;
	private String type;
}
