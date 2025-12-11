package com.czertainly.scheduler.messaging;

import com.czertainly.scheduler.messaging.configuration.MessagingProperties;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * A message post-processor that adds a routing key property to JMS messages.
 * This class implements the {@link MessagePostProcessor} interface
 * and ensures that the "routingKey" property is set on outgoing messages before they are sent.

 * The routing key value is retrieved from the application configuration using the property
 * "spring.messaging.message.routing-key". If the property is not defined, a default value
 * of "scheduler" is used.
 */
@Component
public class RoutingKeyMessagePostProcessor implements MessagePostProcessor {

    private final MessagingProperties messagingProperties;

    public RoutingKeyMessagePostProcessor(MessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    @Override
    public @NonNull Message postProcessMessage(Message message) throws JMSException {
        message.setJMSType(messagingProperties.routingKey().scheduler());
        return message;
    }
}
