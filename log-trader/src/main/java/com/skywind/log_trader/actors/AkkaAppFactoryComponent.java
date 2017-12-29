package com.skywind.log_trader.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.skywind.trading.spring_akka_integration.EmailActor;
import com.skywind.trading.spring_akka_integration.IAkkaAppFactory;
import com.skywind.trading.spring_akka_integration.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class AkkaAppFactoryComponent implements IAkkaAppFactory {

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private SpringExtension springExtension;

    private ActorRef appActor;
    private ActorRef emailActor;
    private ActorRef fileActor;

    private static final String DATA_DIR_PATH = "data";

    @PostConstruct
    public void postConstruct() {
        Path path = Paths.get(DATA_DIR_PATH);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void createActors() {
        emailActor = actorSystem.actorOf(springExtension.props(EmailActor.BEAN_NAME), "email");
        fileActor = actorSystem.actorOf(springExtension.props(FileActor.BEAN_NAME), "file");

        appActor = actorSystem.actorOf(springExtension.props(AppActor.BEAN_NAME), "app");
        appActor.tell(new AppActor.StartApplication(), ActorRef.noSender());
    }

    @Override
    public void stopActors() {
        actorSystem.stop(appActor);
    }

}
