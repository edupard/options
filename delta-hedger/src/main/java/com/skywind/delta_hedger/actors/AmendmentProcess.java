package com.skywind.delta_hedger.actors;

import com.ib.client.OrderStatus;

import java.util.*;
import java.util.stream.Collectors;


public class AmendmentProcess {
    private Map<Integer, TargetOrder> placedOrders = new HashMap<>();

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

    public TargetOrder getTargetOrder(int orderId) {
        return placedOrders.get(orderId);
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

    public void processTargetOrders(List<TargetOrder> targetOrders) {
        List<TargetOrder> orderedTargetOrders = targetOrders.stream()
                .filter((to) -> Math.abs(to.getQty()) > 0)
                .sorted(Comparator.comparing((to) -> to.getIdx()))
                .collect(Collectors.toList());
        this.targetOrderQueue.addAll(orderedTargetOrders);
        this.targetOrders.addAll(orderedTargetOrders);
    }

    public void onOrderPlaced(int orderId, TargetOrder to, PlacedOrderType ot) {
        placedOrders.put(orderId, to);
        to.onOrderSubmitted(orderId, ot);
    }

    public List<AdditionalOrder> onOrderStatus(int orderId, OrderStatus os) {
        List<AdditionalOrder> result = new LinkedList<>();

        TargetOrder filledTo = getTargetOrder(orderId);
        if (filledTo == null) {
            return result;
        }


        boolean buy = filledTo.getQty() > 0;
        List<TargetOrder> oppositeOrders = buy ?
                targetOrders.stream()
                        .filter((to) -> to.getCode().equals(filledTo.getCode()))
                        .filter((to) -> to.getQty() < 0)
                        .sorted(Comparator.comparing((to) -> -to.getPx()))
                        .collect(Collectors.toList())
                :
                targetOrders.stream()
                        .filter((to) -> to.getCode().equals(filledTo.getCode()))
                        .filter((to) -> to.getQty() > 0)
                        .sorted(Comparator.comparing((to) -> to.getPx()))
                        .collect(Collectors.toList());

        if (filledTo.isMainOrder(orderId)) {
            TargetOrder.OrderState mainOrderState = filledTo.getMainOrderState();
            if (os == OrderStatus.Filled && mainOrderState.getOrderStatus() != OrderStatus.Filled) {
                if (!oppositeOrders.isEmpty()) {
                    result.add(new AdditionalOrder(PlacedOrderType.ADDITIONAL, oppositeOrders.get(0), -filledTo.getQty()));
                }
                for (TargetOrder to : oppositeOrders) {
                    TargetOrder.OrderState orderState = to.getMainOrderState();
                    if (orderState != null) {
                        if (orderState.getOrderStatus() == OrderStatus.Filled) {
                            result.add(new AdditionalOrder(PlacedOrderType.MAIN, to, to.getQty()));
                        }
                    }
                }
            }
            mainOrderState.setOrderStatus(os);
        }
        else {
            TargetOrder.OrderState additionalOrderState = filledTo.getAdditionalOrderState(orderId);
            if (additionalOrderState != null) {
                additionalOrderState.setOrderStatus(os);
            }
        }
        return result;
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

    public static boolean isTerminalStage(Stage currentStage) {
        return currentStage == Stage.PYTHON_FAILED ||
                currentStage == Stage.ORDER_REJECTED ||
                currentStage == Stage.COMPLETED ||
                currentStage == Stage.PYTHON_TIMEOUT ||
                currentStage == Stage.PYTHON_DATA_FAILURE ||
                currentStage == Stage.HMD_FAILURE ||
                currentStage == Stage.INTERRUPTED_BY_DISCONNECT;
    }

    public boolean isFinished() {
        return isTerminalStage(currentStage);
    }
}
