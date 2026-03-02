package com.czertainly.scheduler.messaging;

import com.czertainly.scheduler.messaging.configuration.MessagingProperties;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingKeyMessagePostProcessorTest {

    @Mock
    private Message message;

    @Test
    void postProcessMessage_validRoutingKey_setsJmsType() throws JMSException {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange",
                new MessagingProperties.RoutingKey("my.routing.key"),
                null, null
        );
        RoutingKeyMessagePostProcessor processor = new RoutingKeyMessagePostProcessor(props);

        Message result = processor.postProcessMessage(message);

        verify(message).setJMSType("my.routing.key");
        assertSame(message, result);
    }

    @Test
    void postProcessMessage_nullRoutingKey_doesNotSetJmsType() throws JMSException {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );
        RoutingKeyMessagePostProcessor processor = new RoutingKeyMessagePostProcessor(props);

        Message result = processor.postProcessMessage(message);

        verify(message, never()).setJMSType(anyString());
        assertSame(message, result);
    }

    @Test
    void postProcessMessage_blankRoutingKey_doesNotSetJmsType() throws JMSException {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange",
                new MessagingProperties.RoutingKey("   "),
                null, null
        );
        RoutingKeyMessagePostProcessor processor = new RoutingKeyMessagePostProcessor(props);

        Message result = processor.postProcessMessage(message);

        verify(message, never()).setJMSType(anyString());
        assertSame(message, result);
    }

    @Test
    void postProcessMessage_nullSchedulerKey_doesNotSetJmsType() throws JMSException {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange",
                new MessagingProperties.RoutingKey(null),
                null, null
        );
        RoutingKeyMessagePostProcessor processor = new RoutingKeyMessagePostProcessor(props);

        Message result = processor.postProcessMessage(message);

        verify(message, never()).setJMSType(anyString());
        assertSame(message, result);
    }
}
