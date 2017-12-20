package com.skywind.ib;

import akka.actor.ActorRef;
import com.ib.client.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.skywind.trading.spring_akka_integration.MessageSentToExactActorInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IbGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(IbGateway.class);

    private final ActorRef parent;
    private final UUID actorId;

    private final EReaderSignal readerSignal;
    private final EClientSocket clientSocket;
    private Thread readerThread;


    public IbGateway(ActorRef parent, UUID actorId) {
        this.parent = parent;
        this.actorId = actorId;
        this.readerSignal = new EJavaSignal();
        this.clientSocket = new EClientSocket(new Wrapper(), readerSignal);

    }

    public void connect(String host, int port, int clientId) {
        clientSocket.eConnect(host, port, clientId);
    }

    public void disconnect() {
        clientSocket.eDisconnect();
        readerSignal.issueSignal();
    }

    public EClientSocket getClientSocket() {
        return clientSocket;
    }

    public static void log(String callbackName, Object... args) {
        StringBuilder logTemplate = new StringBuilder("IB: ");
        logTemplate.append(callbackName);
        if (args != null) {

            for (int i = 0; i < args.length; ++i) {
                logTemplate.append(" {}");
            }
        }

        LOGGER.debug(logTemplate.toString(), args);
    }


    public static final class NextValidId extends MessageSentToExactActorInstance {

        public NextValidId(int nextValidId, UUID actorId) {
            super(actorId);
            this.nextValidId = nextValidId;
        }

        private final int nextValidId;

        public int getNextValidId() {
            return nextValidId;
        }

    }

    public static final class ExecDetails extends MessageSentToExactActorInstance {

        private final int reqId;
        private final Contract contract;
        private final Execution execution;

        public ExecDetails(int reqId, Contract contract, Execution execution, UUID actorId) {
            super(actorId);
            this.reqId = reqId;
            this.contract = contract;
            this.execution = execution;
        }

        public int getReqId() {
            return reqId;
        }

        public Contract getContract() {
            return contract;
        }

        public Execution getExecution() {
            return execution;
        }

    }

    public static final class ExecDetailsEnd extends MessageSentToExactActorInstance {

        private final int reqId;

        public ExecDetailsEnd(int reqId, UUID actorId) {
            super(actorId);
            this.reqId = reqId;
        }

        public int getReqId() {
            return reqId;
        }

    }

    public static final class ConnectionClosed extends MessageSentToExactActorInstance {

        public ConnectionClosed(UUID actorId) {
            super(actorId);
        }

    }

    public static final class ErrorWithException extends MessageSentToExactActorInstance {

        private final Exception reason;

        public ErrorWithException(Exception reason, UUID actorId) {
            super(actorId);
            this.reason = reason;
        }

        public Exception getReason() {
            return reason;
        }

    }

    public static final class ErrorWithMessage extends MessageSentToExactActorInstance {

        private final String errorMsg;

        public ErrorWithMessage(String errorMsg, UUID actorId) {
            super(actorId);
            this.errorMsg = errorMsg;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

    }

    public static final class ErrorWithCode extends MessageSentToExactActorInstance {

        private final int id;
        private final int errorCode;
        private final String errorMsg;

        public ErrorWithCode(int id, int errorCode, String errorMsg, UUID actorId) {
            super(actorId);
            this.id = id;
            this.errorCode = errorCode;
            this.errorMsg = errorMsg;
        }

        public int getId() {
            return id;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

    }

    public static final class ConnectAck extends MessageSentToExactActorInstance {

        public ConnectAck(UUID actorId) {
            super(actorId);
        }

    }

    public static final class Position extends MessageSentToExactActorInstance {

        private final String account;
        private final Contract contract;
        private final double position;
        private final double positionPrice;

        public Position(String account, Contract contract, double position, double positionPrice, UUID actorId) {
            super(actorId);
            this.account = account;
            this.contract = contract;
            this.position = position;
            this.positionPrice = positionPrice;
        }

        public String getAccount() {
            return account;
        }

        public Contract getContract() {
            return contract;
        }

        public double getPosition() {
            return position;
        }

        public double getPositionPrice() {
            return positionPrice;
        }

    }

    public static final class PositionEnd extends MessageSentToExactActorInstance {

        public PositionEnd(UUID actorId) {
            super(actorId);
        }

    }

    public static final class ManagedAccounts extends MessageSentToExactActorInstance {

        private final String accounts;

        public ManagedAccounts(String accounts, UUID actorId) {
            super(actorId);
            this.accounts = accounts;
        }

        public String getAccounts() {
            return accounts;
        }
    }


    public static final class OrderStatus extends MessageSentToExactActorInstance {

        private final int orderId;
        private final String status;
        private final double filled;
        private final double remaining;
        private final double avgFillPrice;
        private final int permId;
        private final int parentId;
        private final double lastFillPrice;
        private final int clientId;
        private final String whyHeld;
        private final double mktCapPrice;

        public OrderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice, UUID actorId) {
            super(actorId);
            this.orderId = orderId;
            this.status = status;
            this.filled = filled;
            this.remaining = remaining;
            this.avgFillPrice = avgFillPrice;
            this.permId = permId;
            this.parentId = parentId;
            this.lastFillPrice = lastFillPrice;
            this.clientId = clientId;
            this.whyHeld = whyHeld;
            this.mktCapPrice = mktCapPrice;
        }

        public int getOrderId() {
            return orderId;
        }

        public String getStatus() {
            return status;
        }

        public double getFilled() {
            return filled;
        }

        public double getRemaining() {
            return remaining;
        }

        public double getAvgFillPrice() {
            return avgFillPrice;
        }

        public int getPermId() {
            return permId;
        }

        public int getParentId() {
            return parentId;
        }

        public double getLastFillPrice() {
            return lastFillPrice;
        }

        public int getClientId() {
            return clientId;
        }

        public String getWhyHeld() {
            return whyHeld;
        }

    }

    public static final class UpdatePortfolio extends MessageSentToExactActorInstance {

        private final Contract contract;
        private final double position;
        private final double marketPrice;
        private final double marketValue;
        private final double averageCost;
        private final double unrealisedPNL;
        private final double realisedPNL;
        private final String accountName;

        public UpdatePortfolio(Contract contract, double position, double marketPrice, double marketValue, double averageCost, double unrealisedPNL, double realisedPNL, String accountName, UUID actorId) {
            super(actorId);
            this.contract = contract;
            this.position = position;
            this.marketPrice = marketPrice;
            this.marketValue = marketValue;
            this.averageCost = averageCost;
            this.unrealisedPNL = unrealisedPNL;
            this.realisedPNL = realisedPNL;
            this.accountName = accountName;
        }

        public Contract getContract() {
            return contract;
        }

        public double getPosition() {
            return position;
        }

        public double getMarketPrice() {
            return marketPrice;
        }

        public double getMarketValue() {
            return marketValue;
        }

        public double getAverageCost() {
            return averageCost;
        }

        public double getUnrealisedPNL() {
            return unrealisedPNL;
        }

        public double getRealisedPNL() {
            return realisedPNL;
        }

        public String getAccountName() {
            return accountName;
        }

    }

    public static final class HistDataEndMsg extends MessageSentToExactActorInstance {
        private final int reqId;
        private final String startDateStr;
        private final String endDateStr;

        public HistDataEndMsg(int reqId, String startDateStr, String endDateStr, UUID actorId) {
            super(actorId);
            this.reqId = reqId;
            this.startDateStr = startDateStr;
            this.endDateStr = endDateStr;

        }

        public int getReqId() {
            return reqId;
        }

        public String getEndDateStr() {
            return endDateStr;
        }

        public String getStartDateStr() {
            return startDateStr;
        }
    }

    public static final class HistDataMsg extends MessageSentToExactActorInstance {

        private final int reqId;
        private final Bar bar;

        public HistDataMsg(int reqId, Bar bar, UUID actorId) {
            super(actorId);
            this.reqId = reqId;
            this.bar = bar;
        }

        public int getReqId() {
            return reqId;
        }

        public Bar getBar() {
            return bar;
        }
    }

    public static final class ContractDetailsMsg extends MessageSentToExactActorInstance {

        private final int reqId;
        private final ContractDetails contractDetails;

        private ContractDetailsMsg(int reqId, ContractDetails contractDetails, UUID actorId) {
            super(actorId);
            this.reqId = reqId;
            this.contractDetails = contractDetails;
        }

        public int getReqId() {
            return reqId;
        }

        public ContractDetails getContractDetails() {
            return contractDetails;
        }

    }

    public static final class AccountDownloadEnd extends MessageSentToExactActorInstance {

        private final String account;

        public AccountDownloadEnd(String account, UUID actorId) {
            super(actorId);
            this.account = account;
        }

        public String getAccount() {
            return account;
        }

    }

    public static final class UpdateAccountTime extends MessageSentToExactActorInstance {

        private final String timestamp;

        public UpdateAccountTime(String timestamp, UUID actorId) {
            super(actorId);
            this.timestamp = timestamp;
        }

        public String getTimestamp() {
            return timestamp;
        }

    }

    public static final class UpdateAccountValue extends MessageSentToExactActorInstance {

        private final String key;
        private final String value;
        private final String currency;
        private final String accountName;

        public UpdateAccountValue(String key, String value, String currency, String accountName, UUID actorId) {
            super(actorId);
            this.key = key;
            this.value = value;
            this.currency = currency;
            this.accountName = accountName;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getCurrency() {
            return currency;
        }

        public String getAccountName() {
            return accountName;
        }

    }

    public static final class OpenOrder extends MessageSentToExactActorInstance {

        private final int orderId;
        private final Contract contract;
        private final Order order;
        private final OrderState orderState;

        public OpenOrder(int orderId, Contract contract, Order order, OrderState orderState, UUID actorId) {
            super(actorId);
            this.orderId = orderId;
            this.contract = contract;
            this.order = order;
            this.orderState = orderState;
        }

        public int getOrderId() {
            return orderId;
        }

        public Contract getContract() {
            return contract;
        }

        public Order getOrder() {
            return order;
        }

        public OrderState getOrderState() {
            return orderState;
        }

    }

    public static final class TickPrice extends MessageSentToExactActorInstance {

        private final int tickerId;
        private final int field;
        private final double price;
        private final TickAttr tickAttr;

        public TickPrice(int tickerId, int field, double price, TickAttr tickAttr, UUID actorId) {
            super(actorId);
            this.tickerId = tickerId;
            this.field = field;
            this.price = price;
            this.tickAttr = tickAttr;
        }

        public boolean isBid() {
            return field == 1;
        }

        public boolean isAsk() {
            return field == 2;
        }

        public boolean isLast() {
            return field == 4;
        }

        public boolean isClose() {
            return field == 9;
        }

        public int getTickerId() {
            return tickerId;
        }

        public int getField() {
            return field;
        }

        public double getPrice() {
            return price;
        }

        public TickAttr getTickAttr() {
            return tickAttr;
        }
    }

    public static final class OpenOrderEnd extends MessageSentToExactActorInstance {

        public OpenOrderEnd(UUID actorId) {
            super(actorId);
        }

    }


    private class Wrapper implements EWrapper {
        @Override
        public void connectAck() {
            log("connectAck");
            if (clientSocket.isAsyncEConnect()) {
                LOGGER.debug("Acknowledging connection");
                clientSocket.startAPI();
            }
            final EReader reader = new EReader(clientSocket, readerSignal);
            reader.start();
            readerThread = new Thread() {
                public void run() {
                    LOGGER.debug("EReader thread started");
                    try {
                        while (clientSocket.isConnected()) {
                            readerSignal.waitForSignal();
                            reader.processMsgs();
                        }
                    } catch (Throwable t) {
                        LOGGER.error("", t);
                    }
                    LOGGER.debug("EReader thread stopped");
                }
            };
            readerThread.start();
            parent.tell(new ConnectAck(actorId), parent);
        }

        @Override
        public void nextValidId(int nextValidId) {
            log("nextValidId",
                    "nextValidId:",
                    nextValidId);
            parent.tell(new NextValidId(nextValidId, actorId), parent);
        }

        @Override
        public void execDetails(int reqId, Contract contract, Execution execution) {
            log("execDetails",
                    "reqId:",
                    reqId,
                    "contract:",
                    contract,
                    "execution:",
                    execution,
                    "execId:",
                    execution.execId(),
                    "acctNumber:",
                    execution.acctNumber(),
                    "shares:",
                    execution.shares(),
                    "side:",
                    execution.side(),
                    "time:",
                    execution.time(),
                    "clientId:",
                    execution.clientId(),
                    "price:",
                    execution.price(),
                    "avgPrice:",
                    execution.avgPrice(),
                    "cumQty:",
                    execution.cumQty(),
                    "evMultiplier:",
                    execution.evMultiplier(),
                    "evRule:",
                    execution.evRule(),
                    "exchange:",
                    execution.exchange(),
                    "liquidation:",
                    execution.liquidation(),
                    "modelCode:",
                    execution.modelCode(),
                    "orderId:",
                    execution.orderId(),
                    "orderRef:",
                    execution.orderRef(),
                    "permId:",
                    execution.permId()
            );
            parent.tell(new ExecDetails(reqId, contract, execution, actorId), parent);
        }

        @Override
        public void execDetailsEnd(int reqId) {
            log("execDetailsEnd",
                    "reqId:",
                    reqId);
            parent.tell(new ExecDetailsEnd(reqId, actorId), parent);
        }

        @Override
        public void connectionClosed() {
            log("connectionClosed");
            parent.tell(new ConnectionClosed(actorId), parent);
        }

        @Override
        public void error(Exception reason) {
            log("error",
                    "reason:",
                    reason);
            parent.tell(new ErrorWithException(reason, actorId), parent);
        }

        @Override
        public void error(String errorMsg) {
            log("error",
                    "errorMsg:",
                    errorMsg);
            parent.tell(new ErrorWithMessage(errorMsg, actorId), parent);
        }

        @Override
        public void error(int id, int errorCode, String errorMsg) {
            log("error",
                    "id:",
                    id,
                    "errorCode:",
                    errorCode,
                    "errorMsg:",
                    errorMsg);
            parent.tell(new ErrorWithCode(id, errorCode, errorMsg, actorId), parent);
        }

        @Override
        public void position(String account, Contract contract, double position, double positionPrice) {
            log("position",
                    "account:",
                    account,
                    "contract:",
                    contract,
                    "position:",
                    position,
                    "positionPrice:",
                    positionPrice);
            parent.tell(new Position(account, contract, position, positionPrice, actorId), parent);
        }

        @Override
        public void positionEnd() {
            log("positionEnd");
            parent.tell(new PositionEnd(actorId), parent);
        }

        @Override
        public void managedAccounts(String accounts) {
            log("managedAccounts",
                    "accounts:",
                    accounts);
            parent.tell(new ManagedAccounts(accounts, actorId), parent);
        }

        @Override
        public void orderStatus(int orderId, String status, double filled,
                                double remaining, double avgFillPrice, int permId, int parentId,
                                double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
            log("orderStatus",
                    "orderId:",
                    orderId,
                    "status:",
                    status,
                    "filled:",
                    filled,
                    "remaining:",
                    remaining,
                    "avgFillPrice:",
                    avgFillPrice,
                    "permId:",
                    permId,
                    "parentId:",
                    parentId,
                    "lastFillPrice:",
                    lastFillPrice,
                    "clientId:",
                    clientId,
                    "whyHeld:",
                    whyHeld,
                    "mktCapPrice:",
                    mktCapPrice);
            parent.tell(new OrderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice, actorId), parent);

        }

        @Override
        public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
            log("openOrder",
                    "orderId:",
                    orderId,
                    "contract:",
                    contract,
                    "contract.localSymbol:",
                    contract.localSymbol(),
                    "order:",
                    order,
                    "order.action:",
                    order.action(),
                    "order.orderType:",
                    order.orderType(),
                    "order.totalQuantity:",
                    order.totalQuantity(),
                    "orderState:",
                    orderState,
                    "orderState.status",
                    orderState.status());
            parent.tell(new OpenOrder(orderId, contract, order, orderState, actorId), parent);
        }

        @Override
        public void openOrderEnd() {
            log("openOrderEnd");
            parent.tell(new OpenOrderEnd(actorId), parent);
        }

        @Override
        public void tickPrice(int tickerId, int field, double price, TickAttr tickAttr) {
            parent.tell(new TickPrice(tickerId, field, price, tickAttr, actorId), parent);
            log("tickPrice", tickerId, field, price, tickAttr);
        }

        @Override
        public void tickSize(int i, int i1, int i2) {
            log("tickSize", i, i1, i2);
        }

        @Override
        public void tickOptionComputation(int i, int i1, double d, double d1, double d2, double d3, double d4, double d5, double d6, double d7) {
            log("tickOptionComputation", i, i1, d, d1, d2, d3, d4, d5, d6, d7);
        }

        @Override
        public void tickGeneric(int i, int i1, double d) {
            log("tickGeneric", i, i1, d);
        }

        @Override
        public void tickString(int i, int i1, String string) {
            log("tickString", i, i1, string);
        }

        @Override
        public void tickEFP(int i, int i1, double d, String string, double d1, int i2, String string1, double d2, double d3) {
            log("tickEFP", i, i1, d, string, d1, i2, string1, d2, d3);
        }

        @Override
        public void updateAccountValue(String key, String value, String currency, String accountName) {
            log("updateAccountValue", key, value, currency, accountName);
            parent.tell(new UpdateAccountValue(key, value, currency, accountName, actorId), parent);
        }

        @Override
        public void updatePortfolio(Contract contract, double position, double marketPrice, double marketValue, double averageCost, double unrealisedPNL, double realisedPNL, String accountName) {
            log("updatePortfolio", contract, position, marketPrice, marketValue, averageCost, unrealisedPNL, realisedPNL, accountName);
            parent.tell(new UpdatePortfolio(contract, position, marketPrice, marketValue, averageCost, unrealisedPNL, realisedPNL, accountName, actorId), parent);
        }

        @Override
        public void updateAccountTime(String timestamp) {
            log("updateAccountTime", timestamp);
            parent.tell(new UpdateAccountTime(timestamp, actorId), parent);

        }

        @Override
        public void accountDownloadEnd(String account) {
            log("accountDownloadEnd", account);
            parent.tell(new AccountDownloadEnd(account, actorId), parent);

        }

        @Override
        public void contractDetails(int reqId, ContractDetails contractDetails) {
            log("contractDetails",
                    "reqId:",
                    reqId,
                    "contractDetails:",
                    contractDetails);
            parent.tell(new ContractDetailsMsg(reqId, contractDetails, actorId), parent);
        }

        @Override
        public void bondContractDetails(int i, ContractDetails cd) {
            log("bondContractDetails", i, cd);
        }

        @Override
        public void contractDetailsEnd(int reqId) {
            log("contractDetailsEnd",
                    "reqId:",
                    reqId);
        }

        @Override
        public void updateMktDepth(int i, int i1, int i2, int i3, double d, int i4) {
            log("updateMktDepth", i, i1, i2, i3, d, i4);
        }

        @Override
        public void updateMktDepthL2(int i, int i1, String string, int i2, int i3, double d, int i4) {
            log("updateMktDepthL2", i, i1, string, i2, i3, d, i4);
        }

        @Override
        public void updateNewsBulletin(int i, int i1, String string, String string1) {
            log("updateNewsBulletin", i, i1, string, string1);
        }

        @Override
        public void receiveFA(int i, String string) {
            log("receiveFA", i, string);
        }

        @Override
        public void historicalData(int reqId, Bar bar) {
            log("historicalData", reqId, bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume(), bar.count(), bar.wap());
            parent.tell(new HistDataMsg(reqId, bar, actorId), parent);
        }

        @Override
        public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
            log("historicalDataEnd", reqId, startDateStr, endDateStr);
            parent.tell(new HistDataEndMsg(reqId, startDateStr, endDateStr, actorId), parent);
        }

        @Override
        public void scannerParameters(String string) {
            log("scannerParameters", string);
        }

        @Override
        public void scannerData(int i, int i1, ContractDetails cd, String string, String string1, String string2, String string3) {
            log("scannerData", i, i1, cd, string, string1, string2, string3);
        }

        @Override
        public void scannerDataEnd(int i) {
            log("scannerDataEnd", i);
        }

        @Override
        public void realtimeBar(int i, long l, double d, double d1, double d2, double d3, long l1, double d4, int i1) {
            log("realtimeBar", i, l, d, d1, d2, d3, l1, d4, i1);
        }

        @Override
        public void currentTime(long l) {
            log("currentTime", l);
        }

        @Override
        public void fundamentalData(int i, String string) {
            log("fundamentalData", i, string);
        }

        @Override
        public void deltaNeutralValidation(int i, DeltaNeutralContract dnc) {
            log("deltaNeutralValidation", i, dnc);
        }

        @Override
        public void tickSnapshotEnd(int i) {
            log("tickSnapshotEnd", i);
        }

        @Override
        public void marketDataType(int i, int i1) {
            log("marketDataType", i, i1);
        }

        @Override
        public void commissionReport(CommissionReport cr) {
            log("commissionReport", cr);
        }

        @Override
        public void accountSummary(int i, String string, String string1, String string2, String string3) {
            log("accountSummary", i, string, string1, string2, string3);
        }

        @Override
        public void accountSummaryEnd(int i) {
            log("accountSummaryEnd", i);
        }

        @Override
        public void verifyMessageAPI(String string) {
            log("verifyMessageAPI", string);
        }

        @Override
        public void verifyCompleted(boolean bln, String string) {
            log("verifyCompleted", bln, string);
        }

        @Override
        public void verifyAndAuthMessageAPI(String string, String string1) {
            log("verifyAndAuthMessageAPI", string, string1);
        }

        @Override
        public void verifyAndAuthCompleted(boolean bln, String string) {
            log("verifyAndAuthCompleted", bln, string);
        }

        @Override
        public void displayGroupList(int i, String string) {
            log("displayGroupList", i, string);
        }

        @Override
        public void displayGroupUpdated(int i, String string) {
            log("displayGroupUpdated", i, string);
        }

        @Override
        public void positionMulti(int i, String string, String string1, Contract cntrct, double d, double d1) {
            log("positionMulti", i, string, string1, cntrct, d, d1);
        }

        @Override
        public void positionMultiEnd(int i) {
            log("positionMultiEnd", i);
        }

        @Override
        public void accountUpdateMulti(int i, String string, String string1, String string2, String string3, String string4) {
            log("accountUpdateMulti", i, string, string1, string2, string3, string4);
        }

        @Override
        public void accountUpdateMultiEnd(int i) {
            log("accountUpdateMultiEnd", i);
        }

        @Override
        public void securityDefinitionOptionalParameter(int i, String string, int i1, String string1, String string2, Set<String> set, Set<Double> set1) {
            log("securityDefinitionOptionalParameter", i, string, i1, string1, string2, set, set1);
        }

        @Override
        public void securityDefinitionOptionalParameterEnd(int i) {
            log("securityDefinitionOptionalParameterEnd", i);
        }

        @Override
        public void softDollarTiers(int i, SoftDollarTier[] sdts) {
            log("softDollarTiers", i, sdts);
        }

        @Override
        public void familyCodes(FamilyCode[] familyCodes) {
            log("familyCodes", familyCodes);
        }

        @Override
        public void symbolSamples(int i, ContractDescription[] contractDescriptions) {
            log("symbolSamples", i, contractDescriptions);
        }

        @Override
        public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {

        }

        @Override
        public void tickNews(int i, long l, String s, String s1, String s2, String s3) {

        }

        @Override
        public void smartComponents(int i, Map<Integer, Map.Entry<String, Character>> map) {

        }

        @Override
        public void tickReqParams(int i, double v, String s, int i1) {

        }

        @Override
        public void newsProviders(NewsProvider[] newsProviders) {

        }

        @Override
        public void newsArticle(int i, int i1, String s) {

        }

        @Override
        public void historicalNews(int i, String s, String s1, String s2, String s3) {

        }

        @Override
        public void historicalNewsEnd(int i, boolean b) {

        }

        @Override
        public void headTimestamp(int i, String s) {

        }

        @Override
        public void histogramData(int i, List<HistogramEntry> list) {

        }

        @Override
        public void historicalDataUpdate(int i, Bar bar) {

        }

        @Override
        public void rerouteMktDataReq(int i, int i1, String s) {

        }

        @Override
        public void rerouteMktDepthReq(int i, int i1, String s) {

        }

        @Override
        public void marketRule(int i, PriceIncrement[] priceIncrements) {

        }

        @Override
        public void pnl(int i, double v, double v1, double v2) {

        }

        @Override
        public void pnlSingle(int i, int i1, double v, double v1, double v2, double v3) {

        }

        @Override
        public void historicalTicks(int i, List<HistoricalTick> list, boolean b) {

        }

        @Override
        public void historicalTicksBidAsk(int i, List<HistoricalTickBidAsk> list, boolean b) {

        }

        @Override
        public void historicalTicksLast(int i, List<HistoricalTickLast> list, boolean b) {

        }
    }
}
