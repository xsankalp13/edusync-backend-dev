package com.project.edusync.notifications.model;

public class StudentCreatedEvent {

    private final String studentName;
    private final String parentEmail;
    private final String parentPhone;

    public StudentCreatedEvent(
            String studentName,
            String parentEmail,
            String parentPhone
    ) {
        this.studentName = studentName;
        this.parentEmail = parentEmail;
        this.parentPhone = parentPhone;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public String getParentPhone() {
        return parentPhone;
    }
}
