package com.sonartrading.challenge.data.model;

public enum MakerSide {
	SELL, BUY;

	public static MakerSide getByString(String side) {

		if ("buy".equals(side)) {
			return BUY;
		} else if ("sell".equals(side)) {
			return SELL;
		}

		return null;
	}
}