package com.skywind.trading.spring_akka_integration;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.japi.pf.ReceiveBuilder;

import static com.skywind.trading.spring_akka_integration.EmailActor.BEAN_NAME;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * @author Admin
 */
@Component(value = BEAN_NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EmailActor extends AbstractActor {

    public final static String BEAN_NAME = "emailActor";

    private final Logger LOGGER = LoggerFactory.getLogger(EmailActor.class);

    @Value("${email.to}")
    private String emailTo;

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private SpringExtension springExtension;

    @Autowired
    private MailSender mailSender;

    @Override
    public Receive createReceive() {
        return receiveBuilder().
                match(Email.class, m -> {
                    sendEmail(m);
                }).
                match(ResendAttempt.class, m -> {
                    onResendAttempt(m);
                }).
                build();
    }



    private static final int RESEND_EMAIL_INTERVAL_SEC = 20;

    private final Queue<Email> pendingEmails = new LinkedList<>();

    private void sendQueuedEmails() {
        try {
            while (!pendingEmails.isEmpty()) {
                Email m = pendingEmails.peek();
                sendEmailImpl(m);
                pendingEmails.poll();
            }
        } catch (Throwable t) {
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(RESEND_EMAIL_INTERVAL_SEC, TimeUnit.SECONDS),
                    self(),
                    new ResendAttempt(),
                    getContext().dispatcher(),
                    self());
        }
    }

    private void onResendAttempt(ResendAttempt m) {
        sendQueuedEmails();
    }

    private void sendEmail(Email m) {
        pendingEmails.add(m);
        sendQueuedEmails();
    }

    private void sendEmailImpl(Email m) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(emailTo.split(Pattern.quote(",")));
        msg.setSubject(m.getSubject());
        msg.setText(m.getBody());
        mailSender.send(msg);
    }

    public static final class ResendAttempt {

    }

    public static final class Email {

        private final String subject;
        private final String body;

        public Email(String subject, String body) {
            this.subject = subject == null ? "" : subject;
            this.body = body == null ? "" : body;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }
    }
}
