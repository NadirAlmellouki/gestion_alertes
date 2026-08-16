package FST.MST_RSI.PFA.notification.infrastructure.email;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryRequest;
import FST.MST_RSI.PFA.notification.infrastructure.config.EmailNotificationProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailNotificationSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(true);
        properties.setFrom("alertops@test.local");
        adapter = new EmailNotificationSenderAdapter(mailSender, properties);
    }

    @Test
    void sendsEmailSuccessfully() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getMessageID()).thenReturn("msg-123");

        var request = new NotificationDeliveryRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tam@example.com",
                "Jane Doe",
                UUID.randomUUID(),
                "Subject",
                "Body",
                "corr-1"
        );

        var result = adapter.send(request);

        assertThat(result.success()).isTrue();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void failsWhenRecipientMissing() {
        var request = new NotificationDeliveryRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                " ",
                "Jane Doe",
                UUID.randomUUID(),
                "Subject",
                "Body",
                "corr-1"
        );

        var result = adapter.send(request);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("missing");
    }

    @Test
    void failsWhenSmtpThrows() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        var request = new NotificationDeliveryRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tam@example.com",
                "Jane Doe",
                UUID.randomUUID(),
                "Subject",
                "Body",
                "corr-1"
        );

        var result = adapter.send(request);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("SMTP down");
    }
}
