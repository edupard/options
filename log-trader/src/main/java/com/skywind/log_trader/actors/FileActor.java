package com.skywind.log_trader.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

import static com.skywind.log_trader.actors.FileActor.BEAN_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

import com.skywind.log_trader.ui.MainController;
import com.skywind.trading.spring_akka_integration.MessageSentToExactActorInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author Admin
 */
@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class FileActor extends AbstractActor {

    public final static String BEAN_NAME = "fileActor";

    @Autowired
    private ActorSystem actorSystem;

    @Value("${filePath}")
    private String filePath;

    @Autowired
    private MainController controller;

    @Value("${long.size}")
    private double longSize;

    @Value("${short.size}")
    private double shortSize;

    private UUID logTraderActorId = null;

    private ActorSelection logTraderActorSelection;

    private boolean newLine = false;

    @PostConstruct
    public void postConstruct() throws IOException {
        String tmpFilePath = String.format("data/%s.tmp", UUID.randomUUID().toString());

        Files.copy(Paths.get(filePath), Paths.get(tmpFilePath));

        try (Stream<String> lines = Files.lines(Paths.get(tmpFilePath), Charset.defaultCharset())) {
            lines.forEachOrdered(line -> {
                onNewLine(line);
            });
        }
        Files.delete(Paths.get(tmpFilePath));
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        logTraderActorSelection = actorSystem.actorSelection("/user/app/logTrader");
        actorSystem.eventStream().subscribe(self(), FileLine.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FileLine.class, fl -> {
                    onNewFileLine(fl);
                })
                .match(SignalRequest.class, m -> {
                    onSignalRequest(m);
                })
                .matchAny(m -> recievedUnknown(m))
                .build();
    }

    private void onSignalRequest(SignalRequest m) {
        logTraderActorId = m.actorId;
        publishSignal();
    }

    private void recievedUnknown(Object m) {
    }

    private Signal.Action currAction = Signal.Action.WAIT;

    private void publishSignal() {
        logTraderActorSelection.tell(new Signal(currAction, currTargetPosition, newLine, logTraderActorId), self());
    }

    private static double ZERO_POSITION = 0.0d;

    private double currTargetPosition = ZERO_POSITION;

    private void onNewLine(String line) {
        char signal = line.charAt(0);
        switch (signal) {
            case 'B': {
                currAction = Signal.Action.BUY;
                currTargetPosition = longSize;
            }
            break;
            case 'S': {
                currAction = Signal.Action.SELL;
                currTargetPosition = -shortSize;
            }
            break;
            case 'W': {
                currAction = Signal.Action.WAIT;
            }
            break;
            case 'F': {
                currAction = Signal.Action.FLAT;
                currTargetPosition = ZERO_POSITION;
            }
            break;
        }
        // Draw lines in ui - ok
        controller.onTgtPosition(currTargetPosition);
        controller.onFileLine(line);
        if (line.startsWith("B") || line.startsWith("S")) {
            controller.onLastSignalFileLine(line);
        }
    }

    private void onNewFileLine(FileLine fileLine) {
        newLine = true;
        onNewLine(fileLine.getLine());
        publishSignal();
    }

    public static final class SignalRequest {
        private final UUID actorId;

        public SignalRequest(UUID actorId) {
            this.actorId = actorId;
        }
    }

    public static final class FileLine {

        private final String line;

        public FileLine(String line) {
            this.line = line;
        }

        public String getLine() {
            return line;
        }

    }

    public static final class Signal extends MessageSentToExactActorInstance {

        public enum Action {

            BUY,
            SELL,
            FLAT,
            WAIT;
        }

        public enum Position {

            LONG,
            SHORT,
            ZERO;
        }

        private final Action action;
        private final double targetPosition;
        private final boolean newLine;

        public Signal(Action action, double targetPosition, boolean newLine, UUID actorId) {
            super(actorId);
            this.action = action;
            this.targetPosition = targetPosition;
            this.newLine = newLine;
        }

        public boolean isNewLine() {
            return newLine;
        }

        public Action getAction() {
            return action;
        }

        public double getTargetPosition() {
            return targetPosition;
        }

    }
}
