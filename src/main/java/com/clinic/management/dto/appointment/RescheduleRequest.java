package com.clinic.management.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class RescheduleRequest {

    @NotNull(message = "New appointment date and time is required")
    @Future(message = "New appointment date and time must be in the future")
    private LocalDateTime newAppointmentDateTime;

    @Min(value = 10, message = "Duration must be at least 10 minutes")
    @Max(value = 180, message = "Duration cannot exceed 180 minutes")
    private Integer durationMinutes;

    private String reason;

    public RescheduleRequest() {
    }

    public RescheduleRequest(LocalDateTime newAppointmentDateTime, Integer durationMinutes, String reason) {
        this.newAppointmentDateTime = newAppointmentDateTime;
        this.durationMinutes = durationMinutes;
        this.reason = reason;
    }

    public LocalDateTime getNewAppointmentDateTime() {
        return newAppointmentDateTime;
    }

    public void setNewAppointmentDateTime(LocalDateTime newAppointmentDateTime) {
        this.newAppointmentDateTime = newAppointmentDateTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
