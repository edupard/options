package com.skywind.log_trader.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.skywind.trading.spring_akka_integration.EmailActor;
import com.skywind.trading.spring_akka_integration.IAkkaAppFactory;
import com.skywind.trading.spring_akka_integration.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AkkaAppFactoryComponent implements IAkkaAppFactory {

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private SpringExtension springExtension;

    private ActorRef appActor;
    private ActorRef emailActor;

    @Override
    public void createActors() {
        emailActor = actorSystem.actorOf(springExtension.props(EmailActor.BEAN_NAME), "email");

        appActor = actorSystem.actorOf(springExtension.props(AppActor.BEAN_NAME), "app");
        appActor.tell(new AppActor.StartApplication(), ActorRef.noSender());
    }

    @Override
    public void stopActors() {
        actorSystem.stop(appActor);
    }

}
