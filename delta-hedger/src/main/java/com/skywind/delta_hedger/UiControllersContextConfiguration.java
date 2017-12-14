package com.skywind.delta_hedger;

import com.skywind.delta_hedger.ui.MainController;
import com.skywind.spring_javafx_integration.ControllerAndParent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class UiControllersContextConfiguration {

    @Bean
    public MainController mainController() throws IOException {
        ControllerAndParent h = loadView("fxml/main.fxml");
        return (MainController) h.getController();
    }

    protected ControllerAndParent loadView(String url) throws IOException {
        try (InputStream fxmlStream = getClass().getClassLoader().getResourceAsStream(url)) {
            return ControllerAndParent.loadView(fxmlStream);
        }
    }

}