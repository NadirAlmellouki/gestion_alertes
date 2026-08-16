package FST.MST_RSI.PFA.notification.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notification_recipient")
public class NotificationRecipientEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationEntity notification;

    @Column(name = "person_id")
    private UUID personId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false)
    private String destination;

    @Column(name = "recipient_order", nullable = false)
    private int recipientOrder;

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("attemptNumber ASC")
    private List<NotificationAttemptEntity> attempts = new ArrayList<>();

    public NotificationRecipientEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NotificationEntity getNotification() {
        return notification;
    }

    public void setNotification(NotificationEntity notification) {
        this.notification = notification;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getRecipientOrder() {
        return recipientOrder;
    }

    public void setRecipientOrder(int recipientOrder) {
        this.recipientOrder = recipientOrder;
    }

    public List<NotificationAttemptEntity> getAttempts() {
        return attempts;
    }
}
