package com.project.edusync.notifications.listener;

import com.project.edusync.notifications.NotificationPublisher;
import com.project.edusync.notifications.model.StudentCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = "app.notifications.sns", name = "enabled", havingValue = "true")
public class StudentNotificationListener {

    private final NotificationPublisher notificationPublisher;

    public StudentNotificationListener(
            NotificationPublisher notificationPublisher
    ) {
        this.notificationPublisher = notificationPublisher;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleStudentCreated(
            StudentCreatedEvent event
    ) {

        notificationPublisher.studentCreated(
                event.getStudentName(),
                event.getParentEmail(),
                event.getParentPhone()
        );
    }
}
