package com.czertainly.scheduler.messaging.configuration;

import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class JmsConfiguration {

    @Bean
    public ConnectionFactory connectionFactory(MessagingProperties messagingProperties) {
        UriBuilder builder = UriComponentsBuilder.fromUriString(messagingProperties.brokerUrl());
        if (messagingProperties.vhost() != null) {
            builder.queryParam("amqp.vhost", messagingProperties.vhost());
        }

        JmsConnectionFactory factory = new JmsConnectionFactory(builder.build().toString());
        factory.setUsername(messagingProperties.user());
        factory.setPassword(messagingProperties.password());
        factory.setForceSyncSend(true);
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_typeId");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory,
                                   MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
