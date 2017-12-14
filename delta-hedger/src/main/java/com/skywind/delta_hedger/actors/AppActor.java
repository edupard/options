package com.skywind.delta_hedger.actors;

import akka.actor.ActorSystem;
import com.skywind.delta_hedger.ui.MainController;
import com.skywind.trading.spring_akka_integration.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import static com.skywind.delta_hedger.actors.AppActor.BEAN_NAME;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;

import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.japi.pf.ReceiveBuilder;


@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AppActor extends AbstractActor {

    public static final class StartApplication {
    }

    public final static String BEAN_NAME = "appActor";

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private SpringExtension springExtension;

    @Autowired
    private MainController controller;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartApplication.class, s -> {
                    startApplication();
                })
                .matchAny(o -> recievedUnkknown(o))
                .build();
    }

    private void startApplication() {

    }

    private void recievedUnkknown(Object o) {

    }


}
