package com.sonartrading.challenge.data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sonartrading.challenge.api.RestAPI;
import com.sonartrading.challenge.api.RestAPI.SortType;
import com.sonartrading.challenge.api.dto.BitsoResponse;
import com.sonartrading.challenge.api.dto.TradeBook;
import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Trade;
import com.sonartrading.challenge.data.observer.TradesObserver;
import com.sonartrading.challenge.data.observer.TradesObserver.ObserverType;
import com.sonartrading.challenge.exception.InitializationException;
import com.sonartrading.challenge.parser.DateTime;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Slf4j
public class TradesManager {

	public static final int DEFAULT_TIME_TO_POLL_TRADES = 5;
	public static final int DEFAULT_TRADES_TO_OBSERVE = 10;

	private final String book;
	private final RestAPI restAPI;

	private final List<TradesObserver> tradesObservers = new ArrayList<>();

	private final ExecutorService executorService;
	private final ScheduledExecutorService schedulerdExecutor = Executors.newSingleThreadScheduledExecutor();

	private ScheduledFuture<?> scheduledFuture = null;

	private int timeToPoll = DEFAULT_TIME_TO_POLL_TRADES;
	
	// these values could be modified by ui while is used by manager
	private volatile int numOfTradesToObserve = DEFAULT_TRADES_TO_OBSERVE;

	public TradesManager(String book, RestAPI restAPI, ExecutorService executorService) {

		this.book = book;
		this.restAPI = restAPI;
		this.executorService = executorService;
	}

	// ********************** REST CALL / HANDLER Trades **********************

	private void getRecentTrades() {

		restAPI.getRecentTrades(book, SortType.DESC, numOfTradesToObserve)
				.enqueue(new Callback<BitsoResponse<List<TradeBook>>>() {

					@Override
					public void onResponse(Call<BitsoResponse<List<TradeBook>>> call,
							Response<BitsoResponse<List<TradeBook>>> response) {

						BitsoResponse<List<TradeBook>> tradeResponse = response.body();

						if (!tradeResponse.getSuccess()) {
							throw new InitializationException("Error trying to get last orders");
						}

						List<TradeBook> tradesBook = tradeResponse.getPayload();
						List<Trade> trades = tradesBook.stream()
								.map(t -> new Trade(t.getTid(), MakerSide.getByString(t.getMarkerSide()),
										OffsetDateTime.parse(t.getCreated_at(), DateTime.DATE_TIME_FORMATTER),
										Double.parseDouble(t.getAmount()), Double.parseDouble(t.getPrice())))
								.collect(Collectors.toList());

						log.debug("{} trades received", trades.size());
						tradesObservers.forEach(o -> {
							Runnable runnable = () -> o.tradesUpdated(trades);
							if (o.getObserverType() == ObserverType.UI) {
								Platform.runLater(runnable);
							} else {
								executorService.execute(runnable);
							}
						});
					}

					@Override
					public void onFailure(Call<BitsoResponse<List<TradeBook>>> call, Throwable t) {
						throw new InitializationException("Error trying to get recent trades", t);
					}
				});
	}

	private void reschedulePolling() {

		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}

		scheduledFuture = schedulerdExecutor.scheduleWithFixedDelay(this::getRecentTrades, 0, timeToPoll,
				TimeUnit.SECONDS);
	}

	// *************************** DATA OBSERVERS *****************************

	public void addTradesObserver(TradesObserver tradesObserver) {

		tradesObservers.add(tradesObserver);

		// start polling
		if (tradesObservers.size() == 1) {
			reschedulePolling();
		}
	}

	public void setPollingTime(int seconds) {
		timeToPoll = seconds;
		reschedulePolling();
	}
	
	public void setNumOfTradesToObserve(int numOfTradesToObserve) {

		int oldValue = this.numOfTradesToObserve;
		this.numOfTradesToObserve = numOfTradesToObserve;

		if (oldValue < numOfTradesToObserve) {
			reschedulePolling();
		}
	}
}
