package com.sonartrading.challenge.data.model;

import java.time.OffsetDateTime;

import lombok.Value;

@Value
public class Trade {
	private Long tradeID;
	private MakerSide makerSide;
	private OffsetDateTime date;
	private Double amount;
	private Double price;
}
