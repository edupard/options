package com.skywind.delta_hedger.ui;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.skywind.delta_hedger.actors.*;
import com.skywind.spring_javafx_integration.ui.FormatedCellFactory;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;
import java.util.*;

public class MainController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ActorSystem actorSystem;

    private ActorSelection hedgerActor;

    @FXML
    private AnchorPane anchor;

    @FXML
    private Label lblApiConnection;
    @FXML
    private Label lblIbConnection;

    @FXML
    private Label lblProgress;

    @FXML
    private TableView<PositionEntry> tblPositions;
    @FXML
    private TableColumn<PositionEntry, Boolean> colSelected;
    @FXML
    private TableColumn<PositionEntry, String> colCode;
    @FXML
    private TableColumn<PositionEntry, String> colUnderCode;
    @FXML
    private TableColumn<PositionEntry, String> colExpiry;
    @FXML
    private TableColumn<PositionEntry, Double> colDays;
    @FXML
    private TableColumn<PositionEntry, Double> colStrike;
    @FXML
    private TableColumn<PositionEntry, String> colSecType;
    @FXML
    private TableColumn<PositionEntry, Double> colPos;
    @FXML
    private TableColumn<PositionEntry, Double> colPosPx;
    @FXML
    private TableColumn<PositionEntry, Double> colIr;
    @FXML
    private TableColumn<PositionEntry, Double> colVol;
    @FXML
    private TableColumn<PositionEntry, String> colLastPos;
    @FXML
    private TableColumn<PositionEntry, String> colLastTime;

    private Map<String, PositionEntry> positionsDataMap = new HashMap<>();
    private final ObservableList<PositionEntry> positionsData = FXCollections.observableArrayList();
    private final FilteredList<PositionEntry> filteredPositionsData = new FilteredList<>(positionsData, p -> true);
    private final SortedList<PositionEntry> sortedPositionsData = new SortedList<>(filteredPositionsData);


    @FXML
    private TableView<OpenOrderEntry> tblOpenOrders;
    @FXML
    private TableColumn<OpenOrderEntry, Integer> colOoOrderId;
    @FXML
    private TableColumn<OpenOrderEntry, String> colOoCode;
    @FXML
    private TableColumn<OpenOrderEntry, String> colOoSide;
    @FXML
    private TableColumn<OpenOrderEntry, String> colOoPx;
    @FXML
    private TableColumn<OpenOrderEntry, Double> colOoQty;

    private final ObservableList<OpenOrderEntry> ooData = FXCollections.observableArrayList();
    private final FilteredList<OpenOrderEntry> filteredOoData = new FilteredList<>(ooData, p -> true);
    private final SortedList<OpenOrderEntry> sortedOoData = new SortedList<>(filteredOoData);


    @FXML
    private TableView<TargetOrderEntry> tblTargetOrders;

    @FXML
    private TableColumn<TargetOrderEntry, String> colToCode;
    @FXML
    private TableColumn<TargetOrderEntry, String> colToSide;
    @FXML
    private TableColumn<TargetOrderEntry, String> colToPx;
    @FXML
    private TableColumn<TargetOrderEntry, Double> colToQty;
    @FXML
    private TableColumn<TargetOrderEntry, String> colToType;

    private final ObservableList<TargetOrderEntry> toData = FXCollections.observableArrayList();
    private final FilteredList<TargetOrderEntry> filteredToData = new FilteredList<>(toData, p -> true);
    private final SortedList<TargetOrderEntry> sortedToData = new SortedList<>(filteredToData);

    @FXML
    private TableView<TimeBarEntry> tblTimeBars;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbCode;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbDuration;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbBarTime;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbOpen;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbHigh;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbLow;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbClose;
    @FXML
    private TableColumn<TimeBarEntry, Double> colTbVolume;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbLut;

    private Map<HedgerActor.Timebar, TimeBarEntry> timeBarsDataMap = new HashMap<>();
    private final ObservableList<TimeBarEntry> timeBarData = FXCollections.observableArrayList();
    private final FilteredList<TimeBarEntry> filteredTimeBarData = new FilteredList<>(timeBarData, p -> true);
    private final SortedList<TimeBarEntry> sortedTimeBarData = new SortedList<>(filteredTimeBarData);

    public Parent getParent() {
        return anchor;
    }

    public void onClose() {
        ((ConfigurableApplicationContext) applicationContext).close();
    }

    public void onTimeBar(HedgerActor.Timebar tb) {
        if (timeBarsDataMap.containsKey(tb)) {
            TimeBarEntry timeBarEntry = timeBarsDataMap.get(tb);
            timeBarEntry.updateUi(tb);
            timeBarsDataMap.put(tb, timeBarEntry);
        } else {
            TimeBarEntry timeBarEntry = new TimeBarEntry();
            timeBarEntry.updateUi(tb);
            timeBarData.add(timeBarEntry);
            timeBarsDataMap.put(tb, timeBarEntry);
        }
    }

    public void onClearTimeBars() {
        Platform.runLater(() -> {
            timeBarsDataMap.clear();
            timeBarData.clear();
        });
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

    public void onOpenOrders(List<HedgerOrder> oo) {
        Platform.runLater(() -> {
            ooData.clear();
            oo.stream().forEach(ho -> {
                OpenOrderEntry ooEntry = new OpenOrderEntry();
                ooEntry.updateUi(ho);
                ooData.add(ooEntry);
            });
        });
    }

    public void onTargetOrders(List<TargetOrder> to) {
        Platform.runLater(() -> {
            toData.clear();
            to.stream().forEach(ho -> {
                TargetOrderEntry toEntry = new TargetOrderEntry();
                toEntry.updateUi(ho);
                toData.add(toEntry);
            });
        });
    }

    public static final class UpdateUiPositionsBatch {

        private final boolean fullUpdate;

        public UpdateUiPositionsBatch(boolean fullUpdate) {
            this.fullUpdate = fullUpdate;
        }

        public static enum Action {UPDATE, DELETE};

        public static final class BatchElement {
            private final Action action;
            private final String localSymbol;
            private final Position pi;

            public BatchElement(Action action, String localSymbol, Position pi) {
                this.action = action;
                this.localSymbol = localSymbol;
                this.pi = pi;
            }
        }

        private final List<BatchElement> updates = new LinkedList<>();

        public void addAction(Action action, String localSymbol, Position pi) {
            updates.add(new BatchElement(action, localSymbol, new Position(pi)));
        }

        public boolean isFullUpdate() {
            return fullUpdate;
        }

        public List<BatchElement> getUpdates() {
            return updates;
        }
    }

    public class CustomDoubleStringConverter extends StringConverter<Double> {

        @Override
        public Double fromString(String value) {
            // If the specified value is null or zero-length, return null
            if (value == null) {
                return null;
            }

            value = value.replace(',', '.');
            value = value.replace('%', ' ');
            value = value.trim();

            if (value.length() < 1) {
                return null;
            }

            return Double.valueOf(value);
        }

        @Override
        public String toString(Double value) {
            if (value == null) {
                return "";
            }
            return String.format("%.2f %%", value);
        }
    }


    @PostConstruct
    public void init() {
        lblProgress.setText("");

        hedgerActor = actorSystem.actorSelection("/user/app/hedger");

        colSelected.setCellFactory(
                CheckBoxTableCell.forTableColumn(colSelected)
        );
        colSelected.setCellValueFactory(cellData -> {
            PositionEntry pe = cellData.getValue();
            return pe.selectedProperty();
        });

        colCode.setCellValueFactory(cellData -> cellData.getValue().localSymbolProperty());
        colUnderCode.setCellValueFactory(cellData -> cellData.getValue().underLocalSymbolProperty());
        colExpiry.setCellValueFactory(cellData -> cellData.getValue().expiryProperty());

        colDays.setCellValueFactory(cellData -> cellData.getValue().daysProperty().asObject());
        colDays.setCellFactory(new FormatedCellFactory<>("%.2f"));

        colStrike.setCellValueFactory(cellData -> cellData.getValue().strikeProperty().asObject());
        colStrike.setCellFactory(new FormatedCellFactory<>("%.2f"));

        colSecType.setCellValueFactory(cellData -> cellData.getValue().secTypeProperty());

        colPos.setCellValueFactory(cellData -> cellData.getValue().posProperty().asObject());
        colPos.setCellFactory(new FormatedCellFactory<>("%.0f"));

        colPosPx.setCellValueFactory(cellData -> cellData.getValue().posPxProperty().asObject());
        colPos.setCellFactory(new FormatedCellFactory<>("%.5f"));

        colIr.setCellValueFactory(cellData -> {
            PositionEntry pe = cellData.getValue();
            pe.irProperty().addListener(new ChangeListener() {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                    hedgerActor.tell(new HedgerActor.IrChanged(pe.localSymbolProperty().getValue(), (double) newValue), null);
                }
            });

            return pe.irProperty().asObject();
        });
        colIr.setCellFactory(TextFieldTableCell.forTableColumn(new CustomDoubleStringConverter()));

        colVol.setCellValueFactory(cellData -> {
            PositionEntry pe = cellData.getValue();
            pe.volProperty().addListener(new ChangeListener() {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                    hedgerActor.tell(new HedgerActor.VolChanged(pe.localSymbolProperty().getValue(), (double) newValue), null);
                }
            });

            return pe.volProperty().asObject();
        });
        colVol.setCellFactory(TextFieldTableCell.forTableColumn(new CustomDoubleStringConverter()));

        colLastPos.setCellValueFactory(cellData -> cellData.getValue().lastPosProperty());
        colLastTime.setCellValueFactory(cellData -> cellData.getValue().lastTimeProperty());


        sortedPositionsData.comparatorProperty().bind(tblPositions.comparatorProperty());
        tblPositions.setItems(sortedPositionsData);

        colOoOrderId.setCellValueFactory(cellData -> cellData.getValue().orderIdProperty().asObject());
        colOoCode.setCellValueFactory(cellData -> cellData.getValue().codeProperty());
        colOoSide.setCellValueFactory(cellData -> cellData.getValue().sideProperty());
        colOoPx.setCellValueFactory(cellData -> cellData.getValue().pxProperty());
        colOoQty.setCellValueFactory(cellData -> cellData.getValue().qtyProperty().asObject());
        colOoQty.setCellFactory(new FormatedCellFactory<>("%.0f"));

        sortedOoData.comparatorProperty().bind(tblOpenOrders.comparatorProperty());
        tblOpenOrders.setItems(sortedOoData);



        colToCode.setCellValueFactory(cellData -> cellData.getValue().codeProperty());
        colToSide.setCellValueFactory(cellData -> cellData.getValue().sideProperty());
        colToPx.setCellValueFactory(cellData -> cellData.getValue().viewPxProperty());
        colToQty.setCellValueFactory(cellData -> cellData.getValue().qtyProperty().asObject());
        colToQty.setCellFactory(new FormatedCellFactory<>("%.0f"));
        colToType.setCellValueFactory(cellData -> cellData.getValue().orderTypeProperty());

        sortedToData.comparatorProperty().bind(tblTargetOrders.comparatorProperty());
        tblTargetOrders.setItems(sortedToData);







        colTbCode.setCellValueFactory(cellData -> cellData.getValue().localSymbolProperty());
        colTbDuration.setCellValueFactory(cellData -> cellData.getValue().durationProperty());
        colTbBarTime.setCellValueFactory(cellData -> cellData.getValue().barTimeProperty());

        colTbOpen.setCellValueFactory(cellData -> cellData.getValue().openProperty());
        colTbHigh.setCellValueFactory(cellData -> cellData.getValue().highProperty());
        colTbLow.setCellValueFactory(cellData -> cellData.getValue().lowProperty());
        colTbClose.setCellValueFactory(cellData -> cellData.getValue().closeProperty());

        colTbVolume.setCellValueFactory(cellData -> cellData.getValue().volumeProperty().asObject());
        colTbVolume.setCellFactory(new FormatedCellFactory<>("%.0f"));

        colTbLut.setCellValueFactory(cellData -> cellData.getValue().lutProperty());


        sortedTimeBarData.comparatorProperty().bind(tblTimeBars.comparatorProperty());
        tblTimeBars.setItems(sortedTimeBarData);


    }

    public void onPositionsUpdate(UpdateUiPositionsBatch uiUpdates) {
        Platform.runLater(() -> {
            if (uiUpdates.isFullUpdate()) {
                positionsData.clear();
                positionsDataMap.clear();
            }
            for (UpdateUiPositionsBatch.BatchElement e : uiUpdates.getUpdates()) {
                if (e.action == UpdateUiPositionsBatch.Action.UPDATE) {
                    if (positionsDataMap.containsKey(e.localSymbol)) {
                        PositionEntry pe = positionsDataMap.get(e.localSymbol);
                        pe.updateUi(e.pi);
                    } else {
                        PositionEntry pe = new PositionEntry();
                        pe.updateUi(e.pi);
                        positionsDataMap.put(e.localSymbol, pe);
                        positionsData.add(pe);
                    }
                } else {
                    if (positionsDataMap.containsKey(e.localSymbol)) {
                        PositionEntry pe = positionsDataMap.get(e.localSymbol);
                        positionsData.remove(pe);
                        positionsDataMap.remove(e.localSymbol);
                    }
                }
            }
        });
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
    public void onReloadPositions() {
        hedgerActor.tell(new HedgerActor.ReloadPositions(), null);
    }

    @FXML
    public void onRefreshTimebars() {
        hedgerActor.tell(new HedgerActor.RefreshTimebars(), null);
    }

    @FXML
    private CheckBox cbIncludeManualOrders;

    private volatile boolean includeManualOrders = false;

    public boolean isIncludeManualOrders() {
        return includeManualOrders;
    }

    @FXML
    public void onIncludeManualOrdersChanged() {
        includeManualOrders = cbIncludeManualOrders.isSelected();
    }

    @FXML
    public void onRefreshOpenOrders() {
        hedgerActor.tell(new HedgerActor.RefreshOpenOrders(), null);
    }


    @FXML
    private CheckBox cbCancelOrders;

    private volatile boolean cancelOrders = false;

    @FXML
    public void onCancelOrdersChanged() {
        cancelOrders = cbCancelOrders.isSelected();
    }

    public boolean isCancelOrders() {
        return cancelOrders;
    }

    @FXML
    private CheckBox cbRunPython;

    private volatile boolean runPython = false;

    @FXML
    public void onRunPythonChanged() {
        runPython = cbRunPython.isSelected();
    }

    public boolean isRunPython() {
        return runPython;
    }

    @FXML
    private CheckBox cbPlaceOrders;

    private volatile boolean placeOrders = false;

    @FXML
    public void onPlaceOrdersChanged() {
        placeOrders = cbPlaceOrders.isSelected();
    }

    public boolean isPlaceOrders() {
        return placeOrders;
    }

    @FXML
    private TextField tfParam;

    @FXML
    public void onRunPython() {
        lblProgress.setText("Running...");
        lblProgress.setTextFill(Color.BLUE);
        hedgerActor.tell(new HedgerActor.RunAmendmentProcess(tfParam.getText().equals("")? "default" : tfParam.getText()), null);
    }

    @FXML
    public void onShowPortfolio() {
        hedgerActor.tell(new HedgerActor.ShowPortfolio(), null);
    }

    public void onProgress(AmendmentProcess.Stage stage) {
        Platform.runLater(() -> {
            lblProgress.setText(stage.toString());
            switch (stage) {
                case COMPLETED:
                    lblProgress.setTextFill(Color.GREEN);
                    break;
                case FAILED:
                    lblProgress.setTextFill(Color.RED);
                    break;
                case CANCELLED:
                    lblProgress.setTextFill(Color.ORANGE);
                    break;
            }
        });
    }
}
