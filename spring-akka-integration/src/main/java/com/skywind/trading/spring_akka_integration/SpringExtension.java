package com.skywind.trading.spring_akka_integration;

import akka.actor.Props;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringExtension {

    @Autowired
    private ApplicationContext appContext;

    public Props props(String actorBeanName) {
        return Props.create(SpringActorProducer.class, appContext, actorBeanName);
    }

    public Props props(String actorBeanName, Object... args) {
        return Props.create(SpringActorProducer.class, appContext, actorBeanName, args);
    }
}

