package com.sonartrading.challenge.data.model;

import lombok.Data;

@Data
public class Order {

	private final String orderID;
	private final MakerSide markerSide;
	private double amount;
	private double price;

	private OrderStatus status = OrderStatus.NEW;
	
	public Order(String orderID, MakerSide markerSide, double amount, double price) {
		this.orderID = orderID;
		this.markerSide = markerSide;
		this.amount = amount;
		this.price = price;
	}

	public void update(double amount, double price) {
		this.amount = amount;
		this.price = price;
		status = OrderStatus.UPDATED;
	}
}