package com.czertainly.scheduler.messaging;

import com.czertainly.api.model.scheduler.SchedulerJobExecutionMessage;
import com.czertainly.scheduler.messaging.configuration.MessagingProperties;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class JmsMessageProducer {
    private static final Logger logger = getLogger(JmsMessageProducer.class);

    private final JmsTemplate jmsTemplate;
    private final MessagePostProcessor messagePostProcessor;
    private final MessagingProperties messagingProperties;

    public JmsMessageProducer(JmsTemplate jmsTemplate, MessagePostProcessor messagePostProcessor, MessagingProperties messagingProperties) {
        this.jmsTemplate = jmsTemplate;
        this.messagePostProcessor = messagePostProcessor;
        this.messagingProperties = messagingProperties;
    }

    public void sendMessage(final SchedulerJobExecutionMessage schedulerExecutionMessage) {
        logger.info("Sending message to: {}", messagingProperties.producerDestination());
        jmsTemplate.convertAndSend(messagingProperties.producerDestination(), schedulerExecutionMessage, messagePostProcessor);
    }
}
