package com.sonartrading.challenge.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBook {
	
	private List<OrderBookItem> asks;
	private List<OrderBookItem> bids;

	private String updated_at;
	private Long sequence;
}