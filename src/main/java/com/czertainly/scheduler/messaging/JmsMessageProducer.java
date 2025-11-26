package com.czertainly.scheduler.messaging;

import com.czertainly.api.model.scheduler.SchedulerJobExecutionMessage;
import com.czertainly.scheduler.messaging.configuration.MessagingProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
public class JmsMessageProducer {
    private final JmsTemplate jmsTemplate;
    private final MessagePostProcessor messagePostProcessor;
    private final MessagingProperties messagingProperties;

    public JmsMessageProducer(JmsTemplate jmsTemplate, MessagePostProcessor messagePostProcessor, MessagingProperties messagingProperties) {
        this.jmsTemplate = jmsTemplate;
        this.messagePostProcessor = messagePostProcessor;
        this.messagingProperties = messagingProperties;
    }

    public void sendMessage(final SchedulerJobExecutionMessage schedulerExecutionMessage) {
        jmsTemplate.convertAndSend(messagingProperties.producerDestination(), schedulerExecutionMessage, messagePostProcessor);
    }
}
