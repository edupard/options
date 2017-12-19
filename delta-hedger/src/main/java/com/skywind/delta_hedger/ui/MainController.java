package com.skywind.delta_hedger.ui;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.skywind.delta_hedger.actors.HedgerActor;
import com.skywind.delta_hedger.actors.Position;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

public class MainController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ActorSystem actorSystem;

    private ActorSelection hedgerActor;

    @FXML
    private AnchorPane anchor;


    public Parent getParent() {
        return anchor;
    }

    public void onClose() {
        ((ConfigurableApplicationContext) applicationContext).close();
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
    }

    @PostConstruct
    public void init() {
        hedgerActor = actorSystem.actorSelection("/user/app/hedger");
    }

    @FXML
    public void onReloadPositions() {
        hedgerActor.tell(new HedgerActor.ReloadPositions(), null);

    }
}
