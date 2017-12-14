package com.skywind.spring_javafx_integration;

import java.io.IOException;
import java.io.InputStream;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class ControllerAndParent {

    private Parent parent;
    private Object controller;

    public ControllerAndParent(Parent view,
            Object controller) {
        this.parent = view;
        this.controller = controller;
    }

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public static ControllerAndParent loadView(InputStream fxmlStream) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.load(fxmlStream);
        return new ControllerAndParent(loader.getRoot(), loader.getController());
    }
}
