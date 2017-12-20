package com.skywind.trading.spring_akka_integration;

import java.util.UUID;

public class MessageSentToExactActorInstance {

    private final UUID actorId;

    public MessageSentToExactActorInstance(UUID actorId) {
        this.actorId = actorId;
    }

    public boolean isActorInstaceEquals(UUID actorId) {
        return this.actorId.equals(actorId);
    }
}
