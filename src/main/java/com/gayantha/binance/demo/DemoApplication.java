package com.gayantha.binance.demo;

import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gayantha.binance.demo.model.DepthCache;
import com.gayantha.binance.demo.model.OrderBook;
import com.gayantha.binance.demo.service.OrderBookService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DemoApplication {

	@Autowired
	private OrderBookService orderBookService;

	private Map<String, Long> lastUpdateIds = new HashMap<>();
	private Map<String, Boolean> firstEventStatuses = new HashMap<>();
	private final Map<String, WebSocketClient> webSocketClients = new HashMap<>();
	final Map<String, ArrayList<DepthCache>> pendingList = new HashMap<>();
	private final Lock lock = new ReentrantLock();
	private final AtomicBoolean isProcessingPendingList = new AtomicBoolean(false);

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	public void printOrderBooksPlusTotalVolumeChange() {
		for (String symbol : orderBookService.getAllSymbols()) {
			orderBookService.getOrderBook(symbol).printOrderBook(symbol, 50);
		}
	}

	@Scheduled(fixedRate = 10000)
	public void updateOrderBooks() {
		try {
			String[] symbols = orderBookService.getAllSymbols();
			initialize(symbols);
			openWebSocketStreams(symbols);
			for (String symbol : symbols) {
				if (lastUpdateIds.get(symbol) == -1) {
					System.out.println(fetchDepthSnapshot(symbol));
					OrderBook orderBook = deserializeOrderBook(fetchDepthSnapshot(symbol));
					lastUpdateIds.replace(symbol, orderBook.getLastUpdateId());
					orderBook.setSymbol(symbol);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		printOrderBooksPlusTotalVolumeChange();
	}

	@Scheduled(fixedRate = 5000)
	public void processPendingLists() {
		if (isProcessingPendingList.compareAndSet(false, true)) {
			try {
				Set<String> symbols = pendingList.keySet();
				for (String symbol : symbols) {
					processPendingList(symbol);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// System.out.println("Pending list processor is already running.");
		}
	}

	private void processPendingList(String symbol) {
		lock.lock();
		try {
			ArrayList<DepthCache> currentList = pendingList.get(symbol);
			if (!currentList.isEmpty()) {
				Long lastUpdateId = lastUpdateIds.get(symbol);
				Iterator<DepthCache> iterator = currentList.iterator();
				while (iterator.hasNext()) {
					DepthCache depthCache = iterator.next();
					if (depthCache.getFinalUpdateId() <= lastUpdateId) {
						iterator.remove();
						continue;
					}
					if (!firstEventStatuses.get(symbol)) {
						if (depthCache.getFirstUpdateId() <= (lastUpdateId + 1)
								&& depthCache.getFinalUpdateId() >= (lastUpdateId + 1)) {
							firstEventStatuses.put(symbol, true);
							processEvent(depthCache, symbol);
							lastUpdateIds.put(symbol, depthCache.getFinalUpdateId());
							iterator.remove();
						}
					} else {
						if ((lastUpdateId + 1) == depthCache.getFirstUpdateId()) {
							processEvent(depthCache, symbol);
							lastUpdateIds.put(symbol, depthCache.getFinalUpdateId());
							iterator.remove();
						}
					}
				}
			}
		} finally {
			lock.unlock();
			isProcessingPendingList.set(false);
		}
	}

	private void processEvent(DepthCache depthCache, String symbol) {
		removeZeroQuantityPriceLevels(depthCache.getBids());
		removeZeroQuantityPriceLevels(depthCache.getAsks());
		orderBookService.getOrderBook(symbol).updateOrderBook(depthCache.getBids(), depthCache.getAsks());
	}

	private void removeZeroQuantityPriceLevels(List<List<String>> priceLevels) {
		Iterator<List<String>> iterator = priceLevels.iterator();
		while (iterator.hasNext()) {
			List<String> priceLevel = iterator.next();
			if (new BigDecimal(priceLevel.get(1)).compareTo(BigDecimal.ZERO) == 0) {
				iterator.remove();
			}
		}
	}

	private void initialize(String[] symbols) {
		for (String symbol : symbols) {
			if (!lastUpdateIds.containsKey(symbol)) {
				lastUpdateIds.put(symbol, -1L);
			}
			if (!pendingList.containsKey(symbol)) {
				pendingList.put(symbol, new ArrayList<>());
			}
			if (!firstEventStatuses.containsKey(symbol)) {
				firstEventStatuses.put(symbol, false);
			}
		}
	}

	private String fetchDepthSnapshot(String symbol) throws Exception {
		String url = "https://api.binance.com/api/v3/depth?symbol=" + symbol.toUpperCase() + "&limit=50";
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		HttpResponse response = httpClient.execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			throw new RuntimeException("Failed to fetch depth snapshot. Status code: " + statusCode);
		}

		return EntityUtils.toString(response.getEntity());
	}

	public static OrderBook deserializeOrderBook(String json) {
		Gson gson = new Gson();
		TypeToken<OrderBook> orderbookType = new TypeToken<OrderBook>() {
		};
		OrderBook orderbook = gson.fromJson(json, orderbookType.getType());
		return orderbook;
	}

	public static DepthCache deserializeDepthCache(String json) {
		Gson gson = new Gson();
		TypeToken<DepthCache> depthCacheType = new TypeToken<DepthCache>() {
		};
		DepthCache depthCache = gson.fromJson(json, depthCacheType.getType());
		return depthCache;
	}

	private WebSocketClient createWebSocketClient(String symbol) {
		String url = "wss://stream.binance.com:9443/ws/" + symbol.toLowerCase() + "@depth";
		return new WebSocketClient(URI.create(url)) {
			@Override
			public void onOpen(ServerHandshake serverHandshake) {
				// System.out.println("WebSocket connection established for " + symbol);
			}

			@Override
			public void onMessage(String message) {
				DepthCache depthCache = deserializeDepthCache(message);
				ArrayList<DepthCache> currentList = pendingList.get(symbol);
				currentList.add(depthCache);
				pendingList.put(symbol, currentList);
			}

			@Override
			public void onClose(int i, String s, boolean b) {
				// System.out.println("WebSocket connection closed for " + symbol);
			}

			@Override
			public void onError(Exception e) {
				e.printStackTrace();
			}
		};
	}

	private void openWebSocketStreams(String[] symbols) {
		for (String symbol : symbols) {
			if (!webSocketClients.containsKey(symbol)) {
				CompletableFuture.runAsync(() -> {
					try {
						WebSocketConnector connector = new WebSocketConnector();
						connector.connectWebSocket(symbol).get();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	@Component
	public class WebSocketConnector {
		public CompletableFuture<Void> connectWebSocket(String symbol) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			WebSocketClient client = createWebSocketClient(symbol);
			client.connect();
			future.complete(null);
			return future;
		}
	}

}
