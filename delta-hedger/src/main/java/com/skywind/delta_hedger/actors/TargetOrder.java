package com.skywind.delta_hedger.actors;

import java.util.LinkedList;
import java.util.List;

public class TargetOrder {

    public static enum State {
        NOT_SUBMITED_YET,
        SUBMITTED,
        REJECTED,
        FILLED,
    }

    private final int idx;
    private final String code;
    private final double px;
    private final String viewPx;
    private final double qty;
    private final String orderType;


    private State state = State.NOT_SUBMITED_YET;
    private Integer orderId;
    private List<Integer> additionalOrders = new LinkedList<>();

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public TargetOrder(int idx, String code, double px, String viewPx, double qty, String orderType) {
        this.idx = idx;
        this.code = code;
        this.px = px;
        this.viewPx = viewPx;
        this.qty = qty;
        this.orderType = orderType;
    }

    public int getIdx() {
        return idx;
    }

    public String getCode() {
        return code;
    }

    public double getPx() {
        return px;
    }

    public String getViewPx() {
        return viewPx;
    }

    public double getQty() {
        return qty;
    }

    public String getOrderType() {
        return orderType;
    }
}
