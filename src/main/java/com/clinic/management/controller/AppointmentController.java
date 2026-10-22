package com.clinic.management.controller;

import com.clinic.management.dto.appointment.AppointmentRequest;
import com.clinic.management.dto.appointment.AppointmentResponse;
import com.clinic.management.dto.appointment.AppointmentStatusUpdateRequest;
import com.clinic.management.dto.appointment.RescheduleRequest;
import com.clinic.management.dto.common.ApiResponse;
import com.clinic.management.entity.AppointmentStatus;
import com.clinic.management.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment Booking, Rescheduling, and Collision Prevention")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @Operation(summary = "Schedule appointment", description = "Creates a new appointment. Performs collision validation to prevent overlapping bookings for doctor and patient.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return new ResponseEntity<>(ApiResponse.success("Appointment booked successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Query appointments", description = "Retrieves appointments filtered by doctor, patient, date, or appointment status.")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "status", required = false) AppointmentStatus status) {
        List<AppointmentResponse> appointments = appointmentService.getAppointments(doctorId, patientId, date, status);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID", description = "Retrieves full details of a specific appointment.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(@PathVariable Long id) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(ApiResponse.success(appointment));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule appointment", description = "Changes appointment time. Validates doctor and patient availability, preventing schedule conflicts.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            @PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled successfully", response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment", description = "Cancels a booked appointment and records cancellation reason.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestParam(name = "reason", required = false) String reason) {
        AppointmentResponse response = appointmentService.cancelAppointment(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status", description = "Updates status (e.g., CONFIRMED, IN_CONSULTATION, COMPLETED).")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        AppointmentResponse response = appointmentService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Appointment status updated", response));
    }
}
