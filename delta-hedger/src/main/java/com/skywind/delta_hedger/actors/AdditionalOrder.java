package com.skywind.delta_hedger.actors;

public class AdditionalOrder {

    private final PlacedOrderType placedOrderType;
    private final TargetOrder targetOrder;
    private final double qty;

    public PlacedOrderType getPlacedOrderType() {
        return placedOrderType;
    }

    public TargetOrder getTargetOrder() {
        return targetOrder;
    }

    public double getQty() {
        return qty;
    }

    public AdditionalOrder(PlacedOrderType placedOrderType, TargetOrder targetOrder, double qty) {
        this.placedOrderType = placedOrderType;
        this.targetOrder = targetOrder;
        this.qty = qty;
    }
}
