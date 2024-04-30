package com.gayantha.binance.demo.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class DepthCache {

    @SerializedName("e")
    private String eventType;

    @SerializedName("E")
    private long eventTime;

    @SerializedName("s")
    private String symbol;

    @SerializedName("U")
    private long firstUpdateId;

    @SerializedName("u")
    private long finalUpdateId;

    @SerializedName("b")
    private List<List<String>> bids;

    @SerializedName("a")
    private List<List<String>> asks;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public long getFirstUpdateId() {
        return firstUpdateId;
    }

    public void setFirstUpdateId(long firstUpdateId) {
        this.firstUpdateId = firstUpdateId;
    }

    public long getFinalUpdateId() {
        return finalUpdateId;
    }

    public void setFinalUpdateId(long finalUpdateId) {
        this.finalUpdateId = finalUpdateId;
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

}
