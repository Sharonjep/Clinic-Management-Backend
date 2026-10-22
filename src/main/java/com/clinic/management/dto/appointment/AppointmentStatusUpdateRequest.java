package com.clinic.management.dto.appointment;

import com.clinic.management.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public class AppointmentStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private AppointmentStatus status;

    private String notes;

    public AppointmentStatusUpdateRequest() {
    }

    public AppointmentStatusUpdateRequest(AppointmentStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
