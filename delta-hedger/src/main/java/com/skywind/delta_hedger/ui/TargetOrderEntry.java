package com.skywind.delta_hedger.ui;

import com.skywind.delta_hedger.actors.TargetOrder;
import javafx.beans.property.*;

public class TargetOrderEntry {
    private final IntegerProperty idx;
    private final StringProperty code;
    private final StringProperty side;
    private final StringProperty viewPx;
    private final DoubleProperty qty;
    private final StringProperty orderType;

    public TargetOrderEntry() {
        this.idx = new SimpleIntegerProperty();
        this.code = new SimpleStringProperty();
        this.side = new SimpleStringProperty();
        this.viewPx = new SimpleStringProperty();
        this.qty = new SimpleDoubleProperty();
        this.orderType = new SimpleStringProperty();
    }

    public void updateUi(TargetOrder to) {
        idx.setValue(to.getIdx());
        code.set(to.getCode());
        side.set(to.getQty() > 0 ? "BUY" : "SELL");
        viewPx.set(to.getViewPx());
        qty.set(to.getQty());
        orderType.set(to.getOrderType());
    }


    public IntegerProperty idxProperty() {
        return idx;
    }

    public StringProperty codeProperty() {
        return code;
    }


    public StringProperty sideProperty() {
        return side;
    }


    public StringProperty viewPxProperty() {
        return viewPx;
    }


    public DoubleProperty qtyProperty() {
        return qty;
    }


    public StringProperty orderTypeProperty() {
        return orderType;
    }
}
