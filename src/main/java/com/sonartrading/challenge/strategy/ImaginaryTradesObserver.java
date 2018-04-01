package com.sonartrading.challenge.strategy;

import com.sonartrading.challenge.data.model.Trade;

public interface ImaginaryTradesObserver {

	void newImaginaryTrade(Trade trade);
}
