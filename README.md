# Sonar Trading Programming Challenge

## How to generate an executable ###

```bash
      mvn clean package
```
This command generates a new jar in the *target* folder

## How to execute ###

```bash
      java -jar sonar-trading-1.0-SNAPSHOT.jar
```

## Thoughts and considerations ###

* There are two managers (OrdersManager and TradesManager) in charge of subscribing to websocket and making the rest calls. These managers obtains all information, organizes it in memory and distributes to any interested observer.
* OrdersManager manages two memory maps: a HashMap with the orderID as the key and a TreeMap (sorted map) with the price as the key. Although it increases the complexity of the code and the memory space, this allows to perform more efficient searches: *O(1) vs O(log n)*.
* The only sources used are the ‘diff-orders’ channel (Websocket), and the Orderbook and Trades endpoints (REST).
* Although it would be desirable to have a UI model totally decoupled from the data model, the UI is using the data model directly just to save time.
* The contrarian trading strategy is using snapshots obtained by polling the Trades endpoint, so it checks previous trades processed in each notificacion.
* The main class is *com.sonartrading.challenge.SonarTrading*. No framework is used, all the instances (data managers, network clients, ui controller) are created here.
* The project is using log4j for logging / debugging purposes. There configuration file *log4j2.properties* is located in *src/main/resources*.


## Appendix A: Checklist ###

**Feature**|**File name**|**Method name**
:-----:|:-----:|:-----:
Schedule the polling of trades over REST.| TradesManager | addTradesObserver / getRecentTrades
Request a book snapshot over REST.| OrdersManager | getOrderBook
Listen for diff-orders over websocket.| OrdersManager | handleMessage / updateOffersByDiff
Replay diff-orders.| OrdersManager | getOffers->onResponse / updateOffersByDiff
Use config option X to request recent trades.| MainController / TradesManager | tradesToDisplayEvent -> setNumOfTradesToObserve
Use config option X to limit number of ASKs displayed in UI.| MainController / OrdersManager | ordersToDisplayEvent -> setNumOfOrdersToObserve
The loop that causes the trading algorithm to reevaluate.| ContrarianStrategy | tradesUpdated

