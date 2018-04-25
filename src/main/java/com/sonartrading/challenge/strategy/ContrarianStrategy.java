package com.sonartrading.challenge.strategy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Trade;
import com.sonartrading.challenge.data.observer.TradesObserver;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContrarianStrategy implements TradesObserver {

	private final List<Trade> recentTrades = new ArrayList<>();
	private final List<Trade> previousTrades = new ArrayList<>();

	private final List<ImaginaryTradesObserver> tradesObservers = new ArrayList<>();

	private int upTicks = 0;
	private int downTicks = 0;

	private double lastPrice = Double.NaN;

	private long imaginaryIndex = 0;

	public void addImaginaryTradesObserver(ImaginaryTradesObserver tradesObserver) {
		tradesObservers.add(tradesObserver);
	}

	@Override
	public void tradesUpdated(List<Trade> trades) {

		// process just new trades removing previous ones
		recentTrades.addAll(trades);
		recentTrades.removeAll(previousTrades);

		if (recentTrades.isEmpty()) {
			log.debug("There is no new trades");
			return;
		}

		// prepare collection with trades processed for next update
		previousTrades.clear();
		previousTrades.addAll(trades);

		// we are assuming the list is sorted by date (in desc order)
		for (int i = recentTrades.size() - 1; i >= 0; i--) {

			Trade trade = recentTrades.get(i);

			if (trade.getPrice() > lastPrice) {

				upTicks++;
				log.debug("UpTick, upTicks: {}, downTicks: {}", upTicks, downTicks);
				
				if (upTicks >= 3) {
					upTicks = 0;
					log.debug("Buying 1 BTC, upTicks: {}, downTicks: {}", upTicks, downTicks);
					Platform.runLater(() -> tradesObservers.forEach(o -> o.newImaginaryTrade(
							new Trade(imaginaryIndex++, MakerSide.BUY, OffsetDateTime.now(), 1.0, trade.getPrice()))));
				}

			} else if (trade.getPrice() < lastPrice) {

				downTicks++;
				log.debug("DownTick, upTicks: {}, downTicks: {}", upTicks, downTicks);

				if (downTicks >= 3) {
					downTicks = 0;
					log.debug("Selling 1 BTC, upTicks: {}, downTicks: {}", upTicks, downTicks);
					Platform.runLater(() -> tradesObservers.forEach(o -> o.newImaginaryTrade(
							new Trade(imaginaryIndex++, MakerSide.SELL, OffsetDateTime.now(), 1.0, trade.getPrice()))));
				}

			} else {
				log.debug("No action. upTicks: {}, downTicks: {}", upTicks, downTicks);
			}

			lastPrice = trade.getPrice();
		}

		recentTrades.clear();
	}

	@Override
	public ObserverType getObserverType() {
		return ObserverType.LOGIC;
	}

}
