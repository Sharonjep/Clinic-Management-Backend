package com.clinic.management.service;

import com.clinic.management.dto.patient.PatientRequest;
import com.clinic.management.dto.patient.PatientResponse;
import com.clinic.management.entity.Patient;
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        String identifier = generatePatientIdentifier();

        Patient patient = new Patient(
                identifier,
                request.getFirstName(),
                request.getLastName(),
                request.getDateOfBirth(),
                request.getGender(),
                request.getPhone(),
                request.getEmail()
        );
        patient.setAddress(request.getAddress());
        patient.setEmergencyContact(request.getEmergencyContact());
        patient.setMedicalNotes(request.getMedicalNotes());

        Patient savedPatient = patientRepository.save(patient);
        return PatientResponse.fromEntity(savedPatient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        return PatientResponse.fromEntity(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientByIdentifier(String identifier) {
        Patient patient = patientRepository.findByPatientIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "identifier", identifier));
        return PatientResponse.fromEntity(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(PatientResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> searchPatients(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPatients();
        }
        return patientRepository.searchPatients(query.trim()).stream()
                .map(PatientResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setPhone(request.getPhone());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContact(request.getEmergencyContact());
        patient.setMedicalNotes(request.getMedicalNotes());

        Patient updatedPatient = patientRepository.save(patient);
        return PatientResponse.fromEntity(updatedPatient);
    }

    private synchronized String generatePatientIdentifier() {
        int currentYear = Year.now().getValue();
        long count = patientRepository.count() + 1;
        String identifier = String.format("PAT-%d-%04d", currentYear, count);

        // In case of conflict, increment until unique
        while (patientRepository.existsByPatientIdentifier(identifier)) {
            count++;
            identifier = String.format("PAT-%d-%04d", currentYear, count);
        }

        return identifier;
    }
}
