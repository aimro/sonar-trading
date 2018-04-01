package com.sonartrading.challenge.api;

import java.util.List;

import com.sonartrading.challenge.api.dto.BitsoResponse;
import com.sonartrading.challenge.api.dto.OrderBook;
import com.sonartrading.challenge.api.dto.TradeBook;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RestAPI {

	@GET("order_book")
	Call<BitsoResponse<OrderBook>> getOpenOrders(@Query("book") String book, @Query("aggregate") Boolean aggregate);

	@GET("trades")
	Call<BitsoResponse<List<TradeBook>>> getRecentTrades(@Query("book") String book, @Query("sort") String order,
			@Query("limit") Integer limit);

	public static class SortType {

		public static final String ASC = "asc";
		public static final String DESC = "desc";

		private SortType() {
			throw new IllegalStateException("Utility class");
		}
	}
}
