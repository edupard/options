package com.skywind.delta_hedger.ui;

import akka.actor.ActorSystem;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class MainController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ActorSystem actorSystem;

    @FXML
    private AnchorPane anchor;


    public Parent getParent() {
        return anchor;
    }

    public void onClose() {
        ((ConfigurableApplicationContext) applicationContext).close();
    }



}
