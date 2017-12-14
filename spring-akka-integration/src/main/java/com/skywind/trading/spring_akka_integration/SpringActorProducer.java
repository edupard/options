package com.skywind.trading.spring_akka_integration;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.springframework.context.ApplicationContext;

public class SpringActorProducer implements IndirectActorProducer {

    private final ApplicationContext appContext;
    private final String actorBeanName;
    private final Object[] args;

    public SpringActorProducer(ApplicationContext appContext, String actorBeanName) {
        this.appContext = appContext;
        this.actorBeanName = actorBeanName;
        this.args = null;
    }

    public SpringActorProducer(ApplicationContext appContext, String actorBeanName, Object... args) {
        this.appContext = appContext;
        this.actorBeanName = actorBeanName;
        this.args = args;
    }

    @Override
    public Actor produce() {
        if (args == null) {
            return (Actor) appContext.getBean(actorBeanName);
        }
        return (Actor) appContext.getBean(actorBeanName, args);
    }

    @Override
    public Class<? extends Actor> actorClass() {
        return (Class<? extends Actor>) appContext.getType(actorBeanName);
    }

}

