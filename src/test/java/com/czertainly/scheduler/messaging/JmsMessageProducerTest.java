
package com.czertainly.scheduler.messaging;

import com.czertainly.api.model.scheduler.SchedulerJobExecutionMessage;
import com.czertainly.scheduler.messaging.configuration.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JmsMessageProducerTest {

    @Mock
    private JmsTemplate jmsTemplate;
    
    @Mock
    private MessagePostProcessor messagePostProcessor;
    
    @Mock
    private MessagingProperties messagingProperties;
    
    @Mock
    private SchedulerJobExecutionMessage schedulerJobExecutionMessage;
    
    private JmsMessageProducer jmsMessageProducer;
    
    @BeforeEach
    void setUp() {
        jmsMessageProducer = new JmsMessageProducer(jmsTemplate, messagePostProcessor, messagingProperties);
    }
    
    @Test
    void testSendMessage_shouldCallJmsTemplateWithCorrectParameters() {
        // Given
        String exchangeName = "test.exchange";
        when(messagingProperties.producerDestination()).thenReturn(exchangeName);
        
        // When
        jmsMessageProducer.sendMessage(schedulerJobExecutionMessage);
        
        // Then
        verify(jmsTemplate).convertAndSend(exchangeName, schedulerJobExecutionMessage, messagePostProcessor);
    }
    
    @Test
    void testSendMessage_shouldUsePropertiesExchange() {
        // Given
        String expectedExchange = "scheduler.execution";
        when(messagingProperties.producerDestination()).thenReturn(expectedExchange);
        
        // When
        jmsMessageProducer.sendMessage(schedulerJobExecutionMessage);
        
        // Then
        verify(jmsTemplate).convertAndSend(expectedExchange, schedulerJobExecutionMessage, messagePostProcessor);
    }
}
