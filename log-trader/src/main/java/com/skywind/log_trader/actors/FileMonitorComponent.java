package com.skywind.log_trader.actors;

import akka.actor.ActorSystem;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.messaging.Message;

@Configuration
public class FileMonitorComponent {

    @Value("${filePath}")
    private String filePath;

    @Autowired
    private ActorSystem actorSystem;

    public MessageProducerSupport fileContentProducer() {
        ApacheCommonsFileTailingMessageProducer tailFileProducer = new ApacheCommonsFileTailingMessageProducer();
        tailFileProducer.setFile(new File(filePath));
        return tailFileProducer;
    }

    @Bean
    public IntegrationFlow tailFilesFlow() {
        return IntegrationFlows.from(this.fileContentProducer())
                .handle((Message<?> msg) -> {
                    actorSystem.eventStream().publish(new FileActor.FileLine((String) msg.getPayload()));
                })
                .get();
    }
}

