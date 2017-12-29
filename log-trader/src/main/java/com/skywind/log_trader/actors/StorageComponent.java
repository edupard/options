package com.skywind.log_trader.actors;

import com.skywind.ib.IbGateway;
import com.skywind.ib.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.LinkedList;
import java.util.List;

@Component
public class StorageComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageComponent.class);

    @Value("${symbol}")
    private String symbol;

    private static final String COLUMN_LOCAL_SYMBOL = "LocalSymbol";
    private static final String COLUMN_POSITION = "Position";


    private static final String COLUMN_TRADES_DATE = "Date";
    private static final String COLUMN_TRADES_TIME = "Time";
    private static final String COLUMN_TRADES_VOLUME = "Amount";
    private static final String COLUMN_TRADES_SIDE = "Side";
    private static final String COLUMN_TRADES_LOCAL_SYMBOL = "Contract";
    private static final String COLUMN_TRADES_POSITION = "Position";
    private static final String COLUMN_TRADES_A = "A";
    private static final String COLUMN_TRADES_B = "B";
    private static final String COLUMN_TRADES_PRICE = "Price";
    private static final String COLUMN_TRADES_C = "C";
    private static final String COLUMN_TRADES_EXEC_ID = "ExecId";
    private static final String COLUMN_TRADES_TIMESTAMP = "Timestamp";

    private static final String POSITIONS_FILE_PATH = "data/positions.csv";
    private static final String TRADES_FILE_PATH = "data/trades.csv";

    private final static CSVFormat CSV_POSITIONS_FORMAT = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator())
            .withHeader(
                    COLUMN_LOCAL_SYMBOL,
                    COLUMN_POSITION
            );

    private final static CSVFormat CSV_TRADES_FORMAT_WITHOUT_HEADER = CSVFormat.DEFAULT
            .withRecordSeparator(System.lineSeparator());

    private final static CSVFormat CSV_TRADES_FORMAT = CSV_TRADES_FORMAT_WITHOUT_HEADER.withHeader(
            COLUMN_TRADES_DATE,
            COLUMN_TRADES_TIME,
            COLUMN_TRADES_VOLUME,
            COLUMN_TRADES_SIDE,
            COLUMN_TRADES_LOCAL_SYMBOL,
            COLUMN_TRADES_POSITION,
            COLUMN_TRADES_A,
            COLUMN_TRADES_B,
            COLUMN_TRADES_PRICE,
            COLUMN_TRADES_C,
            COLUMN_TRADES_EXEC_ID,
            COLUMN_TRADES_TIMESTAMP
    );

    public void storePositions(double position) {
        try (CSVPrinter writer = new CSVPrinter(new FileWriter(POSITIONS_FILE_PATH, false), CSV_POSITIONS_FORMAT)) {
            writer.printRecord(symbol, position);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    public double readPosition() {
        Path path = Paths.get(POSITIONS_FILE_PATH);
        if (Files.exists(path)) {
            try (CSVParser reader = new CSVParser(new FileReader(POSITIONS_FILE_PATH), CSV_POSITIONS_FORMAT.withSkipHeaderRecord())) {
                for (CSVRecord r : reader) {
                    if (r.get(COLUMN_LOCAL_SYMBOL).equals(symbol)) {
                        return Double.parseDouble(r.get(COLUMN_POSITION));
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return 0.0d;
    }

    private static final DateTimeFormatter IB_TRADE_TIMESTAMP_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd  HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter TIME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm")
            .toFormatter()
            .withZone(ZoneId.systemDefault());


    public void storeTrade(IbGateway.ExecDetails m) {
        Path filePath = Paths.get(TRADES_FILE_PATH);
        if (!Files.exists(filePath)) {
            try (CSVPrinter writer = new CSVPrinter(new FileWriter(TRADES_FILE_PATH), CSV_TRADES_FORMAT)) {
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        try (CSVPrinter writer = new CSVPrinter(new FileWriter(TRADES_FILE_PATH, true), CSV_TRADES_FORMAT_WITHOUT_HEADER)) {
            String sDate = m.getExecution().time();
            String sTime = m.getExecution().time();
            try {
                Instant instant = IB_TRADE_TIMESTAMP_FMT.parse(m.getExecution().time(), Instant::from);
                sDate = DATE_FMT.format(instant);
                sTime = TIME_FMT.format(instant);
            } catch (Throwable t) {
                LOGGER.error("", t);
            }

            writer.printRecord(
                    sDate,
                    sTime,
                    m.getExecution().shares(),
                    m.getExecution().side().equals("BOT") ? "Buy" : "Sell",
                    m.getContract().localSymbol(),
                    Utils.getPosition(m),
                    "",
                    "",
                    m.getExecution().price(),
                    "",
                    m.getExecution().execId(),
                    m.getExecution().time()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Trade> readTrades() {
        List<Trade> trades = new LinkedList<>();
        Path path = Paths.get(TRADES_FILE_PATH);
        if (Files.exists(path)) {
            try (CSVParser reader = new CSVParser(new FileReader(TRADES_FILE_PATH), CSV_TRADES_FORMAT.withSkipHeaderRecord())) {
                for (CSVRecord r : reader) {
                    String localSymbol = r.get(COLUMN_TRADES_LOCAL_SYMBOL);
                    double pos = Double.parseDouble(r.get(COLUMN_TRADES_POSITION));
                    double posPx = Double.parseDouble(r.get(COLUMN_TRADES_PRICE));
                    String execId = r.get(COLUMN_TRADES_EXEC_ID);
                    String time = r.get(COLUMN_TRADES_TIMESTAMP);

                    Trade t = new Trade(localSymbol, pos, posPx, execId, time);
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
