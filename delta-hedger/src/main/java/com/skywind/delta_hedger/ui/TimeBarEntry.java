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

    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static String convertPrice(double px, double futPriceCoeff) {
        double pxWhole = Math.floor(px);
        double pxPart = px - pxWhole;
        double pxPartConverted = Math.round(pxPart * futPriceCoeff);
        return String.format("%.0f'%.0f", pxWhole, pxPartConverted);
    }

    public void updateUi(HedgerActor.Timebar tb, double futPriceCoeff) {
        localSymbol.set(tb.getLocalSymbol());
        duration.set(tb.getDuration());
        barTime.set(tb.getBarTime());

        double tbOpen = tb.getOpen();
        double openWhole = Math.floor(tbOpen);
        double openPart = tbOpen - openWhole;
        double openPartConverted = Math.round(openPart * futPriceCoeff);
        double openConverted = openWhole + openPartConverted / 1000;

        open.set(convertPrice(tb.getOpen(), futPriceCoeff));
        high.set(convertPrice(tb.getHigh(), futPriceCoeff));
        low.set(convertPrice(tb.getLow(), futPriceCoeff));
        close.set(convertPrice(tb.getClose(), futPriceCoeff));

//        open.set(tb.getOpen());
//        high.set(tb.getHigh());
//        low.set(tb.getLow());
//        close.set(tb.getClose());

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

    public StringProperty lutProperty() { return lut; }
}
