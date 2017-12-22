package com.skywind.delta_hedger.actors;

import akka.actor.*;
import com.ib.client.*;
import com.skywind.delta_hedger.ui.MainController;
import com.skywind.ib.IbGateway;
import com.skywind.trading.spring_akka_integration.EmailActor;
import com.skywind.trading.spring_akka_integration.MessageSentToExactActorInstance;
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

    @Autowired
    private MainController controller;

    @Autowired
    private ActorSystem actorSystem;


    private ActorSelection emailActorSelection;


    public HedgerActor() {
        ibGateway = new IbGateway(self(), actorId);
    }


    public static final class Start {
    }

    public static final class ReloadPositions {

    }

    public static final class RefreshTimebars {

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
                .matchAny(m -> recievedUnknown(m))
                .build();
    }

    // should not be called at all
    private void onErrorWithMessage(IbGateway.ErrorWithMessage m) {
//        throw new RuntimeException(m.getErrorMsg());
    }

    // Handles errors generated within the API itself. If an exception is thrown within the API code it will be notified here. Possible cases include errors while reading the information from the socket or even mishandling at EWrapper's implementing class.
    private void onErrorWithException(IbGateway.ErrorWithException m) {
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

    @PostConstruct
    public void postConstruct() {
        controller.onApiConnection(apiConnection);
        controller.onIbConnection(ibConnection);
    }

    private void onErrorWithCode(IbGateway.ErrorWithCode m) {
        if (DISCONNECTED_FROM_IB_ERROR_CODES.contains(m.getErrorCode()))
        {
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
                refreshTimebars(true);
            }
        }

        if (FATAL_ERROR_CODES.contains(m.getErrorCode())) {
            String message = String.format("%d %s", m.getErrorCode(), m.getErrorMsg());
            emailActorSelection.tell(new EmailActor.Email("Options: fatal error", message), self());
            throw new FatalException();
        }

        if (m.getErrorCode() == 2106) {
            refreshTimebars(true);
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

    private void onStart(Start m) {
        start();
    }

    private void start() {
        positions = StorageUtils.readPositions();
        MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(true);
        for (Map.Entry<String, Position> e : positions.entrySet()) {
            uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, e.getKey(), e.getValue());
        }
        for (Trade t : StorageUtils.readTrades()) {
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

    private void onNextValidId(IbGateway.NextValidId m) {
        onPositionsUpdate();
        requestExecutions();
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
    }

    private Set<String> processedTrades = new HashSet<>();

    private void onExecDetails(IbGateway.ExecDetails m) {
        if (!processedTrades.contains(m.getExecution().execId()) && m.getExecution().acctNumber().equals(account)) {
            processedTrades.add(m.getExecution().execId());
            //update positions
            MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);
            String localSymbol = m.getContract().localSymbol();
            Position p = null;
            if (positions.containsKey(localSymbol)) {
                p = positions.get(localSymbol);
                p.updatePosition(m);
            } else {
                Contract c = m.getContract();
                double pos = Utils.getPosition(m);
                double posPx = m.getExecution().price();
                p = new Position(c, pos, posPx, 0.0d, 0.0d);
            }
            if (p.isZero()) {
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.DELETE, localSymbol, p);
                positions.remove(localSymbol);
            } else {
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, localSymbol, p);
                positions.put(localSymbol, p);
            }
            StorageUtils.storeTrade(m);
            StorageUtils.storePositions(positions);
            controller.onPositionsUpdate(uiUpdates);
            onPositionsUpdate();
        }
    }

    public static final class Timebar {
        private final String localSymbol;
        private final String duration;
        private final String barTime;
        private final double open;
        private final double high;
        private final double low;
        private final double close;
        private final double volume;

        public Timebar(String localSymbol, String duration, String barTime, double open, double high, double low, double close, double volume) {
            this.localSymbol = localSymbol;
            this.duration = duration;
            this.barTime = barTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        public String getLocalSymbol() {
            return localSymbol;
        }

        public String getBarTime() {
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

        public double getVolume() {
            return volume;
        }

        public String getDuration() {
            return duration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Timebar timebar = (Timebar) o;
            return Objects.equals(localSymbol, timebar.localSymbol) &&
                    Objects.equals(duration, timebar.duration) &&
                    Objects.equals(barTime, timebar.barTime);
        }

        @Override
        public int hashCode() {

            return Objects.hash(localSymbol, duration, barTime);
        }
    }

    public static final class TimebarArray {
        private final List<Timebar> bars = new LinkedList<>();

        public void onTimeBar(Timebar tb) {
            bars.add(tb);
        }
    }

    public static final class TimeBarRequest {
        private final String localSymbol;
        private final String duration;

        public TimeBarRequest(String localSymbol, String duration) {
            this.localSymbol = localSymbol;
            this.duration = duration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeBarRequest that = (TimeBarRequest) o;
            return Objects.equals(localSymbol, that.localSymbol) &&
                    Objects.equals(duration, that.duration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(localSymbol, duration);
        }

        public String getLocalSymbol() {
            return localSymbol;
        }

        public String getDuration() {
            return duration;
        }
    }

    private Map<Integer, TimeBarRequest> timeBarsRequestMap = new HashMap<>();
    private Map<TimeBarRequest, TimebarArray> currentBars = new HashMap<>();
    int currentHistDataRequestId = 1;

    private void onHistDataEndMsg(IbGateway.HistDataEndMsg m) {
    }

    private void onHistDataUpdateMsg(IbGateway.HistDataUpdateMsg m) {
        onTimeBar(m.getReqId(), m.getBar());
    }

    private void onTimeBar(int reqId, Bar bar) {
        TimeBarRequest r = timeBarsRequestMap.get(reqId);
        if (r == null) {
            return;
        }
        Timebar tb = new Timebar(
                r.localSymbol,
                r.duration,
                bar.time(),
                bar.open(),
                bar.high(),
                bar.low(),
                bar.close(),
                bar.volume()
        );
        TimebarArray arr = currentBars.get(r);
        if (arr == null) {
            arr = new TimebarArray();
            currentBars.put(r, arr);
        }
        arr.onTimeBar(tb);
        controller.onTimeBar(tb);
    }

    private void onHistDataMsg(IbGateway.HistDataMsg m) {
        onTimeBar(m.getReqId(), m.getBar());
    }

    private void onRefreshTimebars(RefreshTimebars m) {
        refreshTimebars(true);
    }

    private void refreshTimebars(boolean fullUpdate) {
        if (fullUpdate) {
            for (Integer reqId : timeBarsRequestMap.keySet()) {
                ibGateway.getClientSocket().cancelHistoricalData(reqId);
            }
            timeBarsRequestMap.clear();
            currentBars.clear();
            controller.onClearTimeBars();
        }
        Set<TimeBarRequest> requests = new HashSet<>();

        for (Map.Entry<String, Position> e : positions.entrySet()) {
            String localSymbol = e.getKey();
            Position p = e.getValue();
            if (p.getContract().secType() == Types.SecType.FUT) {
                requests.add(new TimeBarRequest(localSymbol, "4 hours"));
            } else if (p.getContract().secType() == Types.SecType.OPT) {
                if (p.isContractDetailsDefined()) {
                    requests.add(new TimeBarRequest(p.getContractDetails().underSymbol(), "4 hours"));
                }
            }
        }
        //unsubscribe for invalid requests
        Set<TimeBarRequest> requestsToTerminate = new HashSet<>();
        requestsToTerminate.addAll(timeBarsRequestMap.values());
        requestsToTerminate.removeAll(requests);

        for (Map.Entry<Integer, TimeBarRequest> e : timeBarsRequestMap.entrySet()) {
            if (requestsToTerminate.contains(e.getValue())) {
                ibGateway.getClientSocket().cancelHistoricalData(e.getKey());
            }
        }
        controller.onRemoveTimeBars(requestsToTerminate);


        //Request only new codes
        requests.removeAll(timeBarsRequestMap.values());
        for (TimeBarRequest r : requests) {
            Contract c = new Contract();
            c.localSymbol(r.localSymbol);
            c.secType(Types.SecType.FUT);
            c.exchange(exchange);
            c.currency(ccy);
            ibGateway.getClientSocket().reqHistoricalData(currentHistDataRequestId, c, "", "2 D", r.duration, "TRADES", 0, 1, true, null);
            timeBarsRequestMap.put(currentHistDataRequestId, r);
            currentHistDataRequestId++;
        }
    }

    private Map<Integer, String> contractDetailsRequestMap = new HashMap<>();
    int nextContractDetailsRequestId = 1;

    private void requestContractDetails() {
        for (Map.Entry<String, Position> e : positions.entrySet()) {
            String localSymbol = e.getKey();
            Position p = e.getValue();
            if (!p.isContractDetailsDefined()) {
                contractDetailsRequestMap.put(nextContractDetailsRequestId, localSymbol);
                ibGateway.getClientSocket().reqContractDetails(nextContractDetailsRequestId, p.getContract());
                nextContractDetailsRequestId++;
            }
        }
    }

    private void onPositionsUpdate() {
        requestContractDetails();
        refreshTimebars(false);
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
            positionsBuffer.add(m);
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
                    if (oldPositions.containsKey(localSymbol)) {
                        vol = oldPositions.get(localSymbol).getVol();
                        ir = oldPositions.get(localSymbol).getIr();
                    }
                    Position p = new Position(c, pm.getPosition(), pm.getPositionPrice(), vol, ir);
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
