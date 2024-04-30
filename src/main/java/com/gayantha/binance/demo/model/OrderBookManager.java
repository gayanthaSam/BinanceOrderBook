package com.gayantha.binance.demo.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class OrderBookManager {

    private NavigableMap<BigDecimal, BigDecimal> bids;
    private NavigableMap<BigDecimal, BigDecimal> asks;

    public OrderBookManager() {
        bids = new TreeMap<>(Collections.reverseOrder());
        asks = new TreeMap<>();
    }

    public void updateOrderBook(List<List<String>> newBids, List<List<String>> newAsks) {
        updateBids(newBids);
        updateAsks(newAsks);
    }

    private void updateBids(List<List<String>> newBids) {
        bids.clear();
        for (List<String> bid : newBids) {
            BigDecimal price = new BigDecimal(bid.get(0));
            BigDecimal quantity = new BigDecimal(bid.get(1));
            bids.put(price, quantity);
        }
    }

    private void updateAsks(List<List<String>> newAsks) {
        asks.clear();
        for (List<String> bid : newAsks) {
            BigDecimal price = new BigDecimal(bid.get(0));
            BigDecimal quantity = new BigDecimal(bid.get(1));
            asks.put(price, quantity);
        }
    }

    public void printOrderBook(String symbol, int depth) {
        String topic1 = "Side";
        String topic2 = "Price(USDT)";
        String topic3 = "Sum(USDT)";
        String topic4 = "Total(USDT)";
        String deco = "===========";
        String BUY = "Buy";
        String SELL = "Sell";
        String format = "%-15s%-20s%-20s%-20s%n";
        System.out.println(symbol);
        System.out.println("Buy Order");
        System.out.printf(format, topic1, topic2, topic3, topic4);
        System.out.printf(format, deco, deco, deco, deco);
        printOrderbookEntries(bids, format, BUY, depth);
        System.out.println("Sell Order");
        System.out.printf(format, topic1, topic2, topic3, topic4);
        System.out.printf(format, deco, deco, deco, deco);
        printOrderbookEntries(asks, format, SELL, depth);
    }

    private void printOrderbookEntries(NavigableMap<BigDecimal, BigDecimal> entries, String format, String type,
            int depth) {
        int counter = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, BigDecimal> entry : entries.entrySet()) {
            if (counter > depth - 1) {
                break;
            }
            counter++;
            BigDecimal price = entry.getKey();
            BigDecimal quantity = entry.getValue();
            BigDecimal total = price.multiply(quantity);
            sum = sum.add(total);
            System.out.printf(format, type + counter, formatBigDecimal(price, 2), formatBigDecimal(quantity, 5),
                    formatBigDecimal(total, 7), formatBigDecimal(sum, 7));
        }
    }

    private String formatBigDecimal(BigDecimal value, int decimals) {
        DecimalFormat formatter = null;
        switch (decimals) {
            case 2:
                formatter = new DecimalFormat("#,###.##");
                break;
            case 5:
                formatter = new DecimalFormat("#,###.######");
                break;
            case 7:
                formatter = new DecimalFormat("#,###.########");
                break;
            default:
                break;
        }
        return formatter.format(value);
    }

}
