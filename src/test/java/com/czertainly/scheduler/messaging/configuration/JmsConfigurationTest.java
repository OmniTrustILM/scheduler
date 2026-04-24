package com.czertainly.scheduler.messaging.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JmsConfigurationTest {

    private JmsConfiguration jmsConfiguration;

    @BeforeEach
    void setUp() {
        jmsConfiguration = new JmsConfiguration();
    }

    // --- connectionFactory: RabbitMQ ---

    @Test
    void connectionFactory_rabbitMq_setsUsernameAndPassword() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "myUser", "myPass", null,
                "exchange", null, null, null
        );

        ConnectionFactory factory = jmsConfiguration.connectionFactory(props);

        assertInstanceOf(JmsConnectionFactory.class, factory);
        JmsConnectionFactory jmsFactory = (JmsConnectionFactory) factory;
        assertEquals("myUser", jmsFactory.getUsername());
        assertEquals("myPass", jmsFactory.getPassword());
    }

    @Test
    void connectionFactory_rabbitMqWithVhost_addsVhostQueryParam() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", "myVhost",
                "exchange", null, null, null
        );

        ConnectionFactory factory = jmsConfiguration.connectionFactory(props);

        JmsConnectionFactory jmsFactory = (JmsConnectionFactory) factory;
        assertTrue(jmsFactory.getRemoteURI().contains("amqp.vhost=vhost%3AmyVhost")
                || jmsFactory.getRemoteURI().contains("amqp.vhost=vhost:myVhost"));
    }

    @Test
    void connectionFactory_rabbitMqWithoutVhost_noQueryParam() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );

        ConnectionFactory factory = jmsConfiguration.connectionFactory(props);

        JmsConnectionFactory jmsFactory = (JmsConnectionFactory) factory;
        assertFalse(jmsFactory.getRemoteURI().contains("amqp.vhost"));
    }

    // --- connectionFactory: ServiceBus ---

    @Test
    void connectionFactory_serviceBusWithSas_setsUsernameAndPassword() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                "sasKeyName", "sasKey", null,
                "exchange", null, null, null
        );

        ConnectionFactory factory = jmsConfiguration.connectionFactory(props);

        JmsConnectionFactory jmsFactory = (JmsConnectionFactory) factory;
        assertEquals("sasKeyName", jmsFactory.getUsername());
        assertEquals("sasKey", jmsFactory.getPassword());
    }

    @Test
    void connectionFactory_serviceBusWithAad_setsJwtUsername() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                null, null, null,
                "exchange", null,
                new MessagingProperties.AadAuth("tenant", "client", "secret"),
                null
        );

        ConnectionFactory factory = jmsConfiguration.connectionFactory(props);

        JmsConnectionFactory jmsFactory = (JmsConnectionFactory) factory;
        assertEquals("$jwt", jmsFactory.getUsername());
    }

    // --- producerConnectionFactory (pool) ---

    @Test
    void producerConnectionFactory_appliesPoolConfig() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange", null, null,
                new MessagingProperties.Pool(3, 15000, 30000, 200, false)
        );
        ConnectionFactory innerFactory = jmsConfiguration.connectionFactory(props);

        JmsPoolConnectionFactory pool = jmsConfiguration.producerConnectionFactory(innerFactory, props);

        try {
            assertEquals(3, pool.getMaxConnections());
            assertEquals(15000, pool.getConnectionIdleTimeout());
            assertEquals(30000, pool.getConnectionCheckInterval());
            assertEquals(200, pool.getMaxSessionsPerConnection());
            assertFalse(pool.isUseAnonymousProducers());
        } finally {
            pool.stop();
        }
    }

    @Test
    void producerConnectionFactory_nullPoolConfig_usesDefaults() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );
        ConnectionFactory innerFactory = jmsConfiguration.connectionFactory(props);

        JmsPoolConnectionFactory pool = jmsConfiguration.producerConnectionFactory(innerFactory, props);

        try {
            assertEquals(1, pool.getMaxConnections());
            assertEquals(30000, pool.getConnectionIdleTimeout());
            assertEquals(60000, pool.getConnectionCheckInterval());
            assertEquals(500, pool.getMaxSessionsPerConnection());
            assertTrue(pool.isUseAnonymousProducers());
        } finally {
            pool.stop();
        }
    }

    // --- messageConverter ---

    @Test
    void messageConverter_targetTypeIsText() {
        MessageConverter converter = jmsConfiguration.messageConverter(new ObjectMapper());

        assertInstanceOf(MappingJackson2MessageConverter.class, converter);
    }

    // --- jmsTemplate ---

    @Test
    void jmsTemplate_serviceBus_pubSubDomainTrue() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );
        ConnectionFactory innerFactory = jmsConfiguration.connectionFactory(props);
        JmsPoolConnectionFactory pool = jmsConfiguration.producerConnectionFactory(innerFactory, props);
        MessageConverter converter = jmsConfiguration.messageConverter(new ObjectMapper());

        try {
            JmsTemplate template = jmsConfiguration.jmsTemplate(pool, converter, props);
            assertTrue(template.isPubSubDomain());
        } finally {
            pool.stop();
        }
    }

    @Test
    void jmsTemplate_rabbitMq_pubSubDomainFalse() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );
        ConnectionFactory innerFactory = jmsConfiguration.connectionFactory(props);
        JmsPoolConnectionFactory pool = jmsConfiguration.producerConnectionFactory(innerFactory, props);
        MessageConverter converter = jmsConfiguration.messageConverter(new ObjectMapper());

        try {
            JmsTemplate template = jmsConfiguration.jmsTemplate(pool, converter, props);
            assertFalse(template.isPubSubDomain());
        } finally {
            pool.stop();
        }
    }
}
