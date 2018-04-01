package com.sonartrading.challenge.data;

import static com.sonartrading.challenge.data.model.MakerSide.BUY;
import static com.sonartrading.challenge.data.model.MakerSide.SELL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.sonartrading.challenge.api.RestClient;
import com.sonartrading.challenge.api.dto.BitsoResponse;
import com.sonartrading.challenge.api.dto.OrderBook;
import com.sonartrading.challenge.api.dto.OrderBookItem;
import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Order;
import com.sonartrading.challenge.data.model.OrderStatus;
import com.sonartrading.challenge.data.observer.OrdersObserver;
import com.sonartrading.challenge.exception.InitializationException;
import com.sonartrading.challenge.ws.WebsocketClient;
import com.sonartrading.challenge.ws.dto.DiffOrder;
import com.sonartrading.challenge.ws.dto.WebsocketMessage;
import com.sonartrading.challenge.ws.handler.WebsocketHandler;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author arturo
 *
 */
@Slf4j
public class OrdersManager implements WebsocketHandler {

	private static final String DIFF_ORDERS_SUSCRIPTION = "diff-orders";
	public static final int DEFAULT_ORDERS_TO_OBSERVE = 10;

	private final Gson gson = new Gson();

	private final String book;
	private final RestClient restClient;

	private Object diffOrdersLock = new Object();
	private List<DiffOrder> diffOrders = new LinkedList<>();

	private Map<String, Order> bidOrders;
	private TreeMap<Double, List<Order>> bidOrdersByPrice;

	private Map<String, Order> askOrders;
	private TreeMap<Double, List<Order>> askOrdersByPrice;

	private final ReadWriteLock ordersReadWriteLock = new ReentrantReadWriteLock();
	private final Lock ordersReadLock = ordersReadWriteLock.readLock();
	private final Lock ordersWriteLock = ordersReadWriteLock.writeLock();

	private final List<OrdersObserver> ordersObservers = new ArrayList<>();

	private long lastDiffOrdersSequence = -1;

	// this value could be modified by ui while is used by manager
	private volatile int numOfOrdersToObserve = DEFAULT_ORDERS_TO_OBSERVE;

	private final ExecutorService executorService;

	public OrdersManager(String book, RestClient restClient, WebsocketClient websocketClient,
			ExecutorService executorService) {

		this.book = book;
		this.restClient = restClient;
		this.executorService = executorService;

		websocketClient.addHandler(book, DIFF_ORDERS_SUSCRIPTION, OrdersManager.this);

		getOrderBook();
	}

	// **************** REST CALL / HANDLER: Orders ****************

	private void getOrderBook() {

		restClient.getAPI().getOpenOrders(book, false).enqueue(new Callback<BitsoResponse<OrderBook>>() {

			@Override
			public void onResponse(Call<BitsoResponse<OrderBook>> call, Response<BitsoResponse<OrderBook>> response) {

				BitsoResponse<OrderBook> orderBookResponse = response.body();

				if (!orderBookResponse.getSuccess()) {
					throw new InitializationException("Error trying to get last orders");
				}

				OrderBook orderBook = orderBookResponse.getPayload();
				lastDiffOrdersSequence = orderBook.getSequence();
				log.debug("Order book received, sequence: {}", lastDiffOrdersSequence);

				executorService.execute(() -> {

					askOrders = getOrdersMap(orderBook.getAsks(), SELL);
					askOrdersByPrice = getOrdersSortedMap(askOrders, false);

					bidOrders = getOrdersMap(orderBook.getBids(), BUY);
					bidOrdersByPrice = getOrdersSortedMap(bidOrders, true);

					log.debug("Ask orders: {}, Bid orders: {}", askOrders.size(), bidOrders.size());

					int numOfOrders = numOfOrdersToObserve;
					List<Order> askOrdersToObserve, bidOrdersToObserve;

					// apply previous diff-order queued messages
					synchronized (diffOrdersLock) {
						log.debug("Applying previous diff-order queued messages: {}", diffOrders.size());
						diffOrders.stream().filter(d -> d.getSequence() > lastDiffOrdersSequence)
								.forEach(d -> updateOffersByDiff(d));
						diffOrders = null;
						askOrdersToObserve = getOrdersToObserve(askOrdersByPrice, numOfOrders);
						bidOrdersToObserve = getOrdersToObserve(bidOrdersByPrice, numOfOrders);
					}

					// notify new orders lists to observers
					Platform.runLater(
							() -> ordersObservers.forEach(o -> o.setOrders(askOrdersToObserve, bidOrdersToObserve)));
				});
			}

			@Override
			public void onFailure(Call<BitsoResponse<OrderBook>> call, Throwable t) {
				throw new InitializationException("Error trying to get last orders", t);
			}
		});
	}

	// ******************** WEBSOCKET HANDLERS: DiffOrders ********************

	@Override
	public void handleMessage(WebsocketMessage<?> response) {

		if (response.getPayload() == null || lastDiffOrdersSequence >= response.getSequence()) {
			return;
		}

		executorService.execute(() -> {
			// we could write a custom gson deserializer instead of this step
			DiffOrder[] orders = gson.fromJson(gson.toJson(response.getPayload()), DiffOrder[].class);

			if (orders.length == 0) {
				return;
			}

			DiffOrder diffOrder = orders[0];
			diffOrder.setSequence(response.getSequence());

			synchronized (diffOrdersLock) {
				if (diffOrders != null) {
					diffOrders.add(diffOrder);
				} else {
					updateOffersByDiff(diffOrder);
				}
			}
		});
	}

	// ****************************** DATA MODEL ******************************

	private Order addOrder(Order order) {

		ordersWriteLock.lock();

		switch (order.getMarkerSide()) {
		case BUY:
			bidOrders.put(order.getOrderID(), order);
			addOrderByPrice(bidOrdersByPrice, order);
			break;

		case SELL:
			askOrders.put(order.getOrderID(), order);
			addOrderByPrice(askOrdersByPrice, order);
			break;
		}

		ordersWriteLock.unlock();

		return order;
	}

	private void removeOrder(Order order) {

		ordersWriteLock.lock();

		switch (order.getMarkerSide()) {
		case BUY:
			bidOrders.remove(order.getOrderID());
			removeOrderByPrice(bidOrdersByPrice, order);
			break;

		case SELL:
			askOrders.remove(order.getOrderID());
			removeOrderByPrice(askOrdersByPrice, order);
			break;
		}

		ordersWriteLock.unlock();
	}

	private Order getOrderByID(String orderID, MakerSide makerSide) {

		ordersReadLock.lock();
		Order order = makerSide == BUY ? bidOrders.get(orderID) : askOrders.get(orderID);
		ordersReadLock.unlock();

		return order;
	}

	private void addOrderByPrice(Map<Double, List<Order>> orders, Order order) {

		List<Order> samePriceOrders = orders.get(order.getPrice());

		if (samePriceOrders == null) {
			samePriceOrders = new ArrayList<>();
			orders.put(order.getPrice(), samePriceOrders);
		}

		samePriceOrders.add(order);
	}

	private void removeOrderByPrice(Map<Double, List<Order>> orders, Order order) {

		List<Order> samePriceOrders = orders.remove(order.getPrice());

		if (samePriceOrders == null) {
			return;
		}

		samePriceOrders.remove(order);

		if (!samePriceOrders.isEmpty()) {
			orders.put(order.getPrice(), samePriceOrders);
		}
	}

	/**
	 * Given an existing order, returns the index in the TreeMap
	 */
	private int getOrderIndex(Order order) {

		ordersReadLock.lock();
		NavigableMap<Double, List<Order>> subMap = getOrdersByPriceMap(order.getMarkerSide()).headMap(order.getPrice(),
				true);
		List<Order> ordersSamePrice = subMap.lastEntry().getValue();

		int indexOrdersSamePrice = ordersSamePrice.indexOf(order);
		int indexMap = (int) subMap.values().stream().flatMap(List::stream).count() - ordersSamePrice.size();
		ordersReadLock.unlock();

		return indexMap + indexOrdersSamePrice;
	}

	/**
	 * Returns the {@code TreeMap} of the corresponding {@code MakerSide}
	 */
	private TreeMap<Double, List<Order>> getOrdersByPriceMap(MakerSide makerSide) {
		return makerSide == BUY ? bidOrdersByPrice : askOrdersByPrice;
	}

	/**
	 * Given a {@link List} of {@code Order}, it returns a {@link Map} with the
	 * ID as key.
	 */
	private Map<String, Order> getOrdersMap(List<OrderBookItem> orderBook, MakerSide makerSide) {
		return orderBook.stream().map(o -> new Order(o.getOid(), makerSide, Double.parseDouble(o.getAmount()),
				Double.parseDouble(o.getPrice()))).collect(Collectors.toMap(o -> o.getOrderID(), o -> o));
	}

	/**
	 * Given a {@link Map} of {@code Order} with the ID as key, it returns
	 * {@code TreeMap} with the price as key, grouping the orders with the same
	 * price in a {@code List}.
	 */
	private TreeMap<Double, List<Order>> getOrdersSortedMap(Map<String, Order> map, boolean reverse) {
		return map.values().stream().collect(Collectors.groupingBy(Order::getPrice,
				() -> reverse ? new TreeMap<>(Collections.reverseOrder()) : new TreeMap<>(), Collectors.toList()));
	}

	/**
	 * Updates the memory map using a {@link DiffOrder}
	 */
	private void updateOffersByDiff(DiffOrder diffOrder) {

		MakerSide makerSide = diffOrder.getOrderType() == 0 ? BUY : SELL;
		Order order = getOrderByID(diffOrder.getOrderID(), makerSide);

		double amount = diffOrder.getAmount() != null ? Double.parseDouble(diffOrder.getAmount()) : 0;

		if (order == null && amount == 0) {
			// order not found in memory, it was cancelled: nothing to do
			return;

		} else if (order == null) {
			// order not found, new order
			order = addOrder(
					new Order(diffOrder.getOrderID(), makerSide, amount, Double.parseDouble(diffOrder.getRate())));
		} else {
			// order in memory, check if order was cancelled
			if (amount == 0) {
				order.setStatus(OrderStatus.CANCELLED);
			} else {
				order.update(amount, Double.parseDouble(diffOrder.getRate()));
			}
		}

		int index = getOrderIndex(order);
		if (order.getStatus() == OrderStatus.CANCELLED) {
			removeOrder(order);
		}

		// check if changes from rest call were already applied and the index is
		// in the range to be observed
		int numOfOrders = numOfOrdersToObserve;
		if (diffOrders == null && index < numOfOrders) {
			notifyOrderUpdate(order, index, numOfOrders);
		}
	}

	/**
	 * Return a sorted list from the sorted map with a limited number of orders.
	 * It is not thread safe.
	 */
	private List<Order> getOrdersToObserve(Map<Double, List<Order>> orders, int numOfOrders) {
		return orders.values().stream().limit(numOfOrders).flatMap(List::stream).limit(numOfOrders)
				.collect(Collectors.toList());
	}

	// *************************** DATA OBSERVERS *****************************

	public void addOrdersObserver(OrdersObserver ordersObserver) {
		ordersObservers.add(ordersObserver);
	}

	public void setNumOfOrdersToObserve(int numOfOrdersToObserve) {

		int oldValue = this.numOfOrdersToObserve;
		this.numOfOrdersToObserve = numOfOrdersToObserve;

		if (oldValue < numOfOrdersToObserve) {
			ordersReadLock.lock();
			List<Order> askOrdersToObserve = getOrdersToObserve(askOrdersByPrice, numOfOrdersToObserve);
			List<Order> bidOrdersToObserve = getOrdersToObserve(bidOrdersByPrice, numOfOrdersToObserve);
			ordersReadLock.unlock();

			Platform.runLater(() -> ordersObservers.forEach(o -> {
				o.ordersUpdated(askOrdersToObserve, SELL, -1);
				o.ordersUpdated(bidOrdersToObserve, BUY, -1);
			}));
		}
	}

	private void notifyOrderUpdate(Order order, int index, int numOfOrders) {

		log.debug("Notifiying order change at index: {}, status: {}", index, order.getStatus());

		switch (order.getStatus()) {
		case NEW:
		case CANCELLED:
			// notify full list
			ordersReadLock.lock();
			List<Order> ordersToObserve = getOrdersToObserve(getOrdersByPriceMap(order.getMarkerSide()), numOfOrders);
			ordersReadLock.unlock();
			Platform.runLater(
					() -> ordersObservers.forEach(o -> o.ordersUpdated(ordersToObserve, order.getMarkerSide(), index)));
			break;
		default:
			// notify update
			Platform.runLater(() -> ordersObservers.forEach(o -> o.orderUpdated(order, index)));
			break;
		}
	}
}
