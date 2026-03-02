package com.czertainly.scheduler.messaging.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessagingPropertiesTest {

    // --- RabbitMQ validation ---

    @Test
    void rabbitMq_validWithBrokerUrl() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );
        assertNotNull(props);
    }

    @Test
    void rabbitMq_validWithHostAndPort() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                null, "localhost", 5672,
                "user", "pass", null,
                "exchange", null, null, null
        );
        assertNotNull(props);
    }

    @Test
    void rabbitMq_invalidWithoutBrokerUrlOrHostPort() {
        assertThrows(IllegalArgumentException.class, () -> new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                null, null, null,
                "user", "pass", null,
                "exchange", null, null, null
        ));
    }

    @Test
    void rabbitMq_invalidWithoutUsernamePassword() {
        assertThrows(IllegalArgumentException.class, () -> new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                null, null, null,
                "exchange", null, null, null
        ));
    }

    // --- ServiceBus validation ---

    @Test
    void serviceBus_validWithSasAuth() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                "sasKeyName", "sasKey", null,
                "exchange", null, null, null
        );
        assertNotNull(props);
    }

    @Test
    void serviceBus_validWithAadAuth() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                null, null, null,
                "exchange", null,
                new MessagingProperties.AadAuth("tenant", "client", "secret"),
                null
        );
        assertNotNull(props);
    }

    @Test
    void serviceBus_invalidWithoutBrokerUrl() {
        assertThrows(IllegalArgumentException.class, () -> new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                null, null, null,
                "user", "pass", null,
                "exchange", null, null, null
        ));
    }

    @Test
    void serviceBus_invalidWithoutAnyAuth() {
        assertThrows(IllegalArgumentException.class, () -> new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                null, null, null,
                "exchange", null, null, null
        ));
    }

    // --- getEffectiveBrokerUrl ---

    @Test
    void getEffectiveBrokerUrl_returnsBrokerUrlWhenSet() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://myhost:9999", null, null,
                "user", "pass", null,
                "exchange", null, null, null
        );
        assertEquals("amqp://myhost:9999", props.getEffectiveBrokerUrl());
    }

    @Test
    void getEffectiveBrokerUrl_constructsFromHostAndPort() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                null, "myhost", 5672,
                "user", "pass", null,
                "exchange", null, null, null
        );
        assertEquals("amqp://myhost:5672", props.getEffectiveBrokerUrl());
    }

    // --- producerDestination ---

    @Test
    void producerDestination_serviceBus_returnsExchange() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.SERVICEBUS,
                "amqps://sb.servicebus.windows.net", null, null,
                "user", "pass", null,
                "myExchange", null, null, null
        );
        assertEquals("myExchange", props.producerDestination());
    }

    @Test
    void producerDestination_rabbitMq_returnsExchangePath() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "myExchange",
                new MessagingProperties.RoutingKey("myKey"),
                null, null
        );
        assertEquals("/exchanges/myExchange/myKey", props.producerDestination());
    }

    @Test
    void producerDestination_rabbitMq_nullRoutingKey_returnsExchangePathWithEmptyKey() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "myExchange", null, null, null
        );
        assertEquals("/exchanges/myExchange/", props.producerDestination());
    }

    @Test
    void producerDestination_rabbitMq_nullSchedulerKey_returnsExchangePathWithEmptyKey() {
        MessagingProperties props = new MessagingProperties(
                MessagingProperties.BrokerType.RABBITMQ,
                "amqp://localhost:5672", null, null,
                "user", "pass", null,
                "myExchange",
                new MessagingProperties.RoutingKey(null),
                null, null
        );
        assertEquals("/exchanges/myExchange/", props.producerDestination());
    }

    // --- AadAuth.isEnabled ---

    @Test
    void aadAuth_isEnabled_allFieldsPresent() {
        MessagingProperties.AadAuth auth = new MessagingProperties.AadAuth("tenant", "client", "secret");
        assertTrue(auth.isEnabled());
    }

    @Test
    void aadAuth_isEnabled_missingTenantId() {
        MessagingProperties.AadAuth auth = new MessagingProperties.AadAuth(null, "client", "secret");
        assertFalse(auth.isEnabled());
    }

    @Test
    void aadAuth_isEnabled_missingClientId() {
        MessagingProperties.AadAuth auth = new MessagingProperties.AadAuth("tenant", null, "secret");
        assertFalse(auth.isEnabled());
    }

    @Test
    void aadAuth_isEnabled_missingClientSecret() {
        MessagingProperties.AadAuth auth = new MessagingProperties.AadAuth("tenant", "client", null);
        assertFalse(auth.isEnabled());
    }

    @Test
    void aadAuth_isEnabled_blankField() {
        MessagingProperties.AadAuth auth = new MessagingProperties.AadAuth("tenant", "", "secret");
        assertFalse(auth.isEnabled());
    }

    // --- Pool defaults ---

    @Test
    void pool_nullValuesGetDefaults() {
        MessagingProperties.Pool pool = new MessagingProperties.Pool(null, null, null, null, null);
        assertEquals(1, pool.maxConnections());
        assertEquals(30000, pool.connectionIdleTimeout());
        assertEquals(60000, pool.connectionCheckInterval());
        assertEquals(500, pool.maxSessionsPerConnection());
        assertTrue(pool.useAnonymousProducers());
    }

    @Test
    void pool_zeroMaxConnectionsGetsDefault() {
        MessagingProperties.Pool pool = new MessagingProperties.Pool(0, null, null, 0, null);
        assertEquals(1, pool.maxConnections());
        assertEquals(500, pool.maxSessionsPerConnection());
    }

    @Test
    void pool_explicitValuesPreserved() {
        MessagingProperties.Pool pool = new MessagingProperties.Pool(5, 10000, 20000, 100, false);
        assertEquals(5, pool.maxConnections());
        assertEquals(10000, pool.connectionIdleTimeout());
        assertEquals(20000, pool.connectionCheckInterval());
        assertEquals(100, pool.maxSessionsPerConnection());
        assertFalse(pool.useAnonymousProducers());
    }
}
