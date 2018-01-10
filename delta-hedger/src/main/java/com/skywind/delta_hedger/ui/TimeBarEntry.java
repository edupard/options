package com.skywind.delta_hedger.ui;

import com.skywind.delta_hedger.actors.HedgerActor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class TimeBarEntry {
    private final StringProperty localSymbol;
    private final StringProperty duration;
    private final StringProperty barTime;
    private final StringProperty open;
    private final StringProperty high;
    private final StringProperty low;
    private final StringProperty close;
    private final DoubleProperty volume;
    private final StringProperty lut;

    public TimeBarEntry() {
        this.localSymbol = new SimpleStringProperty();
        this.duration = new SimpleStringProperty();
        this.barTime = new SimpleStringProperty();
        this.open = new SimpleStringProperty();
        this.high = new SimpleStringProperty();
        this.low = new SimpleStringProperty();
        this.close = new SimpleStringProperty();
        this.volume = new SimpleDoubleProperty();
        this.lut = new SimpleStringProperty();
    }

    private static final DateTimeFormatter LUT_FMT = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter BAR_TIME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    public void updateUi(HedgerActor.Timebar tb) {
        localSymbol.set(tb.getLocalSymbol());
        duration.set(tb.getDuration());
        barTime.set(BAR_TIME_FMT.format(tb.getBarTime()));

        open.set(tb.getOpenView());
        high.set(tb.getHighView());
        low.set(tb.getLowView());
        close.set(tb.getCloseView());

        volume.set(tb.getVolume());
        lut.set(LUT_FMT.format(tb.getLut()));
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

    public StringProperty openProperty() {
        return open;
    }

    public StringProperty highProperty() {
        return high;
    }

    public StringProperty lowProperty() {
        return low;
    }

    public StringProperty closeProperty() {
        return close;
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public StringProperty lutProperty() {
        return lut;
    }
}
