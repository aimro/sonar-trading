package com.sonartrading.challenge.view;

import java.util.ArrayList;
import java.util.List;

import com.sonartrading.challenge.data.OrdersManager;
import com.sonartrading.challenge.data.TradesManager;
import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Order;
import com.sonartrading.challenge.data.model.Trade;
import com.sonartrading.challenge.data.observer.OrdersObserver;
import com.sonartrading.challenge.data.observer.TradesObserver;
import com.sonartrading.challenge.strategy.ImaginaryTradesObserver;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainController implements OrdersObserver, TradesObserver, ImaginaryTradesObserver {

	private static final String[] ORDER_FIELD_NAMES = { "orderID", "amount", "price" };
	private static final String[] ORDER_TABLE_COLUMN_NAMES = { "Order ID", "Amount", "Price" };

	private static final String[] TRADE_FIELD_NAMES = { "tradeID", "makerSide", "date", "amount", "price" };
	private static final String[] TRADE_TABLE_COLUMN_NAMES = { "Trade ID", "Maker Side", "Date", "Amount", "Price" };

	@FXML
	private TableView<Order> buyOrdersTable;

	@FXML
	private TableView<Order> sellOrdersTable;

	@FXML
	private TableView<Trade> recentTradesTable;

	@FXML
	private TableView<Trade> imaginaryTradesTable;

	@FXML
	private MenuItem quitMenuItem;

	@FXML
	private TextField tradesPollingTime;

	@FXML
	private TextField numOfTradesToDisplay;
	private int lastNumberOfTradesToDisplay = TradesManager.DEFAULT_TRADES_TO_OBSERVE;

	@FXML
	private TextField numOfOrdersToDisplay;
	private int lastNumberOfOrdersToDisplay = OrdersManager.DEFAULT_ORDERS_TO_OBSERVE;

	private OrdersManager ordersManager;
	private TradesManager tradesManager;

	private List<Trade> recentTrades = new ArrayList<>();
	private List<Order> recentAskOrders = new ArrayList<>();
	private List<Order> recentBidOrders = new ArrayList<>();

	private List<Trade> imaginaryTrades = new ArrayList<>();

	@FXML
	public void initialize() {

		initOrdersTableView(buyOrdersTable, ORDER_TABLE_COLUMN_NAMES, ORDER_FIELD_NAMES);
		initOrdersTableView(sellOrdersTable, ORDER_TABLE_COLUMN_NAMES, ORDER_FIELD_NAMES);
		initOrdersTableView(recentTradesTable, TRADE_TABLE_COLUMN_NAMES, TRADE_FIELD_NAMES);
		initOrdersTableView(imaginaryTradesTable, TRADE_TABLE_COLUMN_NAMES, TRADE_FIELD_NAMES);

		imaginaryTradesTable.setItems(FXCollections.observableList(imaginaryTrades));
		recentTradesTable.setItems(FXCollections.observableList(recentTrades));

		numOfOrdersToDisplay.setText(Integer.toString(OrdersManager.DEFAULT_ORDERS_TO_OBSERVE));
		numOfOrdersToDisplay.setOnKeyPressed(ordersToDisplayEvent);

		numOfTradesToDisplay.setText(Integer.toString(TradesManager.DEFAULT_TRADES_TO_OBSERVE));
		numOfTradesToDisplay.setOnKeyPressed(tradesToDisplayEvent);

		tradesPollingTime.setText(Integer.toString(TradesManager.DEFAULT_TIME_TO_POLL_TRADES));
		tradesPollingTime.setOnKeyPressed(tradesPollingTimeEvent);
	}

	private <T> void initOrdersTableView(TableView<T> tableView, String columnNames[], String fieldNames[]) {
		for (int index = 0; index < columnNames.length; index++) {
			TableColumn<T, String> column = new TableColumn<>(columnNames[index]);
			column.setCellValueFactory(new PropertyValueFactory<T, String>(fieldNames[index]));
			column.setEditable(false);
			column.setSortable(false);
			tableView.getColumns().add(column);
		}
	}

	public void setOrdersManager(OrdersManager ordersManager) {

		this.ordersManager = ordersManager;
		ordersManager.addOrdersObserver(this);
	}

	public void setTradesManager(TradesManager tradesManager) {

		this.tradesManager = tradesManager;
		tradesManager.addTradesObserver(this);
	}

	@FXML
	void onExitMenuItem(ActionEvent event) {
		System.exit(0);
	}

	@Override
	public void tradesUpdated(List<Trade> trades) {
		log.debug("Updating trades table with {} items", trades.size());

		recentTrades.clear();
		recentTrades.addAll(trades);
		recentTradesTable.refresh();
	}

	@Override
	public void orderUpdated(Order order, int index) {

		log.debug("Order with index {} updated", index);

		switch (order.getMarkerSide()) {
		case BUY:
			buyOrdersTable.refresh();
			break;

		case SELL:
			sellOrdersTable.refresh();
			break;
		}
	}

	@Override
	public void ordersUpdated(List<Order> orders, MakerSide makerSide, int changeIndex) {

		log.debug("Updating orders {} table with {} items", makerSide, orders.size());

		List<Order> recentOrders;
		TableView<Order> tableView;

		if (makerSide == MakerSide.BUY) {
			recentOrders = recentBidOrders;
			tableView = buyOrdersTable;
		} else {
			recentOrders = recentAskOrders;
			tableView = sellOrdersTable;
		}

		recentOrders.clear();
		recentOrders.addAll(orders);
		tableView.refresh();
	}

	@Override
	public void setOrders(List<Order> askOrders, List<Order> bidOrders) {

		recentBidOrders.addAll(bidOrders);
		buyOrdersTable.setItems(FXCollections.observableList(recentBidOrders));

		recentAskOrders.addAll(askOrders);
		sellOrdersTable.setItems(FXCollections.observableList(recentAskOrders));
	}

	@Override
	public void newImaginaryTrade(Trade trade) {
		imaginaryTrades.add(trade);
		imaginaryTradesTable.refresh();
	}

	private final EventHandler<? super KeyEvent> ordersToDisplayEvent = event -> {

		if (event.getCode() == KeyCode.ENTER && ordersManager != null) {
			try {
				int number = Integer.parseInt(numOfOrdersToDisplay.getText());

				if (lastNumberOfOrdersToDisplay > number) {
					recentAskOrders.subList(number, recentAskOrders.size()).clear();
					recentBidOrders.subList(number, recentBidOrders.size()).clear();
					buyOrdersTable.refresh();
					sellOrdersTable.refresh();
				}

				lastNumberOfOrdersToDisplay = number;
				ordersManager.setNumOfOrdersToObserve(number);
			} catch (NumberFormatException nfe) {
				// nothing
			}
		}
	};

	private final EventHandler<? super KeyEvent> tradesToDisplayEvent = event -> {

		if (event.getCode() == KeyCode.ENTER && tradesManager != null) {
			try {
				int number = Integer.parseInt(numOfTradesToDisplay.getText());

				if (lastNumberOfTradesToDisplay > number) {
					recentTrades.subList(number, recentTrades.size()).clear();
					recentTradesTable.refresh();
				}

				lastNumberOfTradesToDisplay = number;
				tradesManager.setNumOfTradesToObserve(number);
			} catch (NumberFormatException nfe) {
				// nothing
			}
		}
	};

	private final EventHandler<? super KeyEvent> tradesPollingTimeEvent = event -> {

		if (event.getCode() == KeyCode.ENTER && tradesManager != null) {
			try {
				tradesManager.setPollingTime(Integer.parseInt(tradesPollingTime.getText()));
			} catch (NumberFormatException nfe) {
				// nothing
			}
		}
	};

	@Override
	public ObserverType getObserverType() {
		return ObserverType.UI;
	}

}
