package com.skywind.log_trader.actors;

import com.ib.client.Contract;
import com.skywind.ib.IbGateway;
import com.skywind.ib.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class StorageComponent {

    @Value("${symbol}")
    private String symbol;

    private static final String COLUMN_LOCAL_SYMBOL = "LocalSymbol";
    private static final String COLUMN_POSITION = "Position";
    private static final String COLUMN_PRICE = "Price";
    private static final String COLUMN_EXEC_ID = "ExecId";
    private static final String COLUMN_TIME = "Time";

    private static final String DATA_DIR_PATH = "data";
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
            COLUMN_LOCAL_SYMBOL,
            COLUMN_POSITION,
            COLUMN_PRICE,
            COLUMN_EXEC_ID,
            COLUMN_TIME
    );

    public void storePositions(double position) {

        Path path = Paths.get(DATA_DIR_PATH);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

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


    public void storeTrade(IbGateway.ExecDetails m) {
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
                    Utils.getPosition(m),
                    m.getExecution().price(),
                    m.getExecution().execId(),
                    m.getExecution().time());
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
                    String localSymbol = r.get(COLUMN_LOCAL_SYMBOL);
                    double pos = Double.parseDouble(r.get(COLUMN_POSITION));
                    double posPx = Double.parseDouble(r.get(COLUMN_PRICE));
                    String execId = r.get(COLUMN_EXEC_ID);
                    String time = r.get(COLUMN_TIME);

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
