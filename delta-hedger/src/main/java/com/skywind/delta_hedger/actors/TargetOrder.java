package com.skywind.delta_hedger.actors;

import com.ib.client.OrderStatus;

import java.util.LinkedList;
import java.util.List;

public class TargetOrder {

    public boolean isInitialOrder(int orderId) {
        return mainOrderState != null && mainOrderState.orderId == orderId && mainOrderState.placedOrderType == PlacedOrderType.INITIAL;
    }

    public boolean isMainOrder(int orderId) {
        return mainOrderState != null && mainOrderState.orderId == orderId;
    }

    public OrderState getMainOrderState() {
        return mainOrderState;
    }

    public OrderState getAdditionalOrderState(int orderId) {
        for (OrderState os: additionalOrderStates){
            if (os.orderId == orderId) {
                return os;
            }
        }
        return null;
    }

    public static class OrderState {

        private final PlacedOrderType placedOrderType;
        private final int orderId;
        private OrderStatus orderStatus = OrderStatus.Unknown;

        public OrderState(Integer orderId, PlacedOrderType placedOrderType) {
            this.placedOrderType = placedOrderType;
            this.orderId = orderId;
        }

        public PlacedOrderType getPlacedOrderType() {
            return placedOrderType;
        }

        public int getOrderId() {
            return orderId;
        }

        public OrderStatus getOrderStatus() {
            return orderStatus;
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            this.orderStatus = orderStatus;
        }
    }

    private final int idx;
    private final String code;
    private final double px;
    private final String viewPx;
    private final double qty;
    private final String orderType;


    private OrderState mainOrderState = null;
    private List<OrderState> additionalOrderStates = new LinkedList<>();

    public void onOrderSubmitted(int orderId, PlacedOrderType ot) {
        switch (ot) {
            case INITIAL:
            case MAIN:
                mainOrderState = new OrderState(orderId, ot);
                break;
            case ADDITIONAL:
                additionalOrderStates.add(new OrderState(orderId, ot));
                break;
        }

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
