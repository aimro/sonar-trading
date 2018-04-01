package com.sonartrading.challenge.api.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class TradeBook {
	
	private String book;
	private String created_at;
	private String amount;
	
	@SerializedName("maker_side")
	private String markerSide;
	
	private String price;
	private Long tid;
}