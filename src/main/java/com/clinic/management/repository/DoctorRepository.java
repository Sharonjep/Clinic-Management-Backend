package com.clinic.management.repository;

import com.clinic.management.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByActiveTrue();

    Optional<Doctor> findByEmail(String email);

    List<Doctor> findBySpecializationIgnoreCaseAndActiveTrue(String specialization);
}
