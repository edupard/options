package com.skywind.spring_javafx_integration.ui;

import java.time.Instant;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.util.Duration;

//http://jaakkola.net/juhani/blog/?p=233
public class FlashingTableCell<S, T> extends TableCell<S, T> {

    private static final int HIGHLIGHT_DURATION_MS = 600;
    private static final Duration HIGHLIGHT_DURATION_FX = Duration.millis(HIGHLIGHT_DURATION_MS);
    private static final java.time.Duration HIGHLIGHT_DURATION_JAVA = java.time.Duration.ofMillis(HIGHLIGHT_DURATION_MS);

    private Instant lastUpdateInstant = null;

    @Override
    protected void updateItem(T value,
            boolean empty) {
        T oldValue = getItem();
        super.updateItem(value, empty);

        if (value == null || empty) {
            setText(null);
        } else {
            setText(value.toString());
        }

        boolean changed = false;
        if (oldValue != null) {
            if (value != null) {
                System.out.println(String.format("old: %s new: %s", oldValue.toString(), value.toString()));
                if (!oldValue.toString().equals(value.toString())) {

                    changed = true;
                }
            } else {
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        if (lastUpdateInstant != null) {
            setStyle("-fx-background-color: lightgreen");
        }
        lastUpdateInstant = Instant.now();

        Timeline wonder = new Timeline(new KeyFrame(HIGHLIGHT_DURATION_FX, new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                if (java.time.Duration.between(lastUpdateInstant, Instant.now()).compareTo(HIGHLIGHT_DURATION_JAVA) >= 0) {
                    setStyle("");
                }
            }
        }));
        wonder.setCycleCount(1);
        wonder.play();
    }
}
