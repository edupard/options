package com.skywind.delta_hedger.actors;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AmendmentProcess {

    private Integer placedOrder = null;
    private LinkedList<TargetOrder> targetOrderQueue;

    public void placeOrder(int orderId) {
        placedOrder = orderId;
    }

    public boolean isPlacedOrder(int orderId) {
        if (placedOrder != null) {
            return placedOrder == orderId;
        }
        return false;
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

    public void setTargetOrderQueue(List<TargetOrder> targetOrderQueue) {
        this.targetOrderQueue = new LinkedList<>(targetOrderQueue);
    }

    public TargetOrder getNextTargetOrder() {
        if (targetOrderQueue != null) {
            if (!targetOrderQueue.isEmpty()) {
                return targetOrderQueue.pollFirst();
            }
        }
        return null;
    }


    public enum Stage {
        CANCEL_ORDERS,
        WAIT_ALL_ORDERS_CANCELLED,
        CALL_PY_SCRIPT,
        WAIT_PY_SCRIPT_COMPLETION,
        SHOW_PLACE_CONFIRMATION,
        WAITING_PLACE_CONFIRMATION,
        PLACE_ORDERS,
        PLACE_NEXT_TARGET_ORDER,
        WAIT_TARGET_ORDER_STATE,
        COMPLETED,
        ORDER_REJECTED,
        PYTHON_FAILED,
        PYTHON_TIMEOUT,
        PYTHON_DATA_FAILURE,
        INTERRUPTED_BY_DISCONNECT,
    }

    private Stage currentStage;

    private final boolean cancelOrders;
    private final boolean callPyScript;
    private final boolean placeOrders;
    private final boolean confirmPlaceOrders;
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

    public boolean isConfirmPlaceOrders() {
        return confirmPlaceOrders && placeOrders;
    }

    public boolean includeIntoPositions(String underlyingCode) {
        return command.includeIntoPositions(underlyingCode);
    }

    public AmendmentProcess(HedgerActor.RunAmendmentProcess command, boolean cancelOrders, boolean callPyScript, boolean placeOrders, boolean confirmPlaceOrders) {
        this.command = command;
        this.cancelOrders = cancelOrders;
        this.callPyScript = callPyScript;
        this.placeOrders = placeOrders;
        this.confirmPlaceOrders = confirmPlaceOrders;
        currentStage = Stage.COMPLETED;
        if (cancelOrders) {
            currentStage = Stage.CANCEL_ORDERS;
        } else {
            if (callPyScript) {
                currentStage = Stage.CALL_PY_SCRIPT;
            } else {
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

    public boolean isFinished() {
        return currentStage == Stage.PYTHON_FAILED ||
                currentStage == Stage.ORDER_REJECTED ||
                currentStage == Stage.COMPLETED ||
                currentStage == Stage.PYTHON_TIMEOUT ||
                currentStage == Stage.PYTHON_DATA_FAILURE ||
                currentStage == Stage.INTERRUPTED_BY_DISCONNECT;
    }
}
