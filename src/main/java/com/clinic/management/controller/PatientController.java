package com.clinic.management.controller;

import com.clinic.management.dto.common.ApiResponse;
import com.clinic.management.dto.patient.PatientRequest;
import com.clinic.management.dto.patient.PatientResponse;
import com.clinic.management.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Patient Registration and Demographic Records")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @Operation(summary = "Register new patient", description = "Registers a new patient and automatically generates a unique medical identifier (e.g. PAT-2026-0001).")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(@Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.createPatient(request);
        return new ResponseEntity<>(ApiResponse.success("Patient registered successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Search or list patients", description = "Retrieves patients by optional search query (matching name, phone, or medical identifier).")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getPatients(
            @RequestParam(name = "query", required = false) String query) {
        List<PatientResponse> patients = patientService.searchPatients(query);
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID", description = "Retrieves patient details by database primary key.")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable Long id) {
        PatientResponse patient = patientService.getPatientById(id);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping("/identifier/{identifier}")
    @Operation(summary = "Get patient by medical identifier", description = "Retrieves patient details by patient identifier code.")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientByIdentifier(@PathVariable String identifier) {
        PatientResponse patient = patientService.getPatientByIdentifier(identifier);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient", description = "Updates patient demographic and contact information.")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        PatientResponse updated = patientService.updatePatient(id, request);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", updated));
    }
}
