package com.clinic.management.repository;

import com.clinic.management.entity.Appointment;
import com.clinic.management.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
            Long doctorId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId);

    List<Appointment> findByAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(
            LocalDateTime start, LocalDateTime end);

    List<Appointment> findByStatusOrderByAppointmentDateTimeAsc(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.doctor.id = :doctorId " +
           "AND a.status IN :activeStatuses " +
           "AND a.appointmentDateTime < :endTime " +
           "AND a.endTime > :startTime " +
           "AND (:excludeId IS NULL OR a.id != :excludeId)")
    List<Appointment> findDoctorConflictingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<AppointmentStatus> activeStatuses,
            @Param("excludeId") Long excludeId);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.patient.id = :patientId " +
           "AND a.status IN :activeStatuses " +
           "AND a.appointmentDateTime < :endTime " +
           "AND a.endTime > :startTime " +
           "AND (:excludeId IS NULL OR a.id != :excludeId)")
    List<Appointment> findPatientConflictingAppointments(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<AppointmentStatus> activeStatuses,
            @Param("excludeId") Long excludeId);
}
