package com.skywind.trading.spring_akka_integration;

import akka.actor.ActorSystem;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

@Component
public class AppComponent {

    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AppComponent.class);

    @Autowired
    private IAkkaAppFactory factory;

    @Autowired
    private ActorSystem actorSystem;



    @PostConstruct
    public void start() {
        factory.createActors();
    }

    @PreDestroy
    public void stop() {
        factory.stopActors();
        try {
            actorSystem.terminate().result(Duration.Inf(), null);
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
    }

}
