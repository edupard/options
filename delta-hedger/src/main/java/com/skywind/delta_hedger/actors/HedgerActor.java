package com.skywind.delta_hedger.actors;

import akka.actor.*;
import com.ib.client.*;
import com.skywind.delta_hedger.ui.MainController;
import com.skywind.ib.IbGateway;
import com.skywind.ib.Utils;
import com.skywind.trading.spring_akka_integration.EmailActor;
import com.skywind.trading.spring_akka_integration.MessageSentToExactActorInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.skywind.delta_hedger.actors.HedgerActor.BEAN_NAME;

@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HedgerActor extends AbstractActor {
    public final static String BEAN_NAME = "hedgerActor";
    private final Logger LOGGER = LoggerFactory.getLogger(HedgerActor.class);

    private final UUID actorId = UUID.randomUUID();
    private final IbGateway ibGateway;

    @Value("${ib.host}")
    private String host;


    @Value("${ib.port}")
    private int port;

    @Value("${ib.clientId}")
    private int clientId;

    @Value("${account}")
    private String account;

    @Value("${exchange}")
    private String exchange;

    @Value("${ccy}")
    private String ccy;

    @Value("${underlyings}")
    private String underlyings;

    @Autowired
    private MainController controller;

    @Autowired
    private ActorSystem actorSystem;


    private ActorSelection emailActorSelection;


    public HedgerActor() {
        ibGateway = new IbGateway(self(), actorId);
    }


    public static final class ProcessPendingAmendmentProcessQueueTask {
    }

    public static final class Start {
    }

    public static final class ReloadPositions {

    }

    public static final class RefreshTimebars {

    }

    public static final class RefreshDaysToExpiration {

    }

    public static final class ShowPortfolio {

    }

    public static final class RunAmendmentProcess {

        private final Set<String> targetUnderlyings;
        private final String params;
        private final boolean manual;

        public String getParams() {
            return params;
        }

        public boolean includeIntoPositions(String underlyingCode) {
            // no filter
            if (targetUnderlyings.isEmpty()) {
                return true;
            }
            return targetUnderlyings.contains(underlyingCode);
        }

        public RunAmendmentProcess(Set<String> targetUnderlyings, String params, boolean manual) {
            this.targetUnderlyings = targetUnderlyings;
            this.params = params;
            this.manual = manual;
        }

        public boolean isManual() {
            return manual;
        }
    }

    public static final class PythonScriptResult extends MessageSentToExactActorInstance {

        public enum Result {
            SUCCESS,
            DATA_FAILURE,
            FAILURE,
            TIMEOUT
        }

        private final Result result;

        private final String folder;

        public PythonScriptResult(UUID actorId, Result result, String folder) {
            super(actorId);
            this.result = result;
            this.folder = folder;
        }

        public Result getResult() {
            return result;
        }

        public String getFolder() {
            return folder;
        }
    }


    public static final class RefreshOpenOrders {
    }

    public static final class PosSelectionChanged {
        private final String localSymbol;
        private final boolean selected;

        public PosSelectionChanged(String localSymbol, boolean selected) {
            this.localSymbol = localSymbol;
            this.selected = selected;
        }

        public String getLocalSymbol() {
            return localSymbol;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public static final class IrChanged {
        private final String localSymbol;
        private final double ir;

        public IrChanged(String localSymbol, double ir) {
            this.localSymbol = localSymbol;
            this.ir = ir;
        }
    }

    public static final class VolChanged {
        private final String localSymbol;
        private final double vol;

        public VolChanged(String localSymbol, double vol) {
            this.localSymbol = localSymbol;
            this.vol = vol;
        }
    }


    public static final class Restart extends MessageSentToExactActorInstance {

        public Restart(UUID actorId) {
            super(actorId);
        }

    }

    public static final class PlaceConfirmation {
        private final boolean proceed;

        public PlaceConfirmation(boolean proceed) {
            this.proceed = proceed;
        }

        public boolean isProceed() {
            return proceed;
        }
    }

    public static final class CancelConfirmation {
        private final boolean proceed;

        public CancelConfirmation(boolean proceed) {
            this.proceed = proceed;
        }

        public boolean isProceed() {
            return proceed;
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Start.class, m -> {
                    onStart(m);
                })
                .match(Restart.class, m -> {
                    onRestart(m);
                })
                .match(ReloadPositions.class, m -> {
                    onReloadPositions(m);
                })
                .match(IbGateway.ConnectAck.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onConnected(m);
                    }
                })
                .match(IbGateway.NextValidId.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onNextValidId(m);
                    }
                })
                .match(IbGateway.ExecDetails.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onExecDetails(m);
                    }
                })
                .match(IbGateway.ExecDetailsEnd.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onExecDetailsEnd(m);
                    }
                })
                .match(IbGateway.Position.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onPosition(m);
                    }
                })
                .match(IbGateway.PositionEnd.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onPositionEnd(m);
                    }
                })
                .match(IbGateway.ContractDetailsMsg.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onContractDetails(m);
                    }
                })
                .match(PosSelectionChanged.class, m -> {
                    onPosSelectionChanged(m);
                })
                .match(IrChanged.class, m -> {
                    onIrChanged(m);
                })
                .match(VolChanged.class, m -> {
                    onVolChanged(m);
                })
                .match(RefreshTimebars.class, m -> {
                    onRefreshTimebars(m);
                })
                .match(IbGateway.HistDataMsg.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onHistDataMsg(m);
                    }
                })
                .match(IbGateway.HistDataUpdateMsg.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onHistDataUpdateMsg(m);
                    }
                })
                .match(IbGateway.HistDataEndMsg.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onHistDataEndMsg(m);
                    }
                })
                .match(IbGateway.ErrorWithCode.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onErrorWithCode(m);
                    }
                })
                .match(IbGateway.ErrorWithException.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onErrorWithException(m);
                    }
                })
                .match(IbGateway.ErrorWithMessage.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onErrorWithMessage(m);
                    }
                })
                .match(IbGateway.OpenOrder.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onOpenOrder(m);
                    }
                })
                .match(IbGateway.OpenOrderEnd.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onOpenOrderEnd(m);
                    }
                })
                .match(IbGateway.OrderStatus.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onOrderStatus(m);
                    }
                })
                .match(RefreshOpenOrders.class, m -> {
                    onRefreshOpenOrders(m);
                })
                .match(RunAmendmentProcess.class, m -> {
                    onRunAmendmentProcess(m);
                })
                .match(ShowPortfolio.class, m -> {
                    onShowPortfolio(m);
                })
                .match(PythonScriptResult.class, m -> {
                    onPythonScriptResult(m);
                })
                .match(PlaceConfirmation.class, m -> {
                    onPlaceConfirmation(m);
                })
                .match(CancelConfirmation.class, m -> {
                    onCancelConfirmation(m);
                })
                .match(RefreshDaysToExpiration.class, m -> {
                    onRefreshDaysToExpiration(m);
                })
                .match(ProcessPendingAmendmentProcessQueueTask.class, m -> {
                    onProcessPendingAmendmentProcessQueueTask(m);
                })
                .matchAny(m -> recievedUnknown(m))
                .build();
    }

    private void onProcessPendingAmendmentProcessQueueTask(ProcessPendingAmendmentProcessQueueTask m) {
        processPendingAmendmentProcessQueue();
    }

    private void onRefreshDaysToExpiration(RefreshDaysToExpiration m) {
        MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);
        for (Map.Entry<String, Position> e : positions.entrySet()) {
            uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, e.getKey(), e.getValue());
        }
        controller.onPositionsUpdate(uiUpdates);
    }


    // should not be called at all
    private void onErrorWithMessage(IbGateway.ErrorWithMessage m) {
        throw new RuntimeException(m.getErrorMsg());
    }

    // Handles errors generated within the API itself. If an exception is thrown within the API code it will be notified here. Possible cases include errors while reading the information from the socket or even mishandling at EWrapper's implementing class.
    private void onErrorWithException(IbGateway.ErrorWithException m) {
        throw new RuntimeException(m.getReason());
    }


    public final static Set<Integer> DISCONNECTED_FROM_IB_ERROR_CODES = new HashSet<>();
    public final static Set<Integer> RECONNECTED_TO_IB_ERROR_CODES = new HashSet<>();
    public final static Set<Integer> FATAL_ERROR_CODES = new HashSet<>();

    public final static Set<Integer> HMDS_ERROR_CODES = new HashSet<>();

    static {
        DISCONNECTED_FROM_IB_ERROR_CODES.add(1100);
        DISCONNECTED_FROM_IB_ERROR_CODES.add(2110);

        RECONNECTED_TO_IB_ERROR_CODES.add(1101);
        RECONNECTED_TO_IB_ERROR_CODES.add(1102);

        FATAL_ERROR_CODES.add(1300);
        FATAL_ERROR_CODES.add(501);
        FATAL_ERROR_CODES.add(502);
        FATAL_ERROR_CODES.add(503);
        FATAL_ERROR_CODES.add(504);

        HMDS_ERROR_CODES.add(162);
        HMDS_ERROR_CODES.add(165);
        HMDS_ERROR_CODES.add(166);
        HMDS_ERROR_CODES.add(366);
        HMDS_ERROR_CODES.add(386);
    }


    private boolean ibConnection = false;
    private boolean apiConnection = false;

    private Set<String> underlyingsFilter = new HashSet<>();

    @Autowired
    private Environment env;

    private static final String DEFAULT_VALUE = "";

    public final static class DurationAndSize {
        private final String size;
        private final String duration;

        public DurationAndSize(String size, String duration) {
            this.size = size;
            this.duration = duration;
        }
    }

    private final List<DurationAndSize> barsToRequest = new LinkedList<>();

    @PostConstruct
    public void postConstruct() {
        for (int i = 0; i < 100; ++i) {
            String size = env.getProperty(String.format("bar.%d.size", i), DEFAULT_VALUE);

            if (size.equals(DEFAULT_VALUE)) {
                continue;
            }
            String duration = env.getProperty(String.format("bar.%d.duration", i));
            barsToRequest.add(new DurationAndSize(size, duration));
        }

        String[] split = underlyings.split(",");
        for (String u : split) {
            underlyingsFilter.add(u);
        }
        controller.onApiConnection(apiConnection);
        controller.onIbConnection(ibConnection);
    }

    private void onErrorWithCode(IbGateway.ErrorWithCode m) {
        if (DISCONNECTED_FROM_IB_ERROR_CODES.contains(m.getErrorCode())) {
            if (ibConnection) {
                ibConnection = false;
                controller.onIbConnection(ibConnection);
                String message = "Options: disconnected from IB";
                emailActorSelection.tell(new EmailActor.Email(message, message), self());
            }
        }
        if (RECONNECTED_TO_IB_ERROR_CODES.contains(m.getErrorCode())) {
            if (!ibConnection) {
                ibConnection = true;
                controller.onIbConnection(ibConnection);
                String message = "Options: reconnected to IB";
                emailActorSelection.tell(new EmailActor.Email(message, message), self());
                requestExecutions();
//                refreshTimebars(true);

                getContext().system().scheduler().scheduleOnce(
                        Duration.create(2, TimeUnit.SECONDS),
                        self(),
                        new ProcessPendingAmendmentProcessQueueTask(),
                        getContext().dispatcher(),
                        self());
            }
        }

        if (FATAL_ERROR_CODES.contains(m.getErrorCode())) {
            String message = String.format("%d %s", m.getErrorCode(), m.getErrorMsg());
            emailActorSelection.tell(new EmailActor.Email("Options: fatal error", message), self());
            throw new FatalException();
        }

        if (m.getErrorCode() == 2106) {
//            refreshTimebars(true);
        }
        if (m.getErrorCode() == 507) {
            String message = "Options: ib socket closed";
            emailActorSelection.tell(new EmailActor.Email(message, message), self());
            controller.onApiConnection(false);
            controller.onIbConnection(false);
            throw new RuntimeException("Ib socket closed");
        }

        if (HMDS_ERROR_CODES.contains(m.getErrorCode())) {
            if (amendmentProcess != null && !amendmentProcess.isFinished()) {
                try {
                    String message = "Options: HMD request failed";
                    emailActorSelection.tell(new EmailActor.Email(message, message), self());
                    if (amendmentProcess.getCurrentStage() == AmendmentProcess.Stage.WAIT_TIME_BARS && amendmentProcess.isHistReq(m.getId())) {
                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.HMD_FAILURE);
                    }
                } finally {
                    doNextAmendmentProcessStage();
                }
            }
        }

        if (m.getErrorCode() == 200) {
            if (amendmentProcess != null && !amendmentProcess.isFinished()) {
                try {
                    switch (amendmentProcess.getCurrentStage()) {
                        case WAIT_TARGET_ORDER_STATE:
                            if (amendmentProcess.isPlacedOrder(m.getId())) {
                                // Send email
                                String message = "Options: amendment process interrupted by disconnect";
                                emailActorSelection.tell(new EmailActor.Email(message, message), self());
                                // Set current stage
                                amendmentProcess.setCurrentStage(AmendmentProcess.Stage.INTERRUPTED_BY_DISCONNECT);
                                // Schedule new amendment process
                                pendingProcesses.add(amendmentProcess.getCommand());
                            }
                            break;
                        case WAIT_TIME_BARS:
                            // Send email
                            String message = "Options: amendment process interrupted by disconnect during HMD request";
                            emailActorSelection.tell(new EmailActor.Email(message, message), self());
                            // Set current stage
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.INTERRUPTED_BY_DISCONNECT);
                            // Schedule new amendment process
                            pendingProcesses.add(amendmentProcess.getCommand());
                    }


                } finally {
                    doNextAmendmentProcessStage();
                }
            }
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), (Throwable t) -> SupervisorStrategy.escalate());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public static final long RESTART_INTERVAL_SEC = 5l;

    private Cancellable updateDaysToExpirationTask;

    @Override
    public void preStart() throws Exception {
        super.preStart();

        updateDaysToExpirationTask = getContext().system().scheduler().schedule(
                Duration.create(1l, TimeUnit.MINUTES),
                Duration.create(1l, TimeUnit.MINUTES),
                self(),
                new RefreshDaysToExpiration(),
                getContext().dispatcher(),
                self());


        emailActorSelection = actorSystem.actorSelection("/user/email");
    }

    @Override
    public void postRestart(Throwable thrwbl) throws Exception {
        super.postRestart(thrwbl);

        getContext().system().scheduler().scheduleOnce(Duration.create(RESTART_INTERVAL_SEC, TimeUnit.SECONDS),
                self(),
                new Restart(actorId),
                getContext().dispatcher(),
                self());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();

        if (updateDaysToExpirationTask != null) {
            updateDaysToExpirationTask.cancel();
        }
        ibGateway.disconnect();
    }

    private void onStart(Start m) {
        start();
    }

    private void start() {
        positions.clear();
        for (Map.Entry<String, Position> e : StorageUtils.readPositions().entrySet()) {
            String code = e.getKey();
            Position p = e.getValue();
            if (underlyingsFilter.contains(p.getContract().symbol())) {
                positions.put(code, p);
            }
        }
        MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(true);
        for (Map.Entry<String, Position> e : positions.entrySet()) {
            uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, e.getKey(), e.getValue());
        }
        for (Trade t : StorageUtils.readTrades()) {
            if (positions.containsKey(t.getLocalSymbol())) {
                Position p = positions.get(t.getLocalSymbol());
                p.setLastTrade(t);
            }
            processedTrades.add(t.getExecId());
        }
        controller.onPositionsUpdate(uiUpdates);

        ibGateway.connect(host, port, clientId);
    }

    public void onRestart(Restart m) {
        start();
    }

    private void recievedUnknown(Object m) {
    }

    private int nextOrderId = -1;

    private void onNextValidId(IbGateway.NextValidId m) {
        nextOrderId = m.getNextValidId();
        onPositionsUpdate();
        requestExecutions();
    }

    @Value("${python.path}")
    private String pythonPath;

    @Value("${script.folder}")
    private String scriptFolder;

    private List<TargetOrder> targetOrders = new LinkedList<>();

    private AmendmentProcess amendmentProcess = null;

    private void onCancelConfirmation(CancelConfirmation m) {
        if (amendmentProcess != null) {
            try {
                if (amendmentProcess.getCurrentStage() == AmendmentProcess.Stage.WAITING_CANCEL_CONFIRMATION) {
                    if (m.isProceed()) {
                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CANCELLING);

                    } else {
                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                    }
                }
            } finally {
                doNextAmendmentProcessStage();
            }
        }
    }

    private void onPlaceConfirmation(PlaceConfirmation m) {
        if (amendmentProcess != null) {
            try {
                if (amendmentProcess.getCurrentStage() == AmendmentProcess.Stage.WAITING_PLACE_CONFIRMATION) {
                    if (m.isProceed()) {
                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PLACE_NEXT_TARGET_ORDER);
                    } else {
                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                    }
                }
            } finally {
                doNextAmendmentProcessStage();
            }
        }
    }

    private void doNextAmendmentProcessStage() {
        if (amendmentProcess == null) {
            return;
        }
        try {
            boolean stopCycle = false;
            while (!stopCycle) {
                if (amendmentProcess.isFinished()) {
                    stopCycle = true;
                    continue;
                }
                switch (amendmentProcess.getCurrentStage()) {
                    case REFRESH_TIME_BARS:
                        if (refreshTimebars(true)) {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.WAIT_TIME_BARS);
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CANCEL_ORDERS);
                        }
                        break;
                    case CANCEL_ORDERS:
                        if (amendmentProcess.isCancelOrders()) {
                            if (controller.isStarted()) {
                                if (!openOrders.isEmpty()) {
                                    if (amendmentProcess.isConfirmCancelOrders()) {
                                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.SHOW_CANCEL_CONFIRMATION);
                                    } else {
                                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CANCELLING);
                                    }
                                }
                                else {
                                    amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                                }
                            }
                            else{
                                amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                            }
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                        }
                        break;
                    case SHOW_CANCEL_CONFIRMATION:
                        if (controller.isStarted()) {
                            controller.onConfirmCancelOrders();
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.WAITING_CANCEL_CONFIRMATION);
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                        }
                        break;
                    case CANCELLING:
                        if (controller.isStarted()) {
                            cancelAllOrders();
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.WAIT_ALL_ORDERS_CANCELLED);
                        }
                        else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                        }
                        break;

                    case CALL_PY_SCRIPT:
                        if (amendmentProcess.isCallPyScript()) {
                            runPython();
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.WAIT_PY_SCRIPT_COMPLETION);
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PLACE_ORDERS);
                        }
                        break;

                    case PLACE_ORDERS:
                        if (amendmentProcess.isPlaceOrders()) {
                            if (controller.isStarted()) {
                                if (!amendmentProcess.getTargetOrders().isEmpty()) {
                                    if (amendmentProcess.isConfirmPlaceOrders()) {
                                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.SHOW_PLACE_CONFIRMATION);
                                    } else {
                                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PLACE_NEXT_TARGET_ORDER);
                                    }
                                } else {
                                    amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                                }
                            } else {
                                amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                            }
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                        }
                        break;
                    case SHOW_PLACE_CONFIRMATION:
                        if (controller.isStarted()) {
                            controller.onConfirmOrderPlace();
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.WAITING_PLACE_CONFIRMATION);
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                        }
                        break;
                    case PLACE_NEXT_TARGET_ORDER:
                        if (controller.isStarted()) {
                            TargetOrder to = amendmentProcess.getNextTargetOrder();
                            if (to != null) {
                                amendmentProcess.placeOrder(placeTargetOrder(to));
                                amendmentProcess.setCurrentStage(AmendmentProcess.Stage.WAIT_TARGET_ORDER_STATE);
                            } else {
                                amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                            }
                        } else {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.COMPLETED);
                        }
                        break;
                    default:
                        stopCycle = true;
                        break;
                }
            }
        } finally {
            controller.onProgress(amendmentProcess.getCurrentStage());
            processPendingAmendmentProcessQueue();

            if (amendmentProcess.isFinished() && !amendmentProcess.isResultSent()) {
                amendmentProcess.setResultSent();
                StringBuilder sb = new StringBuilder();
                for (TargetOrder to : amendmentProcess.getTargetOrders()) {
                    sb.append(String.format("%d %s %s %.0f %s", to.getIdx(), to.getCode(), to.getOrderType(), to.getQty(), to.getViewPx()));
                    sb.append(System.lineSeparator());
                }

                String message = String.format("Options: %s amendment procedure %s", amendmentProcess.getCommand().getParams(), amendmentProcess.getCurrentStage().toString());
                emailActorSelection.tell(new EmailActor.Email(message, sb.toString()), self());
            }
        }
    }

    private void cancelAllOrders() {
        for (Integer orderId : openOrders.keySet()) {
            ibGateway.getClientSocket().cancelOrder(orderId);
            amendmentProcess.cancelOrder(orderId);
        }
    }

    private void prepareTargetOrdersList() {
        List<TargetOrder> orderedTargetOrders = targetOrders.stream()
                .filter((to) -> Math.abs(to.getQty()) > 0)
                .sorted(Comparator.comparing((to) -> to.getIdx()))
                .collect(Collectors.toList());
        amendmentProcess.setTargetOrderQueue(orderedTargetOrders);
    }

    private Contract getContract(String code) {
        Contract contract = new Contract();
        contract.secType(Types.SecType.FUT);
        contract.localSymbol(code);
        contract.exchange(exchange);
        contract.currency(ccy);
        return contract;
    }

    private int placeTargetOrder(TargetOrder to) {
        Contract contract = getContract(to.getCode());
        OrderType ot = OrderType.STP;
        switch (to.getOrderType()) {
            case "STP":
                ot = OrderType.STP;
                break;
            case "MKT":
                ot = OrderType.MKT;
                break;
        }
        return placeOrderImpl(contract, to.getQty() > 0 ? Types.Action.BUY : Types.Action.SELL, Math.abs(to.getQty()), PriceUtils.convertPrice(to.getViewPx(), futPriceCoeff), ot);
    }

    public int placeOrderImpl(Contract contract, Types.Action action, double totalQuantity, double px, OrderType orderType) {
        int orderId = nextOrderId;
        Order order = new Order();
        order.action(action);
        order.orderType(orderType);
        if (orderType == OrderType.STP) {
            order.outsideRth(true);
            order.tif(Types.TimeInForce.GTC);
            order.auxPrice(px);
        }
        order.totalQuantity(totalQuantity);
        order.account(account);
        ibGateway.getClientSocket().placeOrder(orderId, contract, order);
        nextOrderId++;
        return orderId;
    }

    private void onPythonScriptResult(PythonScriptResult m) {
        // clean target orders
        targetOrders = new LinkedList<>();
        controller.onTargetOrders(targetOrders);

        // Email
        if (m.getResult() == PythonScriptResult.Result.FAILURE
                || m.getResult() == PythonScriptResult.Result.TIMEOUT
                || m.getResult() == PythonScriptResult.Result.DATA_FAILURE) {
            String message = String.format("Options: python script %s %s", m.getResult().toString(), m.getFolder());
            emailActorSelection.tell(new EmailActor.Email(message, message), self());
        }

        if (amendmentProcess != null) {
            try {
                if (m.getResult() == PythonScriptResult.Result.FAILURE) {
                    amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PYTHON_FAILED);
                } else if (m.getResult() == PythonScriptResult.Result.TIMEOUT) {
                    amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PYTHON_TIMEOUT);
                } else if (m.getResult() == PythonScriptResult.Result.DATA_FAILURE) {
                    amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PYTHON_DATA_FAILURE);
                } else if (m.getResult() == PythonScriptResult.Result.SUCCESS) {
                    if (amendmentProcess.getCurrentStage() == AmendmentProcess.Stage.WAIT_PY_SCRIPT_COMPLETION) {
                        try {
                            targetOrders = StorageUtils.readTargetOrders(m.getFolder());
                            controller.onTargetOrders(targetOrders);
                            prepareTargetOrdersList();
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PLACE_ORDERS);
                        } catch (Throwable t) {
                            String message = String.format("Options: can not read python script result %s %s", m.getResult().toString(), m.getFolder());
                            emailActorSelection.tell(new EmailActor.Email(message, message), self());
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PYTHON_FAILED);
                            LOGGER.debug("", t);
                        }
                    }
                }
            } finally {
                doNextAmendmentProcessStage();
            }
        }
    }

    private static final DateTimeFormatter RUN_TIME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter RUN_TIME_FILE_NAME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd_HHmmss.SSS")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private void storeInputData(List<Position> inputPositions,
                                List<Timebar> inputTimebars,
                                String dataFolder,
                                String dataFolderName,
                                String sRunTime,
                                String scriptParams,
                                String uptrend) throws IOException {
        Path path = Paths.get(dataFolder);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        String inputPositionsFileName = String.format("%s\\positions.csv", dataFolder);
        StorageUtils.storeInputPositions(inputPositions, inputPositionsFileName);
        String inputTimeBarsFileName = String.format("%s\\time_bars.csv", dataFolder);
        StorageUtils.storeInputBars(inputTimebars, inputTimeBarsFileName);
        String comandLineParametersFileName = String.format("%s\\command_line.txt", dataFolder);
        try (PrintWriter out = new PrintWriter(comandLineParametersFileName)) {
            out.println(String.format("\"%s\" \"%s\" \"%s\" \"%s\"", dataFolderName, sRunTime, scriptParams, uptrend));
        }
    }

    public static enum PortfolioResult {
        FAILED_TO_PREPARE_DATA,
        FAILED,
        TIMEOUT,
        SUCCESS,
    }

    private void onShowPortfolio(ShowPortfolio m) {
        Instant runTime = Instant.now();
        String sRunTime = RUN_TIME_FMT.format(runTime);
        String dataFolderName = "profile_data";
        String DATA_FOLDER = String.format("%s\\data\\%s", scriptFolder, dataFolderName);
        try {
            List<Position> inputPositions = positions
                    .values()
                    .stream()
                    .collect(Collectors.toList());
            List<Timebar> inputTimebars = new LinkedList<>();
            for (Map.Entry<HedgerActor.TimeBarRequest, HedgerActor.TimebarArray> entry : currentBars.entrySet()) {
                inputTimebars.addAll(entry.getValue().getBars());
            }

            storeInputData(inputPositions, inputTimebars, DATA_FOLDER, dataFolderName, sRunTime, "", "");
            String pythonScript = String.format("%s\\portfolio.py", scriptFolder);
            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, pythonScript, dataFolderName, sRunTime);
            try {
                Process process = processBuilder.start();
                Thread waitThread = new Thread(() -> {
                    try {
                        process.waitFor();
                        int exitCode = process.exitValue();
                        controller.onPortfolioResult(exitCode == 0 ? PortfolioResult.SUCCESS : PortfolioResult.FAILED);
                    } catch (Throwable t) {
                        LOGGER.error("", t);
                        controller.onPortfolioResult(PortfolioResult.FAILED);
                    }
                });
                waitThread.setDaemon(true);
                waitThread.start();
            } catch (Throwable t) {
                LOGGER.error("", t);
                controller.onPortfolioResult(PortfolioResult.FAILED);
            }
        } catch (Throwable t) {
            controller.onPortfolioResult(PortfolioResult.FAILED_TO_PREPARE_DATA);
        }
    }

    private void runPython() {
        Instant runTime = Instant.now();
        String sRunTime = RUN_TIME_FMT.format(runTime);
        String dataFolderName = RUN_TIME_FILE_NAME_FMT.format(runTime);
        String DATA_FOLDER = String.format("%s\\data\\%s", scriptFolder, dataFolderName);
        String sUpTrend = controller.isUptrend() ? "True" : "False";

        try {
            List<Position> inputPositions = positions
                    .values()
                    .stream()
                    .filter((p) -> {
                        if (p.getContract().secType() == Types.SecType.FUT) {
                            if (amendmentProcess.includeIntoPositions(p.getContract().localSymbol())) {
                                return true;
                            }
                        } else if (p.getContract().secType() == Types.SecType.FOP) {
                            if (amendmentProcess.includeIntoPositions(p.getContractDetails().underSymbol())) {
                                return true;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            List<Timebar> inputTimebars = new LinkedList<>();
            for (Map.Entry<HedgerActor.TimeBarRequest, HedgerActor.TimebarArray> entry : currentBars.entrySet()) {
                if (amendmentProcess.includeIntoPositions(entry.getKey().getLocalSymbol())) {
                    inputTimebars.addAll(entry.getValue().getBars());
                }
            }

            storeInputData(inputPositions, inputTimebars, DATA_FOLDER, dataFolderName, sRunTime, amendmentProcess.getCommand().getParams(), sUpTrend);
        } catch (Throwable t) {
            LOGGER.error("", t);
            self().tell(new PythonScriptResult(actorId, PythonScriptResult.Result.DATA_FAILURE, DATA_FOLDER), self());
            return;
        }

        String pythonScript = String.format("%s\\delta-hedger.py", scriptFolder);
        ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, pythonScript, dataFolderName, sRunTime, amendmentProcess.getCommand().getParams(), sUpTrend);
        try {
            Process process = processBuilder.start();
            Thread waitThread = new Thread(() -> {
                try {
                    if (process.waitFor(30l, TimeUnit.SECONDS)) {
                        int exitCode = process.exitValue();
                        self().tell(new PythonScriptResult(actorId, exitCode == 0 ? PythonScriptResult.Result.SUCCESS : PythonScriptResult.Result.FAILURE, DATA_FOLDER), self());
                    } else {
                        self().tell(new PythonScriptResult(actorId, PythonScriptResult.Result.TIMEOUT, DATA_FOLDER), self());
                    }
                } catch (Throwable t) {
                    LOGGER.error("", t);
                    self().tell(new PythonScriptResult(actorId, PythonScriptResult.Result.FAILURE, DATA_FOLDER), self());
                }
            });
            waitThread.setDaemon(true);
            waitThread.start();
        } catch (Throwable t) {
            LOGGER.error("", t);
            self().tell(new PythonScriptResult(actorId, PythonScriptResult.Result.FAILURE, DATA_FOLDER), self());
        }
    }

    private final LinkedList<RunAmendmentProcess> pendingProcesses = new LinkedList<>();

    private void onRunAmendmentProcess(RunAmendmentProcess m) {
        pendingProcesses.add(m);
        processPendingAmendmentProcessQueue();
    }

    private void processPendingAmendmentProcessQueue() {
        if (amendmentProcess != null && !amendmentProcess.isFinished()) {
            return;
        }
        // Place as pending when disconnected
        if (!ibConnection || !apiConnection) {
            return;
        }
        if (!pendingProcesses.isEmpty()) {
            RunAmendmentProcess m = pendingProcesses.pollFirst();
            amendmentProcess = new AmendmentProcess(m, controller.isCancelOrders(), m.isManual() || controller.isConfirmCancel(),controller.isRunPython(), controller.isPlaceOrders(), m.isManual() || controller.isConfirmPlace());
            String message = String.format("Options: starting %s amendment procedure", m.getParams());
            emailActorSelection.tell(new EmailActor.Email(message, message), self());
            doNextAmendmentProcessStage();
        }
    }

    private void requestOpenOrders() {
        openOrders.clear();
        ibGateway.getClientSocket().reqOpenOrders();
    }

    private void onRefreshOpenOrders(RefreshOpenOrders m) {
        requestOpenOrders();
    }

    private Map<Integer, HedgerOrder> openOrders = new HashMap<>();

    private void onOpenOrder(IbGateway.OpenOrder m) {
        try {
            if (!m.getOrder().account().equals(account)) {
                return;
            }
            if (m.getOrderId() == 0) {
                requestOpenOrders();
                return;
            }
            if (!underlyingsFilter.contains(m.getContract().symbol())) {
                return;
            }
            if (!controller.isIncludeManualOrders() && m.getOrderId() <= 0) {
                return;
            }
            String localSymbol = m.getContract().localSymbol();
            int orderId = m.getOrderId();
            HedgerOrder.Side orderSide = HedgerOrder.Side.BUY;
            switch (m.getOrder().action()) {
                case BUY:
                    orderSide = HedgerOrder.Side.BUY;
                    break;
                case SELL:
                case SSHORT:
                    orderSide = HedgerOrder.Side.SELL;
                    break;
            }

            HedgerOrder.State orderState = HedgerOrder.State.ACTIVE;
            switch (m.getOrderState().status()) {
                case Cancelled:
                case ApiCancelled:
                case PendingCancel:
                    orderState = HedgerOrder.State.CANCELLED;
                    break;
                case Filled:
                    orderState = HedgerOrder.State.FILLED;
                    break;
                case Inactive:
                    orderState = HedgerOrder.State.REJECTED;
                    break;
            }
            double px = 0.0d;
            switch (m.getOrder().orderType()) {
                case LMT:
                    px = m.getOrder().lmtPrice();
                    break;
                case STP:
                    px = m.getOrder().auxPrice();
                    break;
            }

            double qty = m.getOrder().totalQuantity();

            HedgerOrder ho = new HedgerOrder(localSymbol,
                    orderId,
                    orderSide,
                    orderState,
                    qty,
                    px,
                    PriceUtils.convertPrice(px, futPriceCoeff));
            if (!openOrders.containsKey(orderId)) {
                openOrders.put(orderId, ho);
            }
            if (ho.isTerminalState()) {
                openOrders.remove(orderId);
            }
        } finally {
            List<HedgerOrder> oo = openOrders.values().stream().map((ho) -> new HedgerOrder(ho)).collect(Collectors.toList());
            controller.onOpenOrders(oo);
        }
    }

    private static final Set<OrderStatus> acceptedOrderStatuses = new HashSet<>();

    static {
        acceptedOrderStatuses.add(OrderStatus.Submitted);
        acceptedOrderStatuses.add(OrderStatus.PreSubmitted);
        acceptedOrderStatuses.add(OrderStatus.ApiCancelled);
        acceptedOrderStatuses.add(OrderStatus.Cancelled);
        acceptedOrderStatuses.add(OrderStatus.Filled);
    }

    private void onOrderStatus(IbGateway.OrderStatus m) {
        try {
            if (m.getClientId() != clientId) {
                return;
            }
            if (!controller.isIncludeManualOrders() && m.getOrderId() < 0) {
                return;
            }
            OrderStatus orderStatus = OrderStatus.get(m.getStatus());

            if (amendmentProcess != null) {
                try {
                    if (amendmentProcess.isPlacedOrder(m.getOrderId())) {
                        if (orderStatus == OrderStatus.Inactive) {
                            String message = "Options: order rejected";
                            emailActorSelection.tell(new EmailActor.Email(message, message), self());
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.ORDER_REJECTED);
                        } else if (acceptedOrderStatuses.contains(orderStatus)) {
                            amendmentProcess.setCurrentStage(AmendmentProcess.Stage.PLACE_NEXT_TARGET_ORDER);
                        }
                    }
                    if (amendmentProcess.isCancelledOrder(m.getOrderId())) {
                        if (orderStatus == OrderStatus.Cancelled) {
                            amendmentProcess.onCancelComplete(m.getOrderId());
                            if (amendmentProcess.isAllOrdersCancelled()) {
                                amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CALL_PY_SCRIPT);
                            }
                        }
                    }
                } finally {
                    doNextAmendmentProcessStage();
                }
            }

            if (openOrders.containsKey(m.getOrderId())) {
                HedgerOrder ho = openOrders.get(m.getOrderId());

                switch (orderStatus) {
                    case Cancelled:
                    case ApiCancelled:
                    case PendingCancel:
                        ho.onCancelled();
                        break;
                    case Filled:
                        ho.onFilled();
                        break;
                    case Inactive:
                        ho.onRejected();
                        break;
                }
                if (ho.isTerminalState()) {
                    openOrders.remove(m.getOrderId());
                }
            }
        } finally {
            List<HedgerOrder> oo = openOrders.values().stream().map((ho) -> new HedgerOrder(ho)).collect(Collectors.toList());
            controller.onOpenOrders(oo);
        }
    }

    private void onOpenOrderEnd(IbGateway.OpenOrderEnd m) {

    }

    private void onReloadPositions(ReloadPositions m) {
        requestPositions();
    }

    private void requestExecutions() {
        ibGateway.getClientSocket().reqExecutions(1, new ExecutionFilter());
    }

    private List<IbGateway.Position> positionsBuffer = new LinkedList<>();

    private void requestPositions() {
        positionsBuffer.clear();
        ibGateway.getClientSocket().reqPositions();
    }

    private void onConnected(IbGateway.ConnectAck m) {
        ibConnection = true;
        controller.onIbConnection(ibConnection);
        apiConnection = true;
        controller.onApiConnection(apiConnection);
        processPendingAmendmentProcessQueue();
    }

    private Set<String> processedTrades = new HashSet<>();

    private void onExecDetails(IbGateway.ExecDetails m) {
        if (!processedTrades.contains(m.getExecution().execId()) && m.getExecution().acctNumber().equals(account)) {
            processedTrades.add(m.getExecution().execId());

            Position p = null;
            Trade trade = new Trade(m.getContract(),
                    Utils.getPosition(m),
                    m.getExecution().price(),
                    m.getExecution().execId(),
                    m.getExecution().time()
            );
            StorageUtils.storeTrade(m);

            //update positions if necessary
            if (((controller.isIncludeFutures() && m.getContract().secType() == Types.SecType.FUT)
                    || (controller.isIncludeOptions() && m.getContract().secType() == Types.SecType.FOP))
                    && underlyingsFilter.contains(m.getContract().symbol())
                    ) {
                MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);
                String localSymbol = m.getContract().localSymbol();

                if (positions.containsKey(localSymbol)) {
                    p = positions.get(localSymbol);
                    p.updatePosition(m);
                    p.setLastTrade(trade);
                } else {
                    Contract c = m.getContract();
                    double pos = Utils.getPosition(m);
                    double posPx = m.getExecution().price();
                    p = new Position(true, c, pos, posPx, 0.0d, 0.0d, trade);
                }

                if (p.isZero()) {
                    uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.DELETE, localSymbol, p);
                    positions.remove(localSymbol);
                } else {
                    uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, localSymbol, p);
                    positions.put(localSymbol, p);
                }
                StorageUtils.storePositions(positions);
                controller.onPositionsUpdate(uiUpdates);
                onPositionsUpdate();
//                if (m.getContract().secType() == Types.SecType.FOP) {
//                    StringBuilder sb = new StringBuilder(getExecDetails(m));
//                    sb.append(System.lineSeparator());
//                    emailActorSelection.tell(new EmailActor.Email("Options: TRADE", sb.toString()), self());
//
//                }
                // trigger amendment on fut trades only
                if (controller.isTriggerOnTrade() && m.getContract().secType() == Types.SecType.FUT) {
                    HashSet<String> targetUnderlyings = new HashSet<>();
                    targetUnderlyings.add(m.getContract().localSymbol());
                    self().tell(new RunAmendmentProcess(targetUnderlyings, controller.getScriptParams(), false), self());
                }
            }
        }
    }

    private String getExecDetails(IbGateway.ExecDetails ed) {
        return String.format("Exec details: %s %s\n%s %.0f %s at %.2f\n%s",
                ed.getExecution().acctNumber(),
                ed.getExecution().execId(),
                ed.getExecution().side(),
                ed.getExecution().shares(),
                ed.getContract().localSymbol(),
                ed.getExecution().price(),
                ed.getExecution().time());
    }


    public static final class Timebar {
        private final String localSymbol;
        private final String duration;
        private final String size;
        private final Instant barTime;
        private final double open;
        private final String openView;
        private final double high;
        private final String highView;
        private final double low;
        private final String lowView;
        private final double close;
        private final String closeView;
        private final double volume;
        private final Instant lut;

        public Timebar(String localSymbol,
                       String duration,
                       String size,
                       Instant barTime,
                       double open,
                       String openView,
                       double high,
                       String highView,
                       double low,
                       String lowView,
                       double close,
                       String closeView,
                       double volume,
                       Instant lut) {
            this.localSymbol = localSymbol;
            this.duration = duration;
            this.size = size;
            this.barTime = barTime;
            this.open = open;
            this.openView = openView;
            this.high = high;
            this.highView = highView;
            this.low = low;
            this.lowView = lowView;
            this.close = close;
            this.closeView = closeView;
            this.volume = volume;
            this.lut = lut;
        }

        public String getLocalSymbol() {
            return localSymbol;
        }

        public Instant getBarTime() {
            return barTime;
        }

        public double getOpen() {
            return open;
        }

        public double getHigh() {
            return high;
        }

        public double getLow() {
            return low;
        }

        public double getClose() {
            return close;
        }

        public String getOpenView() {
            return openView;
        }

        public String getHighView() {
            return highView;
        }

        public String getLowView() {
            return lowView;
        }

        public String getCloseView() {
            return closeView;
        }

        public double getVolume() {
            return volume;
        }

        public String getDuration() {
            return duration;
        }

        public String getSize() {
            return size;
        }

        public Instant getLut() {
            return lut;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Timebar timebar = (Timebar) o;
            return Objects.equals(localSymbol, timebar.localSymbol) &&
                    Objects.equals(size, timebar.size) &&
                    Objects.equals(barTime, timebar.barTime);
        }

        @Override
        public int hashCode() {

            return Objects.hash(localSymbol, size, barTime);
        }
    }

    public static final class TimebarArray {

        public double getLastPx() {
            return lastTimeBar.close;
        }

        private Timebar lastTimeBar = null;

        private final Set<Timebar> bars = new HashSet<>();

        public void onTimeBar(Timebar tb) {
            lastTimeBar = tb;
            bars.remove(tb);
            bars.add(tb);
        }

        public Collection<Timebar> getBars() {
            return bars;
        }
    }

    public static final class TimeBarRequest {
        private final String localSymbol;
        private final String duration;
        private final String size;

        public TimeBarRequest(String localSymbol, String duration, String size) {
            this.localSymbol = localSymbol;
            this.duration = duration;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeBarRequest that = (TimeBarRequest) o;
            return Objects.equals(localSymbol, that.localSymbol) &&
                    Objects.equals(duration, that.duration) &&
                    Objects.equals(size, that.size);
        }

        @Override
        public int hashCode() {
            return Objects.hash(localSymbol, duration, size);
        }

        public String getLocalSymbol() {
            return localSymbol;
        }
    }

    private Map<Integer, TimeBarRequest> timeBarsRequestMap = new HashMap<>();
    private Map<TimeBarRequest, TimebarArray> currentBars = new HashMap<>();
    int currentHistDataRequestId = 1;

    private void onHistDataEndMsg(IbGateway.HistDataEndMsg m) {
        if (amendmentProcess != null) {
            try {
                if (amendmentProcess.getCurrentStage() == AmendmentProcess.Stage.WAIT_TIME_BARS) {
                    if (amendmentProcess.isHistReq(m.getReqId())) {
                        amendmentProcess.onHistReqComplete(m.getReqId());
                    }
                    if (amendmentProcess.isAllHistReqCompleted()) {
                        amendmentProcess.setCurrentStage(AmendmentProcess.Stage.CANCEL_ORDERS);
                    }
                }
            } finally {
                doNextAmendmentProcessStage();
            }
        }
    }

    private void onHistDataUpdateMsg(IbGateway.HistDataUpdateMsg m) {
        onTimeBar(m.getReqId(), m.getBar());
    }

    @Value("${fut.price.coeff}")
    private double futPriceCoeff;

    private static final DateTimeFormatter BAR_TIME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd  HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private void onTimeBar(int reqId, Bar bar) {
        if (timeBarsRequestMap.containsKey(reqId)) {
            TimeBarRequest r = timeBarsRequestMap.get(reqId);
            if (r == null) {
                return;
            }
            Timebar tb = new Timebar(
                    r.localSymbol,
                    r.duration,
                    r.size,
                    BAR_TIME_FMT.parse(bar.time(), Instant::from),
                    bar.open(),
                    PriceUtils.convertPrice(bar.open(), futPriceCoeff),
                    bar.high(),
                    PriceUtils.convertPrice(bar.high(), futPriceCoeff),
                    bar.low(),
                    PriceUtils.convertPrice(bar.low(), futPriceCoeff),
                    bar.close(),
                    PriceUtils.convertPrice(bar.close(), futPriceCoeff),
                    bar.volume(),
                    Instant.now()
            );
            TimebarArray arr = currentBars.get(r);
            if (arr == null) {
                arr = new TimebarArray();
                currentBars.put(r, arr);
            }
            arr.onTimeBar(tb);
            controller.onTimeBar(tb);


            Position p = positions.get(r.localSymbol);
            if (p != null) {
                p.setLastViewPx(PriceUtils.convertPrice(bar.close(), futPriceCoeff));
                MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, r.localSymbol, p);
                controller.onPositionsUpdate(uiUpdates);
            }
        } else {
            ibGateway.getClientSocket().cancelHistoricalData(reqId);
        }
    }

    private void onHistDataMsg(IbGateway.HistDataMsg m) {
        onTimeBar(m.getReqId(), m.getBar());
    }

    private void onRefreshTimebars(RefreshTimebars m) {
        refreshTimebars(true);
    }

    private boolean refreshTimebars(boolean fullUpdate) {
        // clear UI bars
        controller.onClearTimeBars();
        timeBarsRequestMap.clear();
        currentBars.clear();
        Set<TimeBarRequest> requests = new HashSet<>();

        for (Map.Entry<String, Position> e : positions.entrySet()) {
            String localSymbol = e.getKey();
            Position p = e.getValue();
            if (p.getContract().secType() == Types.SecType.FUT) {

                barsToRequest.stream().forEach(ds->{
                    requests.add(new TimeBarRequest(localSymbol, ds.duration, ds.size));
                });


            } else if (p.getContract().secType() == Types.SecType.FOP) {
                if (p.isContractDetailsDefined()) {
                    barsToRequest.stream().forEach(ds->{
                        requests.add(new TimeBarRequest(p.getContractDetails().underSymbol(), ds.duration, ds.size));
                    });
                }
            }
        }

        if (amendmentProcess != null) {
            if (amendmentProcess.getCurrentStage() == AmendmentProcess.Stage.REFRESH_TIME_BARS) {
                for (int i = 0; i < requests.size(); ++i) {
                    amendmentProcess.addHistRequest(currentHistDataRequestId + i);
                }
            }
        }

        for (TimeBarRequest r : requests) {
            Contract c = new Contract();
            c.localSymbol(r.localSymbol);
            c.secType(Types.SecType.FUT);
            c.exchange(exchange);
            c.currency(ccy);
            ibGateway.getClientSocket().reqHistoricalData(currentHistDataRequestId, c, "", r.duration, r.size, "TRADES", 0, 1, false, null);
            timeBarsRequestMap.put(currentHistDataRequestId, r);
            currentHistDataRequestId++;
        }

        return !requests.isEmpty();

//        if (fullUpdate) {
//            for (Integer reqId : timeBarsRequestMap.keySet()) {
//                ibGateway.getClientSocket().cancelHistoricalData(reqId);
//            }
//            timeBarsRequestMap.clear();
//        }
//        Set<TimeBarRequest> requests = new HashSet<>();
//
//        for (Map.Entry<String, Position> e : positions.entrySet()) {
//            String localSymbol = e.getKey();
//            Position p = e.getValue();
//            if (p.getContract().secType() == Types.SecType.FUT) {
//                requests.add(new TimeBarRequest(localSymbol, "4 hours"));
//            } else if (p.getContract().secType() == Types.SecType.FOP) {
//                if (p.isContractDetailsDefined()) {
//                    requests.add(new TimeBarRequest(p.getContractDetails().underSymbol(), "4 hours"));
//                }
//            }
//        }
//
//        //Request only new codes
//        requests.removeAll(timeBarsRequestMap.values());
//        for (TimeBarRequest r : requests) {
//            Contract c = new Contract();
//            c.localSymbol(r.localSymbol);
//            c.secType(Types.SecType.FUT);
//            c.exchange(exchange);
//            c.currency(ccy);
//            ibGateway.getClientSocket().reqHistoricalData(currentHistDataRequestId, c, "", "2 D", r.duration, "TRADES", 0, 1, true, null);
//            timeBarsRequestMap.put(currentHistDataRequestId, r);
//            currentHistDataRequestId++;
//        }
    }

    private Map<Integer, String> contractDetailsRequestMap = new HashMap<>();
    int nextContractDetailsRequestId = 1;

    private void requestContractDetails() {
        for (Map.Entry<String, Position> e : positions.entrySet()) {
            String localSymbol = e.getKey();
            Position p = e.getValue();
            if (!p.isContractDetailsDefined()) {
                if (!contractDetailsRequestMap.values().contains(localSymbol)) {
                    contractDetailsRequestMap.put(nextContractDetailsRequestId, localSymbol);
                    ibGateway.getClientSocket().reqContractDetails(nextContractDetailsRequestId, p.getContract());
                    nextContractDetailsRequestId++;
                }
            }
        }
    }

    private void onPositionsUpdate() {
        requestContractDetails();
//        refreshTimebars(false);
    }

    private void onContractDetails(IbGateway.ContractDetailsMsg m) {
        if (contractDetailsRequestMap.containsKey(m.getReqId())) {
            String localSymbol = contractDetailsRequestMap.get(m.getReqId());
            if (positions.containsKey(localSymbol)) {
                Position p = positions.get(localSymbol);
                p.setContractDetails(m.getContractDetails());
                MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, localSymbol, p);
                controller.onPositionsUpdate(uiUpdates);
            }
            contractDetailsRequestMap.remove(m.getReqId());
        }
    }

    private void onExecDetailsEnd(IbGateway.ExecDetailsEnd m) {
    }

    private Map<String, Position> positions = new HashMap<>();

    private void onPosition(IbGateway.Position m) {
        if (m.getAccount().equals(account)) {
            if (underlyingsFilter.contains(m.getContract().symbol())) {
                positionsBuffer.add(m);
            }
        }
    }

    private void onPositionEnd(IbGateway.PositionEnd m) {
        MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(true);

        Map<String, Position> oldPositions = new HashMap<>();
        oldPositions.putAll(positions);
        positions.clear();

        for (IbGateway.Position pm : positionsBuffer) {
            if (pm.getPosition() != 0.0d) {
                Contract c = pm.getContract();
                c.exchange(exchange);
                String localSymbol = c.localSymbol();
                if (!positions.containsKey(localSymbol)) {
                    double vol = 0.0d;
                    double ir = 0.0d;
                    boolean selected = true;
                    Trade lastTrade = null;
                    if (oldPositions.containsKey(localSymbol)) {
                        Position oldPosition = oldPositions.get(localSymbol);
                        vol = oldPosition.getVol();
                        ir = oldPosition.getIr();
                        selected = oldPosition.isSelected();
                        lastTrade = oldPosition.getLastTrade();
                    }
                    Position p = new Position(selected, c, pm.getPosition(), pm.getPositionPrice() / Double.parseDouble(pm.getContract().multiplier()), vol, ir, lastTrade);
                    positions.put(localSymbol, p);
                    uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, localSymbol, p);
                }
            }
        }
        StorageUtils.storePositions(positions);
        positionsBuffer.clear();

        onPositionsUpdate();
        controller.onPositionsUpdate(uiUpdates);
        controller.onPositionsReloadComplete();
    }

    private void onPosSelectionChanged(PosSelectionChanged m) {
        if (positions.containsKey(m.localSymbol)) {
            Position p = positions.get(m.localSymbol);
            p.setSelected(m.selected);
            StorageUtils.storePositions(positions);
        }
    }

    private void onIrChanged(IrChanged m) {
        if (positions.containsKey(m.localSymbol)) {
            Position p = positions.get(m.localSymbol);
            p.setIr(m.ir);
            StorageUtils.storePositions(positions);
        }
    }

    private void onVolChanged(VolChanged m) {
        if (positions.containsKey(m.localSymbol)) {
            Position p = positions.get(m.localSymbol);
            p.setVol(m.vol);
            StorageUtils.storePositions(positions);
        }
    }


}
