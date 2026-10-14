package com.otilm.scheduler.messaging;

import com.otilm.api.model.scheduler.SchedulerJobExecutionMessage;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Holds the message that tells core to run a job to a golden recorded on the Spring Boot 3.5 line, byte for byte, so a
 * change of JSON library cannot alter what core reads. Record with {@code -Dwire.golden.write=true} on the 3.5 line
 * only.
 */
@SpringBootTest
@ActiveProfiles("test")
class JmsMessageGoldenTest {

    /** The body exactly as sent, followed by the final newline every text file in the repository ends with. */
    private static final Path GOLDEN = Path.of("src/test/resources/wire/jms-job-execution-message.json");

    private JmsTemplate jmsTemplate;

    @Autowired
    void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Test
    void jobExecutionMessageTravelsAsJsonTextAlone() throws Exception {
        Session session = mock(Session.class);
        TextMessage text = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(text);

        Message message = jmsTemplate
                .getMessageConverter()
                .toMessage(new SchedulerJobExecutionMessage("CryptoAssetPqcSweepTask",
                        "com.otilm.core.tasks.CryptoAssetPqcSweepTask"), session);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(session).createTextMessage(body.capture());
        assertSame(text, message);
        verifyNoInteractions(text);
        String sent = body.getValue() + "\n";
        if (Boolean.getBoolean("wire.golden.write")) {
            Files.writeString(GOLDEN, sent);
        }
        assertEquals(Files.readString(GOLDEN), sent, "The message to core drifted from " + GOLDEN);
    }
}
