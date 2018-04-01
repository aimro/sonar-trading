package com.sonartrading.challenge.ws;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.sonartrading.challenge.exception.InitializationException;
import com.sonartrading.challenge.ws.dto.Subscribe;
import com.sonartrading.challenge.ws.dto.WebsocketMessage;
import com.sonartrading.challenge.ws.handler.WebsocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebsocketClient extends WebSocketAdapter {

	private final Gson gson = new Gson();
	private final Type messageType = new TypeToken<WebsocketMessage<?>>() {
	}.getType();

	private WebSocket websocket;

	private Map<String, Map<String, WebsocketHandler>> handlers = new HashMap<String, Map<String, WebsocketHandler>>();

	public WebsocketClient(String serverUri) {
		try {
			websocket = new WebSocketFactory().createSocket(serverUri).addListener(this).connect();
		} catch (WebSocketException | IOException e) {
			throw new InitializationException("Error trying to create websocket connection", e);
		}
	}

	public void addHandler(String book, String type, WebsocketHandler handler) {

		Map<String, WebsocketHandler> bookHandler = handlers.get(book);

		if (bookHandler == null) {
			bookHandler = new HashMap<>();
			handlers.put(book, bookHandler);
		}

		bookHandler.put(type, handler);
		websocket.sendText(gson.toJson(new Subscribe(book, type)));
	}

	@Override
	public void onTextMessage(WebSocket websocket, String data) throws Exception {

		log.debug("Message received: {}", data);
		WebsocketMessage<?> message = gson.fromJson(data, messageType);

		Map<String, WebsocketHandler> bookHandler = handlers.get(message.getBook());
		if (bookHandler != null && bookHandler.containsKey(message.getType())) {
			bookHandler.get(message.getType()).handleMessage(message);
		}
	}

	@Override
	public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
		throw new RuntimeException("Error on websocket connection", cause);
	}
}
