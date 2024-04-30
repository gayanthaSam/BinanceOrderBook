package com.gayantha.binance.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gayantha.binance.demo.model.OrderBookManager;
import com.google.gson.Gson;

@Service
public class OrderBookService {

    private final Map<String, OrderBookManager> orderBooks;
    private final Gson gson;

    public OrderBookService() {
        orderBooks = new HashMap<>();
        orderBooks.put("BTCUSDT", new OrderBookManager());
        orderBooks.put("ETHUSDT", new OrderBookManager());
        gson = new Gson();
    }

    public OrderBookManager getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }

    public void updateOrderbook(String symbol, List<List<String>> newBids, List<List<String>> newAsks) {
        OrderBookManager orderBook = orderBooks.get(symbol);
        orderBook.updateOrderBook(newBids, newAsks);
    }

    public String[] getAllSymbols() {
        return orderBooks.keySet().toArray(new String[0]);
    }

}
