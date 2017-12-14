package com.skywind.trading.spring_akka_integration;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@ComponentScan
public class AkkaConfiguration {
    @Bean
    public ActorSystem actorSystem(@Value("${akka.system}") String akkaSystemName) {
        Config config = ConfigFactory.parseFile(new File("akka.conf"));
        ActorSystem system = ActorSystem.create(akkaSystemName, config);
        return system;
    }

}
