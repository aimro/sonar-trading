package com.sonartrading.challenge.data;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sonartrading.challenge.api.RestAPI;
import com.sonartrading.challenge.api.dto.BitsoResponse;
import com.sonartrading.challenge.api.dto.OrderBook;
import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Order;
import com.sonartrading.challenge.data.observer.OrdersObserver;
import com.sonartrading.challenge.ws.WebsocketClient;
import com.sonartrading.challenge.ws.dto.DiffOrder;
import com.sonartrading.challenge.ws.dto.WebsocketMessage;

import javafx.embed.swing.JFXPanel;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

public class OrdersManagerTest {

	private static final int DEFAULT_AWAIT_TIME = 4000;
	private static final Type ORDER_BOOK_TYPE = new TypeToken<BitsoResponse<OrderBook>>() {
	}.getType();

	private Gson gson = new Gson();
	private OrdersManager ordersManager;
	private BitsoResponse<OrderBook> simpleBitsoReponse;

	private WebsocketClient wsClient = Mockito.mock(WebsocketClient.class);

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	private BehaviorDelegate<RestAPI> restAPI;
	private final Retrofit retrofit = new Retrofit.Builder().baseUrl("http://mytest.com").build();
	private MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).backgroundExecutor(executorService).build();

	@Before
	public void setUp() throws Exception {
		restAPI = mockRetrofit.create(RestAPI.class);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testOrdersManager() throws InterruptedException {

		CountDownLatch lock = new CountDownLatch(1);
		ordersManager = new OrdersManager(null, restAPI.returningResponse(getSimpleResponse()), wsClient,
				executorService);

		Map<String, List<Order>> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {
			@Override
			public void setOrders(List<Order> askOrders, List<Order> bidOrders) {
				result.put("askOrders", askOrders);
				result.put("bidOrders", bidOrders);
				lock.countDown();
			}
		});

		lock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		Assert.assertNotNull(result.get("askOrders"));
		Assert.assertNotNull(result.get("bidOrders"));
		Assert.assertEquals(3, result.get("askOrders").size());
		Assert.assertEquals(2, result.get("bidOrders").size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testHandleMessageNewOrder() throws InterruptedException {

		OrderBook orderBook = new OrderBook(Collections.emptyList(), Collections.emptyList(), "", 0l);
		BitsoResponse<OrderBook> response = new BitsoResponse<>(true, orderBook);

		CountDownLatch initializationLock = new CountDownLatch(1);
		CountDownLatch resultLock = new CountDownLatch(1);

		ordersManager = new OrdersManager(null, restAPI.returningResponse(response), wsClient, executorService);

		Map<String, Object> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {

			@Override
			public void setOrders(List<Order> newAskOrders, List<Order> newBidOrders) {
				initializationLock.countDown();
			}

			@Override
			public void ordersUpdated(List<Order> orders, MakerSide makerSide, int index) {
				result.put("orders", orders);
				result.put("makerSide", makerSide);
				result.put("index", index);
				resultLock.countDown();
			}
		});

		initializationLock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		WebsocketMessage<DiffOrder[]> message = new WebsocketMessage<>(null, null, 1l,
				new DiffOrder[] { new DiffOrder(10000000000000l, "100", (short) 0, "0.1", "10", "abcd", 1l) });

		ordersManager.handleMessage(message);

		resultLock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		assertEquals(MakerSide.BUY, result.get("makerSide"));
		assertEquals(0, result.get("index"));
		assertEquals("abcd", ((List<Order>) result.get("orders")).get(0).getOrderID());
	}

	@Test
	public void testHandleMessageUpdateOrder() throws InterruptedException {

		CountDownLatch initializationLock = new CountDownLatch(1);
		CountDownLatch resultLock = new CountDownLatch(1);

		ordersManager = new OrdersManager(null, restAPI.returningResponse(getSimpleResponse()), wsClient,
				executorService);

		Map<String, Object> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {

			@Override
			public void setOrders(List<Order> newAskOrders, List<Order> newBidOrders) {
				initializationLock.countDown();
			}

			@Override
			public void orderUpdated(Order order, int index) {
				result.put("order", order);
				result.put("index", index);
				resultLock.countDown();
			}
		});

		initializationLock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		WebsocketMessage<DiffOrder[]> message = new WebsocketMessage<>(null, null, 27215l,
				new DiffOrder[] { new DiffOrder(10000000000000l, "13227.14", (short) 1, "0.4259", "5633.44",
						"RP8lVpgXf04o6vJ6", 27215l) });

		ordersManager.handleMessage(message);

		resultLock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		assertEquals(1, result.get("index"));
		assertEquals("RP8lVpgXf04o6vJ6", ((Order) result.get("order")).getOrderID());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testHandleMessageRemoveOrder() throws InterruptedException {

		CountDownLatch initializationLock = new CountDownLatch(1);
		CountDownLatch resultLock = new CountDownLatch(1);

		ordersManager = new OrdersManager(null, restAPI.returningResponse(getSimpleResponse()), wsClient,
				executorService);

		Map<String, Object> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {

			@Override
			public void setOrders(List<Order> newAskOrders, List<Order> newBidOrders) {
				initializationLock.countDown();
			}

			@Override
			public void ordersUpdated(List<Order> orders, MakerSide makerSide, int index) {
				result.put("orders", orders);
				result.put("makerSide", makerSide);
				result.put("index", index);
				resultLock.countDown();
			}
		});

		initializationLock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		WebsocketMessage<DiffOrder[]> message = new WebsocketMessage<>(null, null, 27215l, new DiffOrder[] {
				new DiffOrder(10000000000000l, "13227.14", (short) 1, "0", "5633.44", "RP8lVpgXf04o6vJ6", 27215l) });

		ordersManager.handleMessage(message);

		resultLock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		assertEquals(MakerSide.SELL, result.get("makerSide"));
		assertEquals(1, result.get("index"));
		assertEquals(2, ((List<Order>) result.get("orders")).size());
	}

	@Test
	public void testApplyDiffOrders() throws InterruptedException {

		NetworkBehavior networkBehavior = NetworkBehavior.create();
		networkBehavior.setDelay(1000, TimeUnit.MILLISECONDS);

		MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).backgroundExecutor(executorService)
				.networkBehavior(networkBehavior).build();

		CountDownLatch lock = new CountDownLatch(1);

		ordersManager = new OrdersManager(null,
				mockRetrofit.create(RestAPI.class).returningResponse(getSimpleResponse()), wsClient, executorService);

		Map<String, List<Order>> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {
			@Override
			public void setOrders(List<Order> askOrders, List<Order> bidOrders) {
				result.put("askOrders", askOrders);
				result.put("bidOrders", bidOrders);
				lock.countDown();
			}
		});

		ordersManager.handleMessage(new WebsocketMessage<>(null, null, 27215l, new DiffOrder[] {
				new DiffOrder(10000000000000l, "28210.7", (short) 1, "5", "5642.14", "46efbiv72drbphig", 27215l) }));

		ordersManager.handleMessage(new WebsocketMessage<>(null, null, 27215l, new DiffOrder[] {
				new DiffOrder(10000000000000l, "13227.14", (short) 1, "0", "5633.44", "RP8lVpgXf04o6vJ6", 27216l) }));

		lock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		Assert.assertNotNull(result.get("askOrders"));
		Assert.assertNotNull(result.get("bidOrders"));
		assertEquals(2, result.get("bidOrders").size());
		assertEquals(2, result.get("askOrders").size());
		assertEquals("46efbiv72drbphig", result.get("askOrders").get(1).getOrderID());
		assertEquals(5, result.get("askOrders").get(1).getAmount(), 0.001);
	}
	
	@Test
	public void testDefaultNumOfOrdersToObserve() throws InterruptedException {

		InputStream inputStream = this.getClass().getResourceAsStream("/json/large_order_book.json");
		BitsoResponse<OrderBook> response = gson.fromJson(new InputStreamReader(inputStream), ORDER_BOOK_TYPE);

		CountDownLatch lock = new CountDownLatch(1);
		ordersManager = new OrdersManager(null, restAPI.returningResponse(response), wsClient, executorService);

		Map<String, List<Order>> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {
			@Override
			public void setOrders(List<Order> askOrders, List<Order> bidOrders) {
				result.put("askOrders", askOrders);
				result.put("bidOrders", bidOrders);
				lock.countDown();
			}
		});

		lock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		Assert.assertNotNull(result.get("askOrders"));
		Assert.assertNotNull(result.get("bidOrders"));
		Assert.assertEquals(OrdersManager.DEFAULT_ORDERS_TO_OBSERVE, result.get("askOrders").size());
		Assert.assertEquals(2, result.get("bidOrders").size());
	}

	@Test
	public void testMoreNumOfOrdersToObserve() throws InterruptedException {

		InputStream inputStream = this.getClass().getResourceAsStream("/json/large_order_book.json");
		BitsoResponse<OrderBook> response = gson.fromJson(new InputStreamReader(inputStream), ORDER_BOOK_TYPE);

		CountDownLatch lock = new CountDownLatch(1);
		CountDownLatch lock2 = new CountDownLatch(2);
		ordersManager = new OrdersManager(null, restAPI.returningResponse(response), wsClient, executorService);

		Map<String, List<Order>> result = new HashMap<>();
		ordersManager.addOrdersObserver(new MockOrdersObserver() {
			@Override
			public void setOrders(List<Order> askOrders, List<Order> bidOrders) {
				result.put("askOrders", askOrders);
				result.put("bidOrders", bidOrders);
				lock.countDown();
			}

			@Override
			public void ordersUpdated(List<Order> orders, MakerSide makerSide, int index) {
				result.put(makerSide == MakerSide.SELL ? "askOrders" : "bidOrders", orders);
				lock2.countDown();
			}
		});

		lock.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		Assert.assertNotNull(result.get("askOrders"));
		Assert.assertNotNull(result.get("bidOrders"));
		Assert.assertEquals(OrdersManager.DEFAULT_ORDERS_TO_OBSERVE, result.get("askOrders").size());
		Assert.assertEquals(2, result.get("bidOrders").size());

		ordersManager.setNumOfOrdersToObserve(11);

		lock2.await(DEFAULT_AWAIT_TIME, TimeUnit.MILLISECONDS);

		Assert.assertEquals(11, result.get("askOrders").size());
		Assert.assertEquals(2, result.get("bidOrders").size());
	}

	private BitsoResponse<OrderBook> getSimpleResponse() {

		if (simpleBitsoReponse == null) {
			InputStream inputStream = this.getClass().getResourceAsStream("/json/simple_order_book.json");
			return gson.fromJson(new InputStreamReader(inputStream), ORDER_BOOK_TYPE);
		}

		return simpleBitsoReponse;
	}

	abstract class MockOrdersObserver implements OrdersObserver {

		@Override
		public void orderUpdated(Order order, int index) {

		}

		@Override
		public void ordersUpdated(List<Order> orders, MakerSide makerSide, int index) {

		}

		@Override
		public void setOrders(List<Order> askOrders, List<Order> bidOrders) {

		}

	}

	@SuppressWarnings("unused")
	private JFXPanel fxPanel = new JFXPanel();

}
