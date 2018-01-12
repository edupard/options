package com.skywind.delta_hedger.actors;

import java.util.HashSet;
import java.util.Set;

public class AmendmentProcess {

    private final Set<Integer> placedOrder = new HashSet<>();

    public void placeOrder(int orderId) {
        placedOrder.add(orderId);
    }

    public boolean isPlacedOrder(int orderId) {
        return placedOrder.contains(orderId);
    }

    private final Set<Integer> cancelledOrders = new HashSet<>();

    public void cancelOrder(int orderId) {
        cancelledOrders.add(orderId);
    }

    public boolean isCancelledOrder(int orderId) {
        return cancelledOrders.contains(orderId);
    }

    public boolean onCancelComplete(int orderId) {
        return cancelledOrders.remove(orderId);
    }

    public boolean isAllOrdersCancelled() {
        return cancelledOrders.isEmpty();
    }


    public enum Stage {
        CANCEL_ORDERS,
        WAIT_ALL_ORDERS_CANCELLED,
        CALL_PY_SCRIPT,
        WAIT_PY_SCRIPT_COMPLETION,
        PLACE_ORDERS,
        COMPLETED,
        CANCELLED,
        FAILED,
    }

    private Stage currentStage;

    private final boolean cancelOrders;
    private final boolean callPyScript;
    private final boolean placeOrders;
    private final HedgerActor.RunAmendmentProcess command;

    public HedgerActor.RunAmendmentProcess getCommand() {
        return command;
    }

    public boolean isCancelOrders() {
        return cancelOrders;
    }

    public boolean isCallPyScript() {
        return callPyScript;
    }

    public boolean isPlaceOrders() {
        return placeOrders;
    }

    public AmendmentProcess(HedgerActor.RunAmendmentProcess command, boolean cancelOrders, boolean callPyScript, boolean placeOrders) {
        this.command = command;
        this.cancelOrders = cancelOrders;
        this.callPyScript = callPyScript;
        this.placeOrders = placeOrders;
        currentStage = Stage.COMPLETED;
        if (cancelOrders) {
            currentStage = Stage.CANCEL_ORDERS;
        }
        else {
            if (callPyScript) {
                currentStage = Stage.CALL_PY_SCRIPT;
            }
            else {
                currentStage = Stage.PLACE_ORDERS;
            }
        }
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }
}
