package com.czertainly.scheduler.messaging.configuration;

import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class JmsConfiguration {

    @Bean
    public ConnectionFactory connectionFactory(MessagingProperties messagingProperties) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(messagingProperties.brokerUrl());

        // For RabbitMQ with AMQP 1.0, vhost is specified in the AMQP Open frame hostname field
        // The hostname field must be "vhost:name" format according to RabbitMQ AMQP 1.0 docs
        // We use amqp.vhost connection property to set this value
        if (messagingProperties.name() == MessagingProperties.BrokerName.RABBITMQ &&
                messagingProperties.vhost() != null &&
                !messagingProperties.vhost().isEmpty()) {
            builder.queryParam("amqp.vhost", "vhost:" + messagingProperties.vhost());
        }
        String brokerUrl = builder.build().toUriString();

        JmsConnectionFactory factory = new JmsConnectionFactory(brokerUrl);
        factory.setUsername(messagingProperties.user());
        factory.setPassword(messagingProperties.password());
        factory.setForceSyncSend(true);
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
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
