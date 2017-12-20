package com.skywind.spring_javafx_integration.ui;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class FormatedCellFactory<S> implements Callback<TableColumn<S, Double>, TableCell<S, Double>> {

    private final String format;

    public FormatedCellFactory(String format) {
        this.format = format;
    }

    @Override
    public TableCell<S, Double> call(TableColumn<S, Double> param) {
        MagicTableCell cell = new MagicTableCell<>();
        cell.setFormat(format);
        return cell;
    }
}

