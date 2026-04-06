package com.project.edusync.notifications;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
@ConditionalOnProperty(prefix = "app.notifications.sns", name = "enabled", havingValue = "true")
public class NotificationPublisher {

    private final SnsClient snsClient;

    public NotificationPublisher(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    private final String TOPIC_ARN =
            "arn:aws:sns:us-east-1:000000000000:notification-topic";

    public void studentCreated(
            String studentName,
            String parentEmail,
            String parentPhone
    ) {

        String message = String.format("""
        {
          "event":"STUDENT_CREATED",
          "studentName":"%s",
          "parentEmail":"%s",
          "parentPhone":"%s"
        }
        """, studentName, parentEmail, parentPhone);

        snsClient.publish(
                PublishRequest.builder()
                        .topicArn(TOPIC_ARN)
                        .message(message)
                        .build()
        );
    }
}