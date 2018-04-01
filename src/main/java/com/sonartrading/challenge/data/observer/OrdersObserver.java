package com.sonartrading.challenge.data.observer;

import java.util.List;

import com.sonartrading.challenge.data.model.MakerSide;
import com.sonartrading.challenge.data.model.Order;

public interface OrdersObserver {

	void orderUpdated(Order order, int index);

	void ordersUpdated(List<Order> orders, MakerSide makerSide, int index);

	void setOrders(List<Order> askOrders, List<Order> bidOrders);
}
