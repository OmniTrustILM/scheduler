package com.czertainly.scheduler.messaging.configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(messagingProperties.brokerUrl());

        // For RabbitMQ with AMQP 1.0, vhost is specified in the AMQP Open frame hostname field
        // The hostname field must be "vhost:name" format according to RabbitMQ AMQP 1.0 docs
        // We use amqp.vhost connection property to set this value
        if (messagingProperties.brokerType() == MessagingProperties.BrokerType.RABBITMQ &&
                messagingProperties.vhost() != null &&
                !messagingProperties.vhost().isEmpty()) {
            builder.queryParam("amqp.vhost", "vhost:" + messagingProperties.vhost());
        }
        String brokerUrl = builder.build().toUriString();

        JmsConnectionFactory factory = new JmsConnectionFactory(brokerUrl);
        factory.setForceSyncSend(true);

        if (messagingProperties.brokerType() == MessagingProperties.BrokerType.SERVICEBUS) {
            configureServiceBusAuthentication(factory, messagingProperties);
            // no CachingConnectionFactory required for Azure Service Bus
            // cached connection would be hold opened and Service Bus this idle connection close after 60 seconds
            return factory;
        }

        // RabbitMQ - standard username/password authentication
        factory.setUsername(messagingProperties.user());
        factory.setPassword(messagingProperties.password());

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory(factory);
        cachingFactory.setSessionCacheSize(messagingProperties.sessionCacheSize());
        return cachingFactory;
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
            factory.setUsername(props.user());
            factory.setPassword(props.password());
        }
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory,
                                   MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setPubSubDomain(true);
        return template;
    }
}
