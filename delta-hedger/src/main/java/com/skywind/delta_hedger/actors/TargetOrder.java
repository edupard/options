package com.skywind.delta_hedger.actors;

import java.util.LinkedList;
import java.util.List;

public class TargetOrder {

    private final int idx;
    private final String code;
    private final double px;
    private final String viewPx;
    private final double qty;
    private final String orderType;


    private Integer orderId;
    private List<Integer> additionalOrders = new LinkedList<>();


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
