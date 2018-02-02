package com.skywind.delta_hedger.actors;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AmendmentProcess {

    private Integer placedOrder = null;
    private LinkedList<TargetOrder> targetOrderQueue = new LinkedList<>();
    private LinkedList<TargetOrder> targetOrders = new LinkedList<>();

    private Set<Integer> histReqIds = new HashSet<>();


    public void addHistRequest(int reqId) {
        histReqIds.add(reqId);
    }

    public boolean isHistReq(int reqId) {
        return histReqIds.contains(reqId);
    }

    public void onHistReqComplete(int reqId) {
        histReqIds.remove(reqId);
    }

    public boolean isAllHistReqCompleted() {
        return histReqIds.isEmpty();
    }

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
        this.targetOrderQueue.addAll(targetOrderQueue);
        this.targetOrders.addAll(targetOrderQueue);
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
        REFRESH_TIME_BARS,
        WAIT_TIME_BARS,
        CANCEL_ORDERS,
        SHOW_CANCEL_CONFIRMATION,
        WAITING_CANCEL_CONFIRMATION,
        CANCELLING,
        WAIT_ALL_ORDERS_CANCELLED,
        CALL_PY_SCRIPT,
        WAIT_PY_SCRIPT_COMPLETION,
        PLACE_ORDERS,
        SHOW_PLACE_CONFIRMATION,
        WAITING_PLACE_CONFIRMATION,
        PLACE_NEXT_TARGET_ORDER,
        WAIT_TARGET_ORDER_STATE,
        COMPLETED,
        ORDER_REJECTED,
        PYTHON_FAILED,
        PYTHON_TIMEOUT,
        HMD_FAILURE,
        PYTHON_DATA_FAILURE,
        INTERRUPTED_BY_DISCONNECT,
    }

    private Stage currentStage;

    private final HedgerActor.RunAmendmentProcess command;

    public HedgerActor.RunAmendmentProcess getCommand() {
        return command;
    }

    public boolean includeIntoPositions(String underlyingCode) {
        return command.includeIntoPositions(underlyingCode);
    }

    private volatile boolean placeAllowed = false;

    public void allowPlace() {
        placeAllowed = true;
    }

    public boolean isPlaceAllowed() {
        return placeAllowed;
    }

    public AmendmentProcess(HedgerActor.RunAmendmentProcess command) {
        this.command = command;
        currentStage = Stage.REFRESH_TIME_BARS;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    private boolean resultSent = false;

    public boolean isResultSent() {
        return resultSent;
    }

    public void setResultSent() {
        resultSent = true;
    }

    public LinkedList<TargetOrder> getTargetOrders() {
        return targetOrders;
    }

    public boolean isFinished() {
        return currentStage == Stage.PYTHON_FAILED ||
                currentStage == Stage.ORDER_REJECTED ||
                currentStage == Stage.COMPLETED ||
                currentStage == Stage.PYTHON_TIMEOUT ||
                currentStage == Stage.PYTHON_DATA_FAILURE ||
                currentStage == Stage.HMD_FAILURE ||
                currentStage == Stage.INTERRUPTED_BY_DISCONNECT;
    }
}
