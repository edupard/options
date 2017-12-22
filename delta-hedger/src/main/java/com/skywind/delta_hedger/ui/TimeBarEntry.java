package com.skywind.delta_hedger.ui;

import com.skywind.delta_hedger.actors.HedgerActor;
import com.skywind.delta_hedger.actors.Position;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class TimeBarEntry {
    private final StringProperty localSymbol;
    private final StringProperty duration;
    private final StringProperty barTime;
    private final DoubleProperty open;
    private final DoubleProperty high;
    private final DoubleProperty low;
    private final DoubleProperty close;
    private final DoubleProperty volume;
    private final StringProperty lut;

    public TimeBarEntry() {
        this.localSymbol = new SimpleStringProperty();
        this.duration = new SimpleStringProperty();
        this.barTime = new SimpleStringProperty();
        this.open = new SimpleDoubleProperty();
        this.high = new SimpleDoubleProperty();
        this.low = new SimpleDoubleProperty();
        this.close = new SimpleDoubleProperty();
        this.volume = new SimpleDoubleProperty();
        this.lut = new SimpleStringProperty();
    }

    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    public void updateUi(HedgerActor.Timebar tb) {
        localSymbol.set(tb.getLocalSymbol());
        duration.set(tb.getDuration());
        barTime.set(tb.getBarTime());
        open.set(tb.getOpen());
        high.set(tb.getHigh());
        low.set(tb.getLow());
        close.set(tb.getClose());
        volume.set(tb.getVolume());
        lut.set(FMT.format(tb.getLut()));

    }

    public StringProperty localSymbolProperty() {
        return localSymbol;
    }

    public StringProperty durationProperty() {
        return duration;
    }

    public StringProperty barTimeProperty() {
        return barTime;
    }

    public DoubleProperty openProperty() {
        return open;
    }

    public DoubleProperty highProperty() {
        return high;
    }

    public DoubleProperty lowProperty() {
        return low;
    }

    public DoubleProperty closeProperty() {
        return close;
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public StringProperty lutProperty() { return lut; }
}
