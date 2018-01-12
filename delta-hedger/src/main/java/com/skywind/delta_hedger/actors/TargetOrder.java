package com.skywind.delta_hedger.actors;

public class TargetOrder {

    private final String code;
    private final double px;
    private final String viewPx;
    private final double qty;
    private final String orderType;

    public TargetOrder(String code, double px, String viewPx, double qty, String orderType) {
        this.code = code;
        this.px = px;
        this.viewPx = viewPx;
        this.qty = qty;
        this.orderType = orderType;
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
