package com.sonartrading.challenge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sonartrading.challenge.api.RestClient;
import com.sonartrading.challenge.data.OrdersManager;
import com.sonartrading.challenge.data.TradesManager;
import com.sonartrading.challenge.strategy.ContrarianStrategy;
import com.sonartrading.challenge.view.MainController;
import com.sonartrading.challenge.ws.WebsocketClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SonarTrading extends Application {

	private static final String WEBSOCKET_ENDPOINT = "wss://ws.bitso.com";
	private static final String HTTP_ENDPOINT = "https://api.bitso.com/v3/";

	private static final String BOOK = "btc_mxn";
	private static final String MAIN_SCENE_FXML = "MainScene.fxml";
	private static final String APP_TITTLE = "Bitso Client";

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Override
	public void start(Stage primaryStage) throws Exception {

		RestClient restClient = new RestClient(HTTP_ENDPOINT);
		WebsocketClient websocketClient = new WebsocketClient(WEBSOCKET_ENDPOINT);

		TradesManager tradesManager = new TradesManager(BOOK, restClient, executorService);
		OrdersManager ordersManager = new OrdersManager(BOOK, restClient, websocketClient, executorService);

		FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(MAIN_SCENE_FXML));

		Parent root = loader.load();

		MainController controller = loader.getController();
		controller.setOrdersManager(ordersManager);
		controller.setTradesManager(tradesManager);

		ContrarianStrategy contrarianStrategy = new ContrarianStrategy();
		tradesManager.addTradesObserver(contrarianStrategy);
		contrarianStrategy.addImaginaryTradesObserver(controller);

		Scene scene = new Scene(root);
		primaryStage.setTitle(APP_TITTLE);
		primaryStage.setScene(scene);

		primaryStage.setOnCloseRequest(e -> {
			Platform.exit();
			System.exit(0);
		});

		primaryStage.show();
	}

	/**
	 * Java main for when running without JavaFX launcher
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
