package com.gayantha.binance.demo.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class OrderBook {

    private String symbol;
    @SerializedName("lastUpdateId")
    private long lastUpdateId;
    private List<List<String>> bids;
    private List<List<String>> asks;

    // Getters and setters
    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    public List<List<String>> getBids() {
        return bids;
    }

    public void setBids(List<List<String>> bids) {
        this.bids = bids;
    }

    public List<List<String>> getAsks() {
        return asks;
    }

    public void setAsks(List<List<String>> asks) {
        this.asks = asks;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

}
