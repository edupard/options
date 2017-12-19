package com.skywind.delta_hedger.actors;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import com.ib.client.Contract;
import com.ib.client.ExecutionFilter;
import com.skywind.delta_hedger.ui.MainController;
import com.skywind.ib.IbGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.*;

import static com.skywind.delta_hedger.actors.HedgerActor.BEAN_NAME;

@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HedgerActor extends AbstractActor {
    public final static String BEAN_NAME = "hedgerActor";
    private final Logger LOGGER = LoggerFactory.getLogger(HedgerActor.class);

    private final UUID uuid = UUID.randomUUID();
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

    @Autowired
    private MainController controller;


    public HedgerActor() {
        ibGateway = new IbGateway(self(), uuid);
    }


    public static final class Start {
    }

    public static final class ReloadPositions {

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Start.class, m -> {
                    onStart(m);
                })
                .match(ReloadPositions.class, m -> {
                    onReloadPositions(m);
                })
                .match(IbGateway.ConnectAck.class, m ->{
                    if (m.isActorInstaceEquals(uuid)) {
                        onConnected(m);
                    }
                })
                .match(IbGateway.NextValidId.class, m->{
                    if (m.isActorInstaceEquals(uuid)) {
                        onNextValidId(m);
                    }
                })
                .match(IbGateway.ExecDetails.class, m->{
                    if (m.isActorInstaceEquals(uuid)) {
                        onExecDetails(m);
                    }
                })
                .match(IbGateway.ExecDetailsEnd.class, m->{
                    if (m.isActorInstaceEquals(uuid)) {
                        onExecDetailsEnd(m);
                    }
                })
                .match(IbGateway.Position.class, m->{
                    if (m.isActorInstaceEquals(uuid)) {
                        onPosition(m);
                    }
                })
                .match(IbGateway.PositionEnd.class, m->{
                    if (m.isActorInstaceEquals(uuid)) {
                        onPositionEnd(m);
                    }
                })
                .matchAny(m -> recievedUnknown(m))
                .build();
    }


    private final SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(), (Throwable t) -> SupervisorStrategy.escalate());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private void recievedUnknown(Object m) {
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        ibGateway.disconnect();
    }

    private void onStart(Start m) {
        positions = StorageUtils.readPositions();
        MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);
        for (Map.Entry<String, Position> e : positions.entrySet()) {
            uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, e.getKey(), e.getValue());
        }
        for (Trade t :StorageUtils.readTrades()) {
            processedTrades.add(t.getExecId());
        }

        ibGateway.connect(host, port, clientId);
    }

    private void onNextValidId(IbGateway.NextValidId m) {
        ibGateway.getClientSocket().reqExecutions(1, new ExecutionFilter());
    }

    private void onReloadPositions(ReloadPositions m) {
        requestPositions();
    }

//    private void requestExecutions() {
//        ibGateway.getClientSocket().reqExecutions(1, new ExecutionFilter());
//    }

    private List<IbGateway.Position> positionsBuffer = new LinkedList<>();

    private void requestPositions() {
        positionsBuffer.clear();
        ibGateway.getClientSocket().reqPositions();
    }

    private void onConnected(IbGateway.ConnectAck m) {
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
            }
            else {
                Contract c = m.getContract();
                double pos = Utils.getPosition(m);
                double posPx = m.getExecution().price();
                p = new Position(c, pos, posPx, 0.0d, 0.0d);
            }
            if (p.isZero()) {
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.DELETE, localSymbol, p);
                positions.remove(localSymbol);
            }
            else {
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, localSymbol, p);
                positions.put(localSymbol, p);
            }
        }
        StorageUtils.storeTrade(m);
        StorageUtils.storePositions(positions);
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
        Set<String> positionsSymbols = new HashSet<>();

        MainController.UpdateUiPositionsBatch uiUpdates = new MainController.UpdateUiPositionsBatch(false);

        for (IbGateway.Position pm : positionsBuffer) {
            if (pm.getPosition() != 0.0d) {
                Contract c = pm.getContract();
                c.exchange(exchange);
                String localSymbol = c.localSymbol();
                Position p = null;
                if (positions.containsKey(localSymbol)) {
                    p = positions.get(localSymbol);
                    p.setPos(pm.getPosition());
                    p.setPosPx(pm.getPositionPrice());
                }
                else {
                    p = new Position(c, pm.getPosition(), pm.getPositionPrice(), 0.0d, 0.0d);
                }
                positions.put(localSymbol, p);
                uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.UPDATE, localSymbol, p);
                positionsSymbols.add(localSymbol);
            }
        }

        Set<String> toDelete = new HashSet<>(positions.keySet());
        toDelete.removeAll(positionsSymbols);
        toDelete.stream().forEach(localSymbol->{
            uiUpdates.addAction(MainController.UpdateUiPositionsBatch.Action.DELETE, localSymbol, positions.get(localSymbol));
        });

        //remove legacy positions
        positions.keySet().retainAll(positionsSymbols);

        StorageUtils.storePositions(positions);
        positionsBuffer.clear();
    }

}
