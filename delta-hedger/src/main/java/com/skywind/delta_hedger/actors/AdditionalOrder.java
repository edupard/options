package com.skywind.delta_hedger.actors;

public class AdditionalOrder {

    public enum OrderType {
        MAIN,
        ADDITIONAL
    }

    private final OrderType orderType;
    private final TargetOrder targetOrder;
    private final double qty;

    public AdditionalOrder(OrderType orderType, TargetOrder targetOrder, double qty) {
        this.orderType = orderType;
        this.targetOrder = targetOrder;
        this.qty = qty;
    }
}
