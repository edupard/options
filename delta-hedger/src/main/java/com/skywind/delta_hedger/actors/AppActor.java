package com.skywind.delta_hedger.actors;


import akka.actor.*;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import com.skywind.delta_hedger.ui.MainController;
import com.skywind.trading.spring_akka_integration.EmailActor;
import com.skywind.trading.spring_akka_integration.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import static com.skywind.delta_hedger.actors.AppActor.BEAN_NAME;


@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AppActor extends AbstractActor {

    private final Logger LOGGER = LoggerFactory.getLogger(AppActor.class);

    public static final class StartApplication {
    }

    public final static String BEAN_NAME = "appActor";

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private SpringExtension springExtension;

    @Autowired
    private MainController controller;

    private ActorRef hedgerActor;

    private ActorSelection emailActorSelection;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartApplication.class, m -> {
                    startApplication(m);
                })
                .matchAny(m -> recievedUnkknown(m))
                .build();
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(-1, Duration.Inf(),
            new Function<Throwable, Directive>() {
                @Override
                public Directive apply(Throwable t) {
                    LOGGER.error("", t);
                    if (t instanceof FatalException) {
                        return SupervisorStrategy.stop();
                    }

                    emailActorSelection.tell(new EmailActor.Email("Options: exception", t.getMessage()), self());
                    return SupervisorStrategy.restart();
                }
            });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private void startApplication(StartApplication m) {
        emailActorSelection = actorSystem.actorSelection("/user/email");

        hedgerActor = getContext().actorOf(springExtension.props(HedgerActor.BEAN_NAME), "hedger");
        hedgerActor.tell(new HedgerActor.Start(), self());
        LOGGER.debug("Started!");
    }

    private void recievedUnkknown(Object m) {
        LOGGER.debug("Recieved unknown message: {}", m.toString());
    }


}
