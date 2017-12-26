package com.skywind.log_trader.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.japi.pf.ReceiveBuilder;

import static com.skywind.log_trader.actors.FileActor.BEAN_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import com.skywind.log_trader.ui.MainController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

    private boolean fileLoaded = false;

    @Autowired
    private MainController controller;

    private int linesProcessed = 0;

    @Override
    public void preStart() throws Exception {
        super.preStart();
        actorSystem.eventStream().subscribe(self(), FileLine.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FileLine.class, fl -> {
                    onNewFileLine(fl);
                })
                .match(ReadInitialFile.class, msg -> {
                    readInitialFile();
                })
                .matchAny(m -> recievedUnknown(m))
                .build();
    }

    private void recievedUnknown(Object m) {
    }

    private Signal.Action currAction = Signal.Action.WAIT;
    private Signal.Position currTgtPosition = Signal.Position.ZERO;

    private void onNewLine(String line, boolean tailer) {
        if (tailer && !fileLoaded) {
            return;
        }

        char signal = line.charAt(0);
        switch (signal) {
            case 'B': {
                currAction = Signal.Action.BUY;
                currTgtPosition = Signal.Position.LONG;
            }
            break;
            case 'S': {
                currAction = Signal.Action.SELL;
                currTgtPosition = Signal.Position.SHORT;
            }
            break;
            case 'W': {
                currAction = Signal.Action.WAIT;
            }
            break;
            case 'F': {
                currAction = Signal.Action.FLAT;
                currTgtPosition = Signal.Position.ZERO;
            }
            break;
        }
        if (tailer) {
            linesProcessed++;
            context().parent().tell(new Signal(currAction, currTgtPosition, linesProcessed), self());
        }
        // Draw lines in ui - ok
        controller.onFileLine(line);
        if (line.startsWith("B") || line.startsWith("S")) {
            controller.onLastSignalFileLine(line);
        }
    }

    private void onNewFileLine(FileLine fileLine) {
        onNewLine(fileLine.getLine(), true);
    }

    private void readInitialFile() throws IOException {

        String tmpFilePath = String.format("data/%s.tmp", UUID.randomUUID().toString());

        Files.copy(Paths.get(filePath), Paths.get(tmpFilePath));

        try (Stream<String> lines = Files.lines(Paths.get(tmpFilePath), Charset.defaultCharset())) {
            lines.forEachOrdered(line -> {
                linesProcessed++;
                onNewLine(line, false);
            });
        }
        context().parent().tell(new Signal(currAction, currTgtPosition, linesProcessed), self());
        Files.delete(Paths.get(tmpFilePath));
        fileLoaded = true;
    }

    public static final class ReadInitialFile {
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

    public static final class Signal {

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
        private final Position targetPosition;
        private final int linesProcessed;

        public Signal(Action action, Position targetPosition, int linesProcessed) {
            this.action = action;
            this.targetPosition = targetPosition;
            this.linesProcessed = linesProcessed;
        }

        public int getLinesProcessed() {
            return linesProcessed;
        }

        public Action getAction() {
            return action;
        }

        public Position getTargetPosition() {
            return targetPosition;
        }

    }
}
