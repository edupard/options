package com.skywind.delta_hedger.ui;

import com.skywind.delta_hedger.actors.HedgerOrder;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

public class OpenOrderEntry {
    private final IntegerProperty orderId;
    private final StringProperty code;
    private final StringProperty side;
    private final StringProperty px;
    private final DoubleProperty qty;

    public OpenOrderEntry() {
        this.orderId = new SimpleIntegerProperty();
        this.code = new SimpleStringProperty();
        this.side = new SimpleStringProperty();
        this.px = new SimpleStringProperty();
        this.qty = new SimpleDoubleProperty();
    }

    public void updateUi(HedgerOrder ho) {
        orderId.set(ho.getOrderId());
        code.set(ho.getCode());
        side.set(ho.getSide().toString());
        px.set(ho.getViewPx());
        qty.set(ho.getQty());
    }

    public IntegerProperty orderIdProperty() {
        return orderId;
    }

    public StringProperty codeProperty() {
        return code;
    }

    public StringProperty sideProperty() {
        return side;
    }

    public StringProperty pxProperty() {
        return px;
    }

    public DoubleProperty qtyProperty() {
        return qty;
    }
}
