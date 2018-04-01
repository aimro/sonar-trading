package com.sonartrading.challenge.api.dto;

import java.util.List;

import lombok.Data;

@Data
public class OrderBook {
	
	private List<OrderBookItem> asks;
	private List<OrderBookItem> bids;

	private String updated_at;
	private Long sequence;
}