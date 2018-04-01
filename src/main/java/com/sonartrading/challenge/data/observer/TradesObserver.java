package com.sonartrading.challenge.data.observer;

import java.util.List;

import com.sonartrading.challenge.data.model.Trade;

public interface TradesObserver {

	void tradesUpdated(List<Trade> trades);
	ObserverType getObserverType();
		
	public enum ObserverType {
		UI, LOGIC
	}
}
