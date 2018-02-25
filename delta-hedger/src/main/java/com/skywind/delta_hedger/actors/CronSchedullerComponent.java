package com.skywind.delta_hedger.actors;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.skywind.delta_hedger.ui.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javax.swing.text.StyledEditorKit;
import java.util.*;

@Component
@Configuration
@EnableScheduling
public class CronSchedullerComponent implements SchedulingConfigurer {

    private final Logger LOGGER = LoggerFactory.getLogger(CronSchedullerComponent.class);

    @Autowired
    private Environment env;

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private MainController controller;

    private ActorSelection hedgerActor;

    //cron.task.0=0 * * ? * * *
    //cron.task.0.param=A

    private static final String DEFAULT_VALUE = "";

    private final List<CronTaskConfig> cronTasks = new LinkedList<>();

    private final static class CronTaskConfig {
        private final String schedule;
        private final String scriptParam;
        private final TradeAction tradeAction;
        private final Set<String> targetUnderlyings;

        public CronTaskConfig(String schedule, String scriptParam, TradeAction tradeAction, Set<String> targetUnderlyings) {
            this.schedule = schedule;
            this.scriptParam = scriptParam;
            this.tradeAction = tradeAction;
            this.targetUnderlyings = targetUnderlyings;
        }
    }

    @PostConstruct
    public void postConstruct() {
        for (int i = 0; i < 100; ++i) {
            String schedule = env.getProperty(String.format("cron.task.%d", i), DEFAULT_VALUE);

            if (schedule.equals(DEFAULT_VALUE)) {
                continue;
            }
            String scriptParams = env.getProperty(String.format("cron.task.%d.param", i));
            TradeAction tradeAction = TradeAction.valueOf(env.getProperty(String.format("cron.task.%d.trigger", i)));
            String sUnderlyings = env.getProperty(String.format("cron.task.%d.underlyings", i), DEFAULT_VALUE);
            Set<String> targetUnderlyings = new HashSet<>();
            if (!sUnderlyings.equals(DEFAULT_VALUE)) {
                String[] split = sUnderlyings.split(",");
                for (String u : split) {
                    targetUnderlyings.add(u);
                }
            }

            cronTasks.add(new CronTaskConfig(schedule, scriptParams, tradeAction, targetUnderlyings));
        }

        hedgerActor = actorSystem.actorSelection("/user/app/hedger");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        for (CronTaskConfig c : cronTasks) {
            CronTask ct;
            ct = new CronTask(
                    () -> {
                        hedgerActor.tell(new HedgerActor.RunAmendmentProcess(c.targetUnderlyings, c.scriptParam, c.tradeAction, HedgerActor.RunAmendmentProcess.TriggerType.CRON), null);
                        LOGGER.debug("CRON: {}", c.scriptParam);
                    },
                    c.schedule);
            taskRegistrar.scheduleCronTask(ct);
        }
    }
}
