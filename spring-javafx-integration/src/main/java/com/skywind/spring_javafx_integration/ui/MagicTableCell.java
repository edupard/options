package com.skywind.spring_javafx_integration.ui;

import java.util.function.Function;
import javafx.scene.control.TableCell;

public class MagicTableCell<S, T> extends TableCell<S, T> {

    private static final String CSS_COLOR_TEMPLATE = "-fx-background-color: %s";

    private Function<S, String> colorSupplier = null;
    private String format = null;

    public void setColorSupplier(Function<S, String> colorSupplier) {
        this.colorSupplier = colorSupplier;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    protected void updateItem(T value,
                              boolean empty) {
        super.updateItem(value, empty);

        if (value == null || empty) {
            setText(null);
        } else {
            setText(format == null ? value.toString() : String.format(format, value));
        }

        if (colorSupplier != null) {
            S item = (S) getTableRow().getItem();
            String cssColor = item == null ? "" : colorSupplier.apply(item);
            setStyle(String.format(CSS_COLOR_TEMPLATE, cssColor));
        }
    }
}

