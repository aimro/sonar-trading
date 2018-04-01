package com.sonartrading.challenge.api.dto;

import lombok.Data;

@Data
public class OrderBookItem {

	private String oid;
	private String book;
	private String price;
	private String amount;
}
