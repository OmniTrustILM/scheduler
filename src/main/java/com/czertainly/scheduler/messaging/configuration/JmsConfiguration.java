package com.czertainly.scheduler.messaging.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class JmsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JmsConfiguration.class);

    @Bean
    public ConnectionFactory connectionFactory(MessagingProperties messagingProperties) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(messagingProperties.getEffectiveBrokerUrl());

        // For RabbitMQ with AMQP 1.0, vhost is specified in the AMQP Open frame hostname field
        // The hostname field must be "vhost:name" format according to RabbitMQ AMQP 1.0 docs
        // We use amqp.vhost connection property to set this value
        boolean urlHasAmqpVhost = builder.build().getQueryParams().containsKey("amqp.vhost");
        boolean hasConfiguredVhost = messagingProperties.brokerType() == MessagingProperties.BrokerType.RABBITMQ
                && messagingProperties.virtualHost() != null
                && !messagingProperties.virtualHost().isEmpty();

        if (hasConfiguredVhost && urlHasAmqpVhost) {
            logger.warn("BROKER_URL already contains 'amqp.vhost' query parameter; ignoring BROKER_VIRTUAL_HOST={}",
                    messagingProperties.virtualHost());
        } else if (hasConfiguredVhost) {
            builder.queryParam("amqp.vhost", "vhost:" + messagingProperties.virtualHost());
        }
        String brokerUrl = builder.build().toUriString();

        JmsConnectionFactory factory = new JmsConnectionFactory(brokerUrl);
        // Sync sends so publish failures surface immediately; acceptable for low-volume scheduler messages.
        factory.setForceSyncSend(true);

        if (messagingProperties.brokerType() == MessagingProperties.BrokerType.SERVICEBUS) {
            configureServiceBusAuthentication(factory, messagingProperties);
            return factory;
        }

        // RabbitMQ - standard username/password authentication
        factory.setUsername(messagingProperties.username());
        factory.setPassword(messagingProperties.password());

        return factory;
    }

    /**
     * Configures authentication for Azure Service Bus.
     * Supports both AAD (Azure Active Directory) and SAS (Shared Access Signature) authentication.
     */
    private void configureServiceBusAuthentication(JmsConnectionFactory factory, MessagingProperties props) {
        if (props.aadAuth() != null && props.aadAuth().isEnabled()) {
            logger.debug("Configuring Azure Service Bus with AAD authentication");

            TokenCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(props.aadAuth().tenantId())
                    .clientId(props.aadAuth().clientId())
                    .clientSecret(props.aadAuth().clientSecret())
                    .build();

            AadTokenProvider tokenProvider = new AadTokenProvider(credential);

            // Special username for OAuth2 token authentication
            factory.setUsername("$jwt");
            factory.setExtension(
                    JmsConnectionExtensions.PASSWORD_OVERRIDE.toString(),
                    tokenProvider
            );
        } else {
            // SAS (Shared Access Signature) token authentication
            logger.debug("Configuring Azure Service Bus with SAS authentication");
            factory.setUsername(props.username());
            factory.setPassword(props.password());
        }
    }

    @Bean(destroyMethod = "stop")
    public JmsPoolConnectionFactory producerConnectionFactory(ConnectionFactory connectionFactory,
                                                               MessagingProperties messagingProperties) {
        // JmsPoolConnectionFactory manages connection/session lifecycle independently of
        // Spring's shared-connection mechanism. On connection failure (e.g. amqp:connection:forced),
        // the pool auto-evicts the dead connection and provides a fresh one on the next borrow —
        // no manual resetConnection() needed.
        //
        // connectionIdleTimeout evicts idle connections before the broker's idle thresholds.
        // useAnonymousProducers=true keeps a persistent AMQP link on the connection, preventing
        // the broker's "no active links" forced close.
        // connectionCheckInterval enables a background thread to actively evict stale connections.
        MessagingProperties.Pool poolConfig = messagingProperties.pool();
        if (poolConfig == null) {
            poolConfig = new MessagingProperties.Pool(null, null, null, null, null);
        }

        JmsPoolConnectionFactory pool = new JmsPoolConnectionFactory();
        pool.setConnectionFactory(connectionFactory);
        pool.setMaxConnections(poolConfig.maxConnections());
        pool.setConnectionIdleTimeout(poolConfig.connectionIdleTimeout());
        pool.setConnectionCheckInterval(poolConfig.connectionCheckInterval());
        pool.setMaxSessionsPerConnection(poolConfig.maxSessionsPerConnection());
        pool.setUseAnonymousProducers(poolConfig.useAnonymousProducers());
        pool.start();
        logger.info("Started JMS producer connection pool: maxConnections={}, connectionIdleTimeout={}ms, connectionCheckInterval={}ms, maxSessionsPerConnection={}, useAnonymousProducers={}",
                poolConfig.maxConnections(), poolConfig.connectionIdleTimeout(),
                poolConfig.connectionCheckInterval(), poolConfig.maxSessionsPerConnection(),
                poolConfig.useAnonymousProducers());
        return pool;
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(JmsPoolConnectionFactory producerConnectionFactory,
                                   MessageConverter messageConverter,
                                   MessagingProperties messagingProperties) {
        JmsTemplate template = new JmsTemplate(producerConnectionFactory);
        template.setMessageConverter(messageConverter);
        if (messagingProperties.brokerType() == MessagingProperties.BrokerType.SERVICEBUS) {
            template.setPubSubDomain(true);
        }
        return template;
    }
}
