package com.clinic.management.dto.queue;

import com.clinic.management.entity.QueueStatus;
import jakarta.validation.constraints.NotNull;

public class QueueStatusUpdateRequest {

    @NotNull(message = "Queue status is required")
    private QueueStatus status;

    private String notes;

    public QueueStatusUpdateRequest() {
    }

    public QueueStatusUpdateRequest(QueueStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
