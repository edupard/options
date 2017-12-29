package com.skywind.log_trader.actors;

import akka.actor.*;
import com.ib.client.*;
import com.skywind.ib.Utils;
import com.skywind.log_trader.ui.MainController;
import com.skywind.ib.IbGateway;
import com.skywind.trading.spring_akka_integration.EmailActor;
import com.skywind.trading.spring_akka_integration.MessageSentToExactActorInstance;
import com.skywind.trading.spring_akka_integration.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.skywind.log_trader.actors.LogTraderActor.BEAN_NAME;

@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LogTraderActor extends AbstractActor {
    public final static String BEAN_NAME = "logTraderActor";
    private final Logger LOGGER = LoggerFactory.getLogger(LogTraderActor.class);

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

    @Value("${symbol}")
    private String symbol;

    @Value("${exchange}")
    private String exchange;

    @Value("${ccy}")
    private String ccy;



    protected int nextOrderId = -1;

    @Autowired
    private MainController controller;

    @Autowired
    private SpringExtension springExtension;

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private StorageComponent storage;

    private ActorSelection fileActorSelection;
    private ActorSelection emailActorSelection;


    public LogTraderActor() {
        ibGateway = new IbGateway(self(), actorId);
    }


    public static final class Start {
    }

    public static final class ReloadPosition {

    }

    public final static class StartByUserAction {
    }

    public final static class SimulateError {
    }

    public static enum AdjustPosition {
        LONG,
        FLAT,
        SHORT
    }



    public static final class Restart extends MessageSentToExactActorInstance {

        public Restart(UUID actorId) {
            super(actorId);
        }

    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Start.class, m -> {
                    onStart(m);
                })
                .match(Restart.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onRestart(m);
                    }
                })
                .match(ReloadPosition.class, m -> {
                    onReloadPosition(m);
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
                .match(FileActor.Signal.class, m -> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onSignal(m);
                    }
                })
                .match(IbGateway.OpenOrder.class, m-> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onOpenOrder(m);
                    }
                })
                .match(IbGateway.OrderStatus.class, m-> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onOrderStatus(m);
                    }
                })
                .match(IbGateway.UpdateAccountValue.class, m-> {
                    if (m.isActorInstaceEquals(actorId)) {
                        onUpdateAccountValue(m);
                    }
                })
                .match(IbGateway.TickPrice.class, tp -> {
                    onTickPrice(tp);
                })
                .match(StartByUserAction.class, m -> {
                    onStartByUser(m);
                })
                .match(SimulateError.class, m -> {
                    onSimulateError(m);
                })
                .match(AdjustPosition.class, m-> {
                    onAdjustPosition(m);
                })
                .matchAny(m -> recievedUnknown(m))
                .build();
    }


    private void onUpdateAccountValue(IbGateway.UpdateAccountValue m) {
        if (m.getAccountName().equals(account)) {
            switch (m.getKey()) {
                case "InitMarginReq": {
                    String initMargin = String.format("%s %s", m.getValue(), m.getCurrency());
                    controller.onInitMargin(initMargin);
                }
                break;
                case "NetLiquidation": {
                    String nlv = String.format("%s %s", m.getValue(), m.getCurrency());
                    controller.onNlv(nlv);
                }
                break;
            }
        }
    }


    private FileActor.Signal signal;

    private void onStartByUser(StartByUserAction m) {
        if (signal != null) {
            placeOrder();
        }
    }

    private void onSignal(FileActor.Signal m) {
        signal = m;
        targetPosition = signal.getTargetPosition();
        placeOrder();
    }

    private int pendingOrderId = -1;
    private double pendingOrderSize = 0;
    private boolean schedulePlaceOrderCall = false;

    private void placeOrder() {
        if (initializing) {
            return;
        }
//        if (!signal.isNewLine()) {
//            return;
//        }
        if (signal.getAction() == FileActor.Signal.Action.WAIT) {
            return;
        }
        if (!controller.isStarted()) {
            return;
        }

        if (pendingOrderId != -1) {
            schedulePlaceOrderCall = true;
            ibGateway.getClientSocket().cancelOrder(pendingOrderId);
        }
        else {
            double positionDelta = targetPosition - position;
            if (positionDelta != 0) {
                String message = String.format("Log trader market order: %s %.0f ", symbol, positionDelta);
                emailActorSelection.tell(new EmailActor.Email(message, message), self());

                placeMarketOrder(positionDelta);
            }
        }
    }

    @Value("${long.size}")
    private double longSize;

    @Value("${short.size}")
    private double shortSize;

    private static double ZERO_POSITION = 0.0d;

    private void onAdjustPosition(AdjustPosition m) {
//        if (pendingOrderId != -1) {
//            getContext().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
//                    self(),
//                    m,
//                    getContext().dispatcher(),
//                    self());
//            return;
//        }
        double tgt = ZERO_POSITION;
        switch (m) {
            case FLAT:
                tgt = ZERO_POSITION;
                break;
            case LONG:
                tgt = longSize;
                break;
            case SHORT:
                tgt = -shortSize;
                break;
        }
        double positionDelta = tgt - position;
        if (positionDelta != 0) {
            String message = String.format("Log trader market order: %s %.0f ", symbol, positionDelta);
            emailActorSelection.tell(new EmailActor.Email(message, message), self());
            placeMarketOrder(positionDelta);
        }
    }

    private Contract getContract() {
        Contract contract = new Contract();
        contract.secType(Types.SecType.FUT);
        contract.localSymbol(symbol);
        contract.exchange(exchange);
        contract.currency(ccy);
        return contract;
    }

    private void placeMarketOrder(double positionDelta) {
        Contract contract = getContract();
        pendingOrderId = placeMarketOrderImpl(contract, positionDelta > 0 ? Types.Action.BUY : Types.Action.SELL, Math.abs(positionDelta), false);
        pendingOrderSize = Math.abs(positionDelta);
    }

    public int placeMarketOrderImpl(Contract contract, Types.Action action, double totalQuantity, boolean test) {
        int orderId = nextOrderId;
        Order order = new Order();
        order.action(action);
        order.orderType(OrderType.MKT);
        order.totalQuantity(totalQuantity);
        order.account(account);
        order.whatIf(test);
        ibGateway.getClientSocket().placeOrder(orderId, contract, order);
        nextOrderId++;
        return orderId;
    }


    private void startListenFileSignals() {
        fileActorSelection.tell(new FileActor.SignalRequest(actorId), self());
    }



    // should not be called at all
    private void onErrorWithMessage(IbGateway.ErrorWithMessage m) {
        String message = String.format("Log trader error: %s", m.getErrorMsg());
        emailActorSelection.tell(new EmailActor.Email(message, message), self());
        throw new RuntimeException(m.getErrorMsg());
    }

    // Handles errors generated within the API itself. If an exception is thrown within the API code it will be notified here. Possible cases include errors while reading the information from the socket or even mishandling at EWrapper's implementing class.
    private void onErrorWithException(IbGateway.ErrorWithException m) {
        String message = String.format("Log trader error: %s", m.getReason().getMessage());
        emailActorSelection.tell(new EmailActor.Email(message, message), self());
        throw new RuntimeException(m.getReason());
    }


    public final static Set<Integer> DISCONNECTED_FROM_IB_ERROR_CODES = new HashSet<>();
    public final static Set<Integer> RECONNECTED_TO_IB_ERROR_CODES = new HashSet<>();
    public final static Set<Integer> FATAL_ERROR_CODES = new HashSet<>();

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
    }


    private boolean ibConnection = false;
    private boolean apiConnection = false;

    private void onErrorWithCode(IbGateway.ErrorWithCode m) {
        if (DISCONNECTED_FROM_IB_ERROR_CODES.contains(m.getErrorCode())) {
            if (ibConnection) {
                ibConnection = false;
                controller.onIbConnection(ibConnection);
                String message = "Log trader: disconnected from IB";
                emailActorSelection.tell(new EmailActor.Email(message, message), self());
            }

        }
        if (RECONNECTED_TO_IB_ERROR_CODES.contains(m.getErrorCode())) {
            if (!ibConnection) {
                ibConnection = true;
                controller.onIbConnection(ibConnection);
                String message = "Log trader: reconnected to IB";
                emailActorSelection.tell(new EmailActor.Email(message, message), self());

                requestMarketData();
                requestExecutions();
                requestAccountUpdates();
            }
        }

        if (FATAL_ERROR_CODES.contains(m.getErrorCode())) {
            String message = String.format("%d %s", m.getErrorCode(), m.getErrorMsg());
            emailActorSelection.tell(new EmailActor.Email("Log trader: fatal error", message), self());
            throw new FatalException();
        }

        if (m.getErrorCode() == 507) {
            String message = "Log trader: ib socket closed";
            emailActorSelection.tell(new EmailActor.Email(message, message), self());
            controller.onApiConnection(false);
            controller.onIbConnection(false);
            throw new RuntimeException("Ib socket closed");
        }
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), (Throwable t) -> SupervisorStrategy.escalate());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public static final long RESTART_INTERVAL_SEC = 5l;

    @Override
    public void preStart() throws Exception {
        super.preStart();

        fileActorSelection = actorSystem.actorSelection("/user/file");

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
        ibGateway.disconnect();
    }

    @PostConstruct
    public void postConstruct() {
        controller.onApiConnection(apiConnection);
        controller.onIbConnection(ibConnection);
        controller.onUnknownPosition();
    }

    private void onStart(Start m) {
        start();
    }

    private double targetPosition = 0.0d;

    private double position = 0.0d;

    private void start() {
        position = storage.readPosition();
        for (Trade t : storage.readTrades()) {
            processedTrades.add(t.getExecId());
        }
        ibGateway.connect(host, port, clientId);
    }

    public void onRestart(Restart m) {
        start();
    }

    private void recievedUnknown(Object m) {
    }

    //OK
    private void onNextValidId(IbGateway.NextValidId m) {
        nextOrderId = m.getNextValidId();
        requestMarketData();
        requestExecutions();
        requestAccountUpdates();
    }




    //OK
    private void onReloadPosition(ReloadPosition m) {
        requestPositions();
    }

    private void onSimulateError(SimulateError m) {
        throw new RuntimeException("test");
    }

    private void onTickPrice(IbGateway.TickPrice tp) {
        if (tp.isLast() || tp.isClose()) {
            controller.onLastPx(tp.getPrice());

        }
    }

    int mktDataReqId = 1;
    private void requestMarketData() {
        Contract contract = getContract();
        ibGateway.getClientSocket().reqMktData(mktDataReqId, contract, "", false, false, null);
    }

    public void requestAccountUpdates() {
        ibGateway.getClientSocket().reqAccountUpdates(true, account);
    }
    //OK
    private void requestExecutions() {
        ibGateway.getClientSocket().reqExecutions(1, new ExecutionFilter());
    }

    private List<IbGateway.Position> positionsBuffer = new LinkedList<>();

    //OK
    private void requestPositions() {
        positionsBuffer.clear();
        ibGateway.getClientSocket().reqPositions();
    }

    //OK
    private void onConnected(IbGateway.ConnectAck m) {
        ibConnection = true;
        controller.onIbConnection(ibConnection);
        apiConnection = true;
        controller.onApiConnection(apiConnection);
    }

    private Set<String> processedTrades = new HashSet<>();

    private static final double ZERO_POS_TRESHOLD = 1e-6;

    private void onExecDetails(IbGateway.ExecDetails m) {
        if (!processedTrades.contains(m.getExecution().execId())
                && m.getExecution().acctNumber().equals(account)
                && m.getContract().localSymbol().equals(symbol)) {
            processedTrades.add(m.getExecution().execId());
            //update positions
            Trade trade = new Trade(m.getContract().localSymbol(),
                    Utils.getPosition(m),
                    m.getExecution().price(),
                    m.getExecution().execId(),
                    m.getExecution().time()
            );
            position += trade.getPos();
            storage.storeTrade(m);
            storage.storePositions(position);
            controller.onPosition(position);

            if (m.getExecution().orderId() == pendingOrderId) {
                pendingOrderSize -= m.getExecution().shares();
                if (Math.abs(pendingOrderSize) < ZERO_POS_TRESHOLD)
                {
                    pendingOrderId = -1;
                    pendingOrderSize = 0.0d;
                    if (schedulePlaceOrderCall) {
                        schedulePlaceOrderCall = false;
                        placeOrder();
                    }
                }
            }
        }
    }

    private static Set<OrderStatus> finalOrderStatus = new HashSet<>();

    static  {
        finalOrderStatus.add(OrderStatus.Filled);
        finalOrderStatus.add(OrderStatus.Cancelled);
        finalOrderStatus.add(OrderStatus.Inactive);
    }

    private void processOrderStatus(int orderId, OrderStatus orderStatus) {
        if (orderId == pendingOrderId) {
            if (orderStatus == OrderStatus.Inactive) {
                String message = "Log trader: order rejected";
                emailActorSelection.tell(new EmailActor.Email(message, message), self());
            }
            if (finalOrderStatus.contains(orderStatus)) {
                pendingOrderId = -1;
                pendingOrderSize = 0.0d;
                if (schedulePlaceOrderCall) {
                    schedulePlaceOrderCall = false;
                    placeOrder();
                }
            }
        }
    }

    private void onOpenOrder(IbGateway.OpenOrder m) {
        processOrderStatus(m.getOrderId(), m.getOrderState().status());
    }

    private void onOrderStatus(IbGateway.OrderStatus m) {
        OrderStatus orderStatus = OrderStatus.get(m.getStatus());
        processOrderStatus(m.getOrderId(), orderStatus);
    }

    boolean initializing = true;

    private void onExecDetailsEnd(IbGateway.ExecDetailsEnd m) {
        if (initializing) {
            startListenFileSignals();
            controller.onPosition(position);
        }
        initializing = false;
    }

    private void onPosition(IbGateway.Position m) {
        if (m.getAccount().equals(account)
                && m.getContract().localSymbol().equals(symbol)) {
            positionsBuffer.add(m);
        }
    }

    private void onPositionEnd(IbGateway.PositionEnd m) {
        for (IbGateway.Position pm : positionsBuffer) {
            if (pm.getPosition() != 0.0d) {
                position = pm.getPosition();
            }
        }
        storage.storePositions(position);
        positionsBuffer.clear();

        controller.onPosition(position);
        controller.onPositionsReloadComplete();
    }


}
