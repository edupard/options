package com.skywind.log_trader;

import com.skywind.log_trader.actors.ActorsConfiguration;
import com.skywind.log_trader.ui.MainController;
import com.skywind.spring_javafx_integration.AbstractJavaFxApplicationSupport;
import com.skywind.trading.spring_akka_integration.AkkaConfiguration;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 * @author Admin
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
@Import(value = {
        AkkaConfiguration.class,
        ActorsConfiguration.class
})
public class Application extends AbstractJavaFxApplicationSupport {

    @Autowired
    private MainController controller;

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        launchApp(Application.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("log trader");
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("fxml/logo.png")));
        stage.setScene(new Scene(controller.getParent()));
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.setOnCloseRequest((WindowEvent we) -> {
            controller.onClose();
        });
        stage.show();

    }

}
