package com.skywind.delta_hedger.ui;

import com.skywind.delta_hedger.actors.TargetOrder;
import javafx.beans.property.*;

public class TargetOrderEntry {
    private final StringProperty code;
    private final StringProperty side;
    private final StringProperty viewPx;
    private final DoubleProperty qty;
    private final StringProperty orderType;

    public TargetOrderEntry() {
        this.code = new SimpleStringProperty();
        this.side = new SimpleStringProperty();
        this.viewPx = new SimpleStringProperty();
        this.qty = new SimpleDoubleProperty();
        this.orderType = new SimpleStringProperty();
    }

    public void updateUi(TargetOrder to) {
        code.set(to.getCode());
        side.set(to.getQty() > 0 ? "BUY" : "SELL");
        viewPx.set(to.getViewPx());
        qty.set(to.getQty());
        orderType.set(to.getOrderType());
    }

    public String getCode() {
        return code.get();
    }

    public StringProperty codeProperty() {
        return code;
    }

    public String getSide() {
        return side.get();
    }

    public StringProperty sideProperty() {
        return side;
    }

    public String getViewPx() {
        return viewPx.get();
    }

    public StringProperty viewPxProperty() {
        return viewPx;
    }

    public double getQty() {
        return qty.get();
    }

    public DoubleProperty qtyProperty() {
        return qty;
    }

    public String getOrderType() {
        return orderType.get();
    }

    public StringProperty orderTypeProperty() {
        return orderType;
    }
}
