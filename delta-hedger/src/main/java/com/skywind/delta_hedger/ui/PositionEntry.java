package com.skywind.delta_hedger.ui;

import com.ib.client.ContractDetails;
import com.skywind.delta_hedger.actors.Position;
import com.skywind.delta_hedger.actors.Utils;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class PositionEntry {
    private final BooleanProperty selected;
    private final StringProperty localSymbol;
    private final StringProperty underLocalSymbol;
    private final StringProperty expiry;
    private final DoubleProperty days;
    private final DoubleProperty strike;
    private final StringProperty secType;
    private final DoubleProperty pos;
    private final DoubleProperty ir;
    private final DoubleProperty vol;

    public PositionEntry() {
        this.selected = new SimpleBooleanProperty(true);
        this.localSymbol = new SimpleStringProperty();
        this.underLocalSymbol = new SimpleStringProperty();
        this.expiry = new SimpleStringProperty();
        this.days = new SimpleDoubleProperty();
        this.strike = new SimpleDoubleProperty();
        this.secType = new SimpleStringProperty();
        this.pos = new SimpleDoubleProperty();
        this.ir = new SimpleDoubleProperty();
        this.vol = new SimpleDoubleProperty();
    }

    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static final double SECONDS_PER_DAY = 86400;

    public void updateUi(Position p) {
        localSymbol.set(p.getContract().localSymbol());
        if (p.isContractDetailsDefined()) {
            ContractDetails cd = p.getContractDetails();
            underLocalSymbol.set(cd.underSymbol());
            if (p.getExpiry() != null) {
                expiry.set(FMT.format(p.getExpiry()));
                Instant now = Instant.now();
                Duration d = Duration.between(now, p.getExpiry());
                days.set((double) d.getSeconds() / SECONDS_PER_DAY);
            }
        }
        else {
            underLocalSymbol.set("-");
            expiry.set("-");
            days.set(0);
        }
        // TODO: update days periodically

        strike.set(p.getContract().strike());
        // how to propogate to script
        secType.set(Utils.getSecType(p.getContract()));
        pos.set(p.getPos());
        ir.set(p.getIr());
        vol.set(p.getVol());
    }

    public ObservableValue<Boolean> selectedProperty() {
        return selected;
    }

    public ObservableValue<String> localSymbolProperty() {
        return localSymbol;
    }

    public ObservableValue<String> expiryProperty() {
        return expiry;
    }

    public DoubleProperty daysProperty() {
        return days;
    }

    public DoubleProperty strikeProperty() {
        return strike;
    }

    public ObservableValue<String> secTypeProperty() {
        return secType;
    }

    public DoubleProperty posProperty() {
        return pos;
    }

    public DoubleProperty irProperty() {
        return ir;
    }

    public DoubleProperty volProperty() {
        return vol;
    }

    public ObservableValue<String> underLocalSymbolProperty() {
        return underLocalSymbol;
    }
}