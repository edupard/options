package com.skywind.delta_hedger;

import com.skywind.delta_hedger.actors.ActorsConfiguration;
import com.skywind.delta_hedger.ui.MainController;
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
import org.springframework.context.annotation.PropertySource;

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
@PropertySource(value = "file:user.properties", ignoreResourceNotFound = true)
public class Application extends AbstractJavaFxApplicationSupport {

    @Autowired
    private MainController controller;

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        launchApp(Application.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("delta hedger");
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
