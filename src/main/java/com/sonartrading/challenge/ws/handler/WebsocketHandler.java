package com.sonartrading.challenge.ws.handler;

import com.sonartrading.challenge.ws.dto.WebsocketMessage;

public interface WebsocketHandler {

	void handleMessage(WebsocketMessage<?> response);
}
