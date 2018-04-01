package com.sonartrading.challenge.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

	private String action;
	private String reponse;
	private Long time;
	private String type;
}
