package com.sonartrading.challenge.ws.handler;

import java.util.LinkedList;

import com.google.gson.Gson;
import com.sonartrading.challenge.ws.dto.DiffOrder;
import com.sonartrading.challenge.ws.dto.WebsocketMessage;

public class DiffOrdersHandler implements WebsocketHandler {

//	public DiffOrdersHandler(String book, String type) {
//		super(book, type);
//	}
	
	private final Gson gson = new Gson();
	private final LinkedList<DiffOrder> diffOrders = new LinkedList<>();
	
	private long lastSequence = -1;

	@Override
	public void handleMessage(WebsocketMessage<?> response) {

		if (response.getPayload() == null) {
			return;
		}

		if (lastSequence >= response.getSequence()) {
			return;
		}

		lastSequence = response.getSequence();
		DiffOrder newOrders[] = gson.fromJson(gson.toJson(response.getPayload()), DiffOrder[].class);
		
		if(newOrders.length > 0) {
			addDiffOrder(newOrders[0]);
		}
	}
	
	public void addDiffOrder(DiffOrder diffOrder) {
		diffOrders.addLast(diffOrder);		
	}

}
