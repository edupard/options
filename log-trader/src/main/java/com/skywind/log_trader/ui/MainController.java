package com.skywind.log_trader.ui;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.skywind.log_trader.actors.LogTraderActor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;

public class MainController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ActorSystem actorSystem;

    private ActorSelection logTraderActor;

    @Value("${account}")
    private String account;

    @Value("${symbol}")
    private String symbol;

    @FXML
    private AnchorPane anchor;

    @FXML
    private Label lblApiConnection;
    @FXML
    private Label lblIbConnection;


    @FXML
    private Label lblAccount;

    @FXML
    private Label lblSymbol;
    @FXML
    private Label lblLastFileLine;
    @FXML
    private Label lblLastFileSignalLine;
    @FXML
    private Label lblPosition;
    @FXML
    private Label lblTgtPosition;

    @FXML
    private Button btnStartStop;



    public Parent getParent() {
        return anchor;
    }

    public void onClose() {
        ((ConfigurableApplicationContext) applicationContext).close();
    }

    public void onApiConnection(boolean apiConnection) {
        Platform.runLater(() -> {
            lblApiConnection.setTextFill(apiConnection ? Color.GREEN : Color.RED);
        });
    }

    public void onIbConnection(boolean ibConnection) {
        Platform.runLater(() -> {
            lblIbConnection.setTextFill(ibConnection ? Color.GREEN : Color.RED);
        });
    }

    @PostConstruct
    public void postConstruct() {
        lblPosition.setTextFill(Color.RED);
        lblTgtPosition.setTextFill(Color.RED);
        lblAccount.setText(account);
        lblSymbol.setText(symbol);
        btnStartStop.setStyle("-fx-base: red;");
    }

    public void onPosition(double position) {
        Platform.runLater(() -> {
            String sPosition = String.format("position: %.0f", position);
            lblPosition.setText(sPosition);
            lblPosition.setTextFill(Color.GREEN);
        });
    }

    public void onTgtPosition(double position) {
        Platform.runLater(() -> {
            String sPosition = String.format("tgt: %.0f", position);
            lblTgtPosition.setText(sPosition);
            lblTgtPosition.setTextFill(Color.GREEN);
        });
    }

    public void onUnknownTgtPosition() {
        Platform.runLater(() -> {
            lblTgtPosition.setText("tgt: unknown");
            lblTgtPosition.setTextFill(Color.RED);
        });
    }

    public void onUnknownPosition() {
        Platform.runLater(() -> {
            lblPosition.setText("position: unknown");
            lblPosition.setTextFill(Color.RED);
        });
    }


    @PostConstruct
    public void init() {
        logTraderActor = actorSystem.actorSelection("/user/app/logTrader");
    }

    public void onPositionsReloadComplete() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Positions reload complete!");
            alert.setContentText("Please check positions manually");
            alert.show();
        });
    }

    @FXML
    public void onReloadPosition() {
        logTraderActor.tell(new LogTraderActor.ReloadPosition(), null);
    }

    private volatile boolean started = false;

    @FXML
    public void onStartStop() {
        if (!started) {
            btnStartStop.setText("Stop");
            btnStartStop.setStyle("-fx-base: green;");
        } else {
            btnStartStop.setText("Start");
            btnStartStop.setStyle("-fx-base: red;");
        }
        started = !started;
        if (started) {
            logTraderActor.tell(new LogTraderActor.StartByUserAction(), null);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void onLastSignalFileLine(String line) {
        Platform.runLater(() -> {
            lblLastFileSignalLine.setText(line);
        });
    }

    public void onFileLine(String line) {
        Platform.runLater(() -> {
            lblLastFileLine.setText(line);
        });
    }

    @FXML
    public void onSimulateError() {
        logTraderActor.tell(new LogTraderActor.SimulateError(), null);
    }

}
