package com.skywind.delta_hedger.actors;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
@Configuration
@EnableScheduling
public class CronSchedullerComponent implements SchedulingConfigurer {

    @Autowired
    private Environment env;

    @Autowired
    private ActorSystem actorSystem;

    private ActorSelection hedgerActor;

    //cron.task.0=0 * * ? * * *
    //cron.task.0.param=A

    private static final String DEFAULT_VALUE = "";

    private final List<CronTaskConfig> cronTasks = new LinkedList<>();

    private final static class CronTaskConfig {
        private final String schedule;
        private final String scriptParam;

        public CronTaskConfig(String schedule, String scriptParam) {
            this.schedule = schedule;
            this.scriptParam = scriptParam;
        }
    }

    @PostConstruct
    public void postConstruct() {
        for (int i =0; i < 100; ++i) {
            String schedule = env.getProperty(String.format("cron.task.%d", i),DEFAULT_VALUE);
            String scriptParams = env.getProperty(String.format("cron.task.%d.param", i),DEFAULT_VALUE);
            if (schedule.equals(DEFAULT_VALUE)) {
                break;
            }
            cronTasks.add(new CronTaskConfig(schedule, scriptParams));
        }

        hedgerActor = actorSystem.actorSelection("/user/app/hedger");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        for (CronTaskConfig c : cronTasks) {
            CronTask ct = new CronTask(
                    ()-> {
                        hedgerActor.tell(new HedgerActor.RunAmendmentProcess(c.scriptParam, false), null);
                        },
                    c.schedule);
            taskRegistrar.scheduleCronTask(ct);
        }
    }
}
