package com.project.edusync.notifications.listener;

import com.project.edusync.notifications.NotificationPublisher;
import com.project.edusync.notifications.model.StudentCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
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
