package com.clinic.management.service;

import com.clinic.management.dto.doctor.DoctorRequest;
import com.clinic.management.dto.doctor.DoctorResponse;
import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.User;
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        Doctor doctor = new Doctor(
                request.getName(),
                request.getSpecialization(),
                request.getConsultationRoom(),
                request.getPhone(),
                request.getEmail()
        );

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            doctor.setUser(user);
        }

        Doctor savedDoctor = doctorRepository.save(doctor);
        return DoctorResponse.fromEntity(savedDoctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        return DoctorResponse.fromEntity(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(DoctorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getActiveDoctors() {
        return doctorRepository.findByActiveTrue().stream()
                .map(DoctorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));

        doctor.setName(request.getName());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setConsultationRoom(request.getConsultationRoom());
        doctor.setPhone(request.getPhone());
        doctor.setEmail(request.getEmail());

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            doctor.setUser(user);
        }

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return DoctorResponse.fromEntity(updatedDoctor);
    }

    @Transactional
    public void deactivateDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }
}
