package com.sonartrading.challenge.ws.dto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiffOrder {

	@SerializedName("d")
	private Long timestamp;

	@SerializedName("r")
	private String rate;

	// 0 indicates buy 1 indicates sell
	@SerializedName("t")
	private Short orderType;
	
	@SerializedName("a")
	private String amount;

	@SerializedName("v")
	private String value;

	@SerializedName("o")
	private String orderID;
	
	private long sequence;
}
