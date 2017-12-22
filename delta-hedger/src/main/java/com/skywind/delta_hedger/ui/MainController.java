package com.skywind.delta_hedger.ui;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.skywind.delta_hedger.actors.HedgerActor;
import com.skywind.delta_hedger.actors.Position;
import com.skywind.spring_javafx_integration.ui.FormatedCellFactory;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private TableView<TimeBarEntry> tblTimeBars;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbCode;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbDuration;
    @FXML
    private TableColumn<TimeBarEntry, String> colTbBarTime;
    @FXML
    private TableColumn<TimeBarEntry, Double> colTbOpen;
    @FXML
    private TableColumn<TimeBarEntry, Double> colTbHigh;
    @FXML
    private TableColumn<TimeBarEntry, Double> colTbLow;
    @FXML
    private TableColumn<TimeBarEntry, Double> colTbClose;
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
        }
        else {
            TimeBarEntry timeBarEntry = new TimeBarEntry();
            timeBarEntry.updateUi(tb);
            timeBarData.add(timeBarEntry);
            timeBarsDataMap.put(tb, timeBarEntry);
        }
    }

    public void onClearTimeBars() {
        Platform.runLater(()-> {
            timeBarsDataMap.clear();
            timeBarData.clear();
        });
    }

    public void onApiConnection(boolean apiConnection) {
        Platform.runLater(()->{
            lblApiConnection.setTextFill(apiConnection ? Color.GREEN : Color.RED);
        });
    }

    public void onIbConnection(boolean ibConnection) {
        Platform.runLater(()->{
            lblIbConnection.setTextFill(ibConnection ? Color.GREEN : Color.RED);
        });
    }

    public static final class UpdateUiPositionsBatch {

        private final boolean fullUpdate;

        public UpdateUiPositionsBatch(boolean fullUpdate) {
            this.fullUpdate = fullUpdate;
        }

        public static enum Action { UPDATE, DELETE};

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


        colTbCode.setCellValueFactory(cellData -> cellData.getValue().localSymbolProperty());
        colTbDuration.setCellValueFactory(cellData -> cellData.getValue().durationProperty());
        colTbBarTime.setCellValueFactory(cellData -> cellData.getValue().barTimeProperty());

        colTbOpen.setCellValueFactory(cellData -> cellData.getValue().openProperty().asObject());
        colTbOpen.setCellFactory(new FormatedCellFactory<>("%.3f"));

        colTbHigh.setCellValueFactory(cellData -> cellData.getValue().highProperty().asObject());
        colTbHigh.setCellFactory(new FormatedCellFactory<>("%.3f"));

        colTbLow.setCellValueFactory(cellData -> cellData.getValue().lowProperty().asObject());
        colTbLow.setCellFactory(new FormatedCellFactory<>("%.3f"));

        colTbClose.setCellValueFactory(cellData -> cellData.getValue().closeProperty().asObject());
        colTbClose.setCellFactory(new FormatedCellFactory<>("%.3f"));

        colTbVolume.setCellValueFactory(cellData -> cellData.getValue().volumeProperty().asObject());
        colTbVolume.setCellFactory(new FormatedCellFactory<>("%.0f"));

        colTbLut.setCellValueFactory(cellData -> cellData.getValue().lutProperty());


        sortedTimeBarData.comparatorProperty().bind(tblTimeBars.comparatorProperty());
        tblTimeBars.setItems(sortedTimeBarData);


    }

    public void onPositionsUpdate(UpdateUiPositionsBatch uiUpdates) {
        Platform.runLater(()->{
            if (uiUpdates.isFullUpdate()) {
                positionsData.clear();
                positionsDataMap.clear();
            }
            for (UpdateUiPositionsBatch.BatchElement e : uiUpdates.getUpdates()) {
                if (e.action == UpdateUiPositionsBatch.Action.UPDATE) {
                    if (positionsDataMap.containsKey(e.localSymbol)) {
                        PositionEntry pe = positionsDataMap.get(e.localSymbol);
                        pe.updateUi(e.pi);
                    }
                    else {
                        PositionEntry pe = new PositionEntry();
                        pe.updateUi(e.pi);
                        positionsDataMap.put(e.localSymbol, pe);
                        positionsData.add(pe);
                    }
                }
                else {
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
}
