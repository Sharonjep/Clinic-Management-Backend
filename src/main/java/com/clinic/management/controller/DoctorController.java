package com.clinic.management.controller;

import com.clinic.management.dto.common.ApiResponse;
import com.clinic.management.dto.doctor.DoctorRequest;
import com.clinic.management.dto.doctor.DoctorResponse;
import com.clinic.management.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Doctors", description = "Doctor Management Endpoints")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register doctor", description = "Admin-only endpoint to create doctor profiles and assign consultation rooms.")
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(@Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.createDoctor(request);
        return new ResponseEntity<>(ApiResponse.success("Doctor created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all doctors", description = "Retrieves active or all physicians in the clinic.")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        List<DoctorResponse> doctors = activeOnly ? doctorService.getActiveDoctors() : doctorService.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID", description = "Retrieves specific physician information.")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        DoctorResponse doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success(doctor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update doctor", description = "Admin-only endpoint to update doctor contact or room information.")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        DoctorResponse updated = doctorService.updateDoctor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate doctor", description = "Admin-only endpoint to deactivate a physician.")
    public ResponseEntity<ApiResponse<Void>> deactivateDoctor(@PathVariable Long id) {
        doctorService.deactivateDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor deactivated successfully", null));
    }
}
