package com.clinic.management.controller;

import com.clinic.management.dto.common.ApiResponse;
import com.clinic.management.dto.queue.CheckInRequest;
import com.clinic.management.dto.queue.QueueEntryResponse;
import com.clinic.management.dto.queue.QueueStatusUpdateRequest;
import com.clinic.management.dto.queue.QueueSummaryResponse;
import com.clinic.management.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/queues")
@Tag(name = "Patient Queue", description = "Real-Time Patient Queue Management and Waiting Room Tracking")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Check in patient to daily queue",
            description = "Assigns an atomic sequential queue number (e.g. Q-001) for today, calculates position in line, and estimates wait time.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> checkInPatient(@Valid @RequestBody CheckInRequest request) {
        QueueEntryResponse response = queueService.checkInPatient(request);
        return new ResponseEntity<>(ApiResponse.success("Patient checked in to queue successfully", response), HttpStatus.CREATED);
    }

    @PostMapping("/call-next")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Call next waiting patient",
            description = "Advances queue for a doctor (or clinic), moving the highest priority or earliest checked-in patient to IN_PROGRESS.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> callNextPatient(
            @RequestParam(name = "doctorId", required = false) Long doctorId) {
        QueueEntryResponse response = queueService.callNextPatient(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Next patient called into consultation room", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Operation(summary = "Update patient queue status",
            description = "Transitions queue entry through lifecycle: WAITING -> IN_PROGRESS -> COMPLETED (or CANCELLED/NO_SHOW).")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody QueueStatusUpdateRequest request) {
        QueueEntryResponse response = queueService.updateQueueStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Queue status updated", response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    @Operation(summary = "Get daily queue summary and metrics",
            description = "Returns current waiting list with position numbers, estimated wait times, currently serving patients, and total statistics.")
    public ResponseEntity<ApiResponse<QueueSummaryResponse>> getQueueSummary(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "doctorId", required = false) Long doctorId) {
        QueueSummaryResponse summary = queueService.getQueueSummary(date, doctorId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/display")
    @Operation(summary = "Public waiting room display board",
            description = "Unauthenticated endpoint for waiting area TV monitors showing current ticket being served and upcoming tickets.")
    public ResponseEntity<ApiResponse<QueueSummaryResponse>> getDisplayBoard(
            @RequestParam(name = "doctorId", required = false) Long doctorId) {
        QueueSummaryResponse summary = queueService.getQueueSummary(LocalDate.now(), doctorId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
