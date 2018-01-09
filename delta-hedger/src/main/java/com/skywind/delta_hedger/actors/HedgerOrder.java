package com.skywind.delta_hedger.actors;

public class HedgerOrder {


    public int getOrderId() {
        return orderId;
    }

    public String getCode() {
        return code;
    }

    public Side getSide() {
        return side;
    }

    public double getPx() {
        return px;
    }

    public String getViewPx() {
        return viewPx;
    }

    public double getQty() {
        return outstandingQty;
    }

    public enum Side {
        BUY,
        SELL;
    }

    public enum State {
        ACTIVE,
        REJECTED,
        FILLED,
        CANCELLED;
    }

    private final String code;
    private final int orderId;
    private final Side side;
    private State state;
    private double outstandingQty;
    private double remainingQty;
    private double px;
    private String viewPx;

    public HedgerOrder(String code, int orderId, Side side, State state, double outstandingQty, double px, String viewPx) {
        this.code = code;
        this.orderId = orderId;
        this.side = side;
        this.state= state;
        this.outstandingQty = outstandingQty;
        this.remainingQty = outstandingQty;
        this.px = px;
        this.viewPx = viewPx;
    }

    public HedgerOrder(HedgerOrder other) {
        this.code = other.code;
        this.orderId = other.orderId;
        this.side = other.side;
        this.state = other.state;
        this.outstandingQty = other.outstandingQty;
        this.remainingQty = other.remainingQty;
        this.px = other.px;
    }

    private static final double ZERO_POS_TRESHOLD = 1e-6;

    public void onExecution(double qty) {
        remainingQty-= qty;
        if (Math.abs(remainingQty) < ZERO_POS_TRESHOLD) {
            state = State.FILLED;
        }
    }

    public void onFilled() {
        remainingQty = 0;
        state = State.FILLED;
    }

    public void onRejected() {
        state = State.REJECTED;
    }

    public void onCancelled() {
        state = State.CANCELLED;
    }

    public boolean isTerminalState() {
        return state == State.FILLED || state == State.REJECTED || state == State.CANCELLED;
    }

}
