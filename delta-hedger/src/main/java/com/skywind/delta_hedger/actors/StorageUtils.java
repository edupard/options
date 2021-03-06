package com.skywind.delta_hedger.actors;

import com.ib.client.Contract;
import com.skywind.ib.IbGateway;
import com.skywind.ib.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class StorageUtils {
    private static final String COLUMN_SELECTED = "Selected";
    private static final String COLUMN_LOCAL_SYMBOL = "LocalSymbol";
    private static final String COLUMN_SYMBOL = "Symbol";
    private static final String COLUMN_SEC_TYPE = "SecType";
    private static final String COLUMN_CURRENCY = "Currency";
    private static final String COLUMN_EXCHANGE = "Exchange";
    private static final String COLUMN_LAST_TRADE_DATE = "LastTradeDate";
    private static final String COLUMN_STRIKE = "Strike";
    private static final String COLUMN_RIGHT = "Right";
    private static final String COLUMN_MULTIPLIER = "Multiplier";
    private static final String COLUMN_POSITION = "Position";
    private static final String COLUMN_PRICE = "Price";
    private static final String COLUMN_EXEC_ID = "ExecId";
    private static final String COLUMN_TIME = "Time";
    private static final String COLUMN_IR = "Ir";
    private static final String COLUMN_VOL = "Vol";
    private static final String COLUMN_CODE = "Code";
    private static final String COLUMN_UNDERLYING_CODE = "Underlying";
    private static final String COLUMN_EXPIRY = "Expiry";

    private static final String COLUMN_BAR_LENGTH = "Length";
    private static final String COLUMN_OPEN = "Open";
    private static final String COLUMN_HIGH = "High";
    private static final String COLUMN_LOW = "Low";
    private static final String COLUMN_CLOSE = "Close";
    private static final String COLUMN_VOLUME = "Volume";

    private static final String DATA_DIR_PATH = "data";
    private static final String POSITIONS_FILE_PATH = "data/positions.csv";
    private static final String TRADES_FILE_PATH = "data/trades.csv";

    private final static CSVFormat CSV_POSITIONS_FORMAT = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator())
            .withHeader(
                    COLUMN_SELECTED,
                    COLUMN_LOCAL_SYMBOL,
                    COLUMN_SYMBOL,
                    COLUMN_SEC_TYPE,
                    COLUMN_CURRENCY,
                    COLUMN_EXCHANGE,
                    COLUMN_LAST_TRADE_DATE,
                    COLUMN_STRIKE,
                    COLUMN_RIGHT,
                    COLUMN_MULTIPLIER,
                    COLUMN_POSITION,
                    COLUMN_PRICE,
                    COLUMN_IR,
                    COLUMN_VOL
            );

    private final static CSVFormat CSV_TRADES_FORMAT_WITHOUT_HEADER = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator());

    private final static CSVFormat CSV_TRADES_FORMAT = CSV_TRADES_FORMAT_WITHOUT_HEADER.withHeader(
            COLUMN_LOCAL_SYMBOL,
            COLUMN_SYMBOL,
            COLUMN_SEC_TYPE,
            COLUMN_CURRENCY,
            COLUMN_EXCHANGE,
            COLUMN_LAST_TRADE_DATE,
            COLUMN_STRIKE,
            COLUMN_RIGHT,
            COLUMN_MULTIPLIER,
            COLUMN_POSITION,
            COLUMN_PRICE,
            COLUMN_EXEC_ID,
            COLUMN_TIME
    );

    private final static CSVFormat CSV_INPUT_POSITIONS_FORMAT = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator())
            .withHeader(
                    COLUMN_CODE,
                    COLUMN_UNDERLYING_CODE,
                    COLUMN_SEC_TYPE,
                    COLUMN_EXPIRY,
                    COLUMN_STRIKE,
                    COLUMN_RIGHT,
                    COLUMN_MULTIPLIER,
                    COLUMN_POSITION,
                    COLUMN_PRICE,
                    COLUMN_IR,
                    COLUMN_VOL
            );

    private static final DateTimeFormatter TIME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    public static void storeInputPositions(List<Position> inputPositions, String positionsFileName) throws IOException {
        try (CSVPrinter writer = new CSVPrinter(new FileWriter(positionsFileName, false), CSV_INPUT_POSITIONS_FORMAT)) {
            for (Position p : inputPositions) {
                if (p.isSelected()) {
                    writer.printRecord(
                            p.getContract().localSymbol(),
                            p.getContractDetails() == null ? "" : p.getContractDetails().underSymbol(),
                            p.getContract().secType(),
                            p.getExpiry() == null ? "" : TIME_FMT.format(p.getExpiry()),
                            p.getContract().strike(),
                            p.getContract().right(),
                            p.getContract().multiplier(),
                            p.getPos(),
                            p.getPosPx(),
                            p.getIr(),
                            p.getVol()
                    );
                }
            }
        }
    }

    private final static CSVFormat CSV_INPUT_TIME_BARS_FORMAT = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator())
            .withHeader(
                    COLUMN_CODE,
                    COLUMN_TIME,
                    COLUMN_BAR_LENGTH,
                    COLUMN_OPEN,
                    COLUMN_HIGH,
                    COLUMN_LOW,
                    COLUMN_CLOSE,
                    COLUMN_VOLUME
            );

    public static void storeInputBars(List<HedgerActor.Timebar> inputTimebars, String inputTimeBarsFileName) throws IOException {
        try (CSVPrinter writer = new CSVPrinter(new FileWriter(inputTimeBarsFileName, false), CSV_INPUT_TIME_BARS_FORMAT)) {
            for (HedgerActor.Timebar tb : inputTimebars) {
                writer.printRecord(
                        tb.getLocalSymbol(),
                        TIME_FMT.format(tb.getBarTime()),
                        tb.getSize(),
                        tb.getOpen(),
                        tb.getHigh(),
                        tb.getLow(),
                        tb.getClose(),
                        tb.getVolume()
                );
            }
        }
    }

    private static final String COLUMN_TGT_ORDERS_IDX = "idx";
    private static final String COLUMN_TGT_ORDERS_CODE = "code";
    private static final String COLUMN_TGT_ORDERS_PX = "px";
    private static final String COLUMN_TGT_ORDERS_VIEW_PX = "view_px";
    private static final String COLUMN_TGT_ORDERS_QTY = "qty";
    private static final String COLUMN_TGT_ORDERS_TYPE = "type";


    private final static CSVFormat CSV_TARGER_ORDERS_FORMAT = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator())
            .withHeader(
                    COLUMN_TGT_ORDERS_CODE,
                    COLUMN_TGT_ORDERS_IDX,
                    COLUMN_TGT_ORDERS_PX,
                    COLUMN_TGT_ORDERS_QTY,
                    COLUMN_TGT_ORDERS_TYPE,
                    COLUMN_TGT_ORDERS_VIEW_PX
            );

    public static List<TargetOrder> readTargetOrders(String folderName) {
        List<TargetOrder> result = new LinkedList<>();
        String tgtOrdersPath = String.format("%s\\target_orders.csv", folderName);
        Path path = Paths.get(tgtOrdersPath);
        if (Files.exists(path)) {
            try (CSVParser reader = new CSVParser(new FileReader(tgtOrdersPath), CSV_TARGER_ORDERS_FORMAT.withSkipHeaderRecord())) {
                for (CSVRecord r : reader) {
                    String code = r.get(COLUMN_TGT_ORDERS_CODE);
                    int idx = Integer.parseInt(r.get(COLUMN_TGT_ORDERS_IDX));
                    double px = Double.parseDouble(r.get(COLUMN_TGT_ORDERS_PX));
                    String viewPx = r.get(COLUMN_TGT_ORDERS_VIEW_PX);
                    double qty = Double.parseDouble(r.get(COLUMN_TGT_ORDERS_QTY));
                    String orderType = r.get(COLUMN_TGT_ORDERS_TYPE);
                    result.add(new TargetOrder(idx, code, px, viewPx, qty, orderType));
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Target order file absent");
        }
        return result;
    }

    public static void storePositions(Map<String, Position> positionsByLocalSymbol) {

        Path path = Paths.get(DATA_DIR_PATH);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (CSVPrinter writer = new CSVPrinter(new FileWriter(POSITIONS_FILE_PATH, false), CSV_POSITIONS_FORMAT)) {
            for (Map.Entry<String, Position> entry : positionsByLocalSymbol.entrySet()) {
                Position pi = entry.getValue();
                writer.printRecord(
                        pi.isSelected(),
                        pi.getContract().localSymbol(),
                        pi.getContract().symbol(),
                        pi.getContract().secType(),
                        pi.getContract().currency(),
                        pi.getContract().exchange(),
                        pi.getContract().lastTradeDateOrContractMonth(),
                        pi.getContract().strike(),
                        pi.getContract().right(),
                        pi.getContract().multiplier(),
                        pi.getPos(),
                        pi.getPosPx(),
                        pi.getIr(),
                        pi.getVol()
                );
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    public static Map<String, Position> readPositions() {
        Map<String, Position> positions = new HashMap<>();
        Path path = Paths.get(POSITIONS_FILE_PATH);
        if (Files.exists(path)) {
            try (CSVParser reader = new CSVParser(new FileReader(POSITIONS_FILE_PATH), CSV_POSITIONS_FORMAT.withSkipHeaderRecord())) {
                for (CSVRecord r : reader) {
                    Contract c = new Contract();
                    c.localSymbol(r.get(COLUMN_LOCAL_SYMBOL));
                    c.symbol(r.get(COLUMN_SYMBOL));
                    c.secType(r.get(COLUMN_SEC_TYPE));
                    c.currency(r.get(COLUMN_CURRENCY));
                    c.exchange(r.get(COLUMN_EXCHANGE));
                    c.lastTradeDateOrContractMonth(r.get(COLUMN_LAST_TRADE_DATE));
                    c.strike(Double.parseDouble(r.get(COLUMN_STRIKE)));
                    c.right(r.get(COLUMN_RIGHT));
                    c.multiplier(r.get(COLUMN_MULTIPLIER));


                    boolean selected = Boolean.parseBoolean(r.get(COLUMN_SELECTED));
                    double pos = Double.parseDouble(r.get(COLUMN_POSITION));
                    double posPx = Double.parseDouble(r.get(COLUMN_PRICE));
                    double ir = Double.parseDouble(r.get(COLUMN_IR));
                    double vol = Double.parseDouble(r.get(COLUMN_VOL));

                    Position p = new Position(selected, c, pos, posPx, vol, ir, null);
                    positions.put(c.localSymbol(), p);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return positions;
    }


    public static void storeTrade(IbGateway.ExecDetails m) {
        Path path = Paths.get(DATA_DIR_PATH);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Path filePath = Paths.get(TRADES_FILE_PATH);
        if (!Files.exists(filePath)) {
            try (CSVPrinter writer = new CSVPrinter(new FileWriter(TRADES_FILE_PATH), CSV_TRADES_FORMAT)) {
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        try (CSVPrinter writer = new CSVPrinter(new FileWriter(TRADES_FILE_PATH, true), CSV_TRADES_FORMAT_WITHOUT_HEADER)) {
            writer.printRecord(m.getContract().localSymbol(),
                    m.getContract().symbol(),
                    m.getContract().secType(),
                    m.getContract().currency(),
                    m.getContract().exchange(),
                    m.getContract().lastTradeDateOrContractMonth(),
                    m.getContract().strike(),
                    m.getContract().right(),
                    m.getContract().multiplier(),
                    Utils.getPosition(m),
                    m.getExecution().price(),
                    m.getExecution().execId(),
                    m.getExecution().time());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Trade> readTrades() {
        List<Trade> trades = new LinkedList<>();
        Path path = Paths.get(TRADES_FILE_PATH);
        if (Files.exists(path)) {
            try (CSVParser reader = new CSVParser(new FileReader(TRADES_FILE_PATH), CSV_TRADES_FORMAT.withSkipHeaderRecord())) {
                for (CSVRecord r : reader) {
                    Contract c = new Contract();
                    c.localSymbol(r.get(COLUMN_LOCAL_SYMBOL));
                    c.symbol(r.get(COLUMN_SYMBOL));
                    c.secType(r.get(COLUMN_SEC_TYPE));
                    c.currency(r.get(COLUMN_CURRENCY));
                    c.exchange(r.get(COLUMN_EXCHANGE));
                    c.lastTradeDateOrContractMonth(r.get(COLUMN_LAST_TRADE_DATE));
                    c.strike(Double.parseDouble(r.get(COLUMN_STRIKE)));
                    c.right(r.get(COLUMN_RIGHT));
                    c.multiplier(r.get(COLUMN_MULTIPLIER));

                    double pos = Double.parseDouble(r.get(COLUMN_POSITION));
                    double posPx = Double.parseDouble(r.get(COLUMN_PRICE));
                    String execId = r.get(COLUMN_EXEC_ID);
                    String time = r.get(COLUMN_TIME);

                    Trade t = new Trade(c, pos, posPx, execId, time);
                    trades.add(t);

                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return trades;
    }


}
