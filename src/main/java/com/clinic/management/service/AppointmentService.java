package com.clinic.management.service;

import com.clinic.management.dto.appointment.AppointmentRequest;
import com.clinic.management.dto.appointment.AppointmentResponse;
import com.clinic.management.dto.appointment.AppointmentStatusUpdateRequest;
import com.clinic.management.dto.appointment.RescheduleRequest;
import com.clinic.management.entity.Appointment;
import com.clinic.management.entity.AppointmentStatus;
import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Patient;
import com.clinic.management.exception.AppointmentConflictException;
import com.clinic.management.exception.BadRequestException;
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.AppointmentRepository;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES = Arrays.asList(
            AppointmentStatus.SCHEDULED,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.IN_CONSULTATION
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        if (!doctor.isActive()) {
            throw new BadRequestException("Doctor " + doctor.getName() + " is currently inactive and cannot accept appointments");
        }

        LocalDateTime startTime = request.getAppointmentDateTime();
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 30;
        LocalDateTime endTime = startTime.plusMinutes(duration);

        // Check Doctor Conflicts
        validateDoctorAvailability(doctor, startTime, endTime, null);

        // Check Patient Conflicts
        validatePatientAvailability(patient, startTime, endTime, null);

        Appointment appointment = new Appointment(patient, doctor, startTime, endTime, request.getReason());
        appointment.setNotes(request.getNotes());

        Appointment saved = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(Long id, RescheduleRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule an appointment that is already marked as COMPLETED");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Cannot reschedule a CANCELLED appointment. Please create a new booking");
        }

        LocalDateTime newStartTime = request.getNewAppointmentDateTime();
        long currentDurationMinutes = Duration.between(appointment.getAppointmentDateTime(), appointment.getEndTime()).toMinutes();
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : (int) currentDurationMinutes;
        LocalDateTime newEndTime = newStartTime.plusMinutes(duration);

        // Validate conflicts excluding current appointment
        validateDoctorAvailability(appointment.getDoctor(), newStartTime, newEndTime, appointment.getId());
        validatePatientAvailability(appointment.getPatient(), newStartTime, newEndTime, appointment.getId());

        appointment.setAppointmentDateTime(newStartTime);
        appointment.setEndTime(newEndTime);

        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            String updatedNotes = (appointment.getNotes() != null ? appointment.getNotes() + "\n" : "") +
                    "[Rescheduled on " + LocalDateTime.now().format(TIME_FORMATTER) + "]: " + request.getReason();
            appointment.setNotes(updatedNotes);
        }

        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id, String reason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel an appointment that is already completed");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        if (reason != null && !reason.trim().isEmpty()) {
            String updatedNotes = (appointment.getNotes() != null ? appointment.getNotes() + "\n" : "") +
                    "[Cancelled on " + LocalDateTime.now().format(TIME_FORMATTER) + "]: " + reason;
            appointment.setNotes(updatedNotes);
        }

        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatusUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        appointment.setStatus(request.getStatus());
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            appointment.setNotes(request.getNotes());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        return AppointmentResponse.fromEntity(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(Long doctorId, Long patientId, LocalDate date, AppointmentStatus status) {
        if (doctorId != null && date != null) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);
            return appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(doctorId, dayStart, dayEnd)
                    .stream()
                    .filter(a -> status == null || a.getStatus() == status)
                    .map(AppointmentResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        if (patientId != null) {
            return appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId)
                    .stream()
                    .filter(a -> status == null || a.getStatus() == status)
                    .map(AppointmentResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        if (date != null) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);
            return appointmentRepository.findByAppointmentDateTimeBetweenOrderByAppointmentDateTimeAsc(dayStart, dayEnd)
                    .stream()
                    .filter(a -> status == null || a.getStatus() == status)
                    .map(AppointmentResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        if (status != null) {
            return appointmentRepository.findByStatusOrderByAppointmentDateTimeAsc(status)
                    .stream()
                    .map(AppointmentResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return appointmentRepository.findAll().stream()
                .map(AppointmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateDoctorAvailability(Doctor doctor, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Appointment> conflicts = appointmentRepository.findDoctorConflictingAppointments(
                doctor.getId(), start, end, ACTIVE_STATUSES, excludeId
        );

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            throw new AppointmentConflictException(String.format(
                    "Doctor %s is unavailable. Conflicting appointment [%s] scheduled from %s to %s",
                    doctor.getName(),
                    conflict.getId(),
                    conflict.getAppointmentDateTime().format(TIME_FORMATTER),
                    conflict.getEndTime().format(TIME_FORMATTER)
            ));
        }
    }

    private void validatePatientAvailability(Patient patient, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Appointment> conflicts = appointmentRepository.findPatientConflictingAppointments(
                patient.getId(), start, end, ACTIVE_STATUSES, excludeId
        );

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            throw new AppointmentConflictException(String.format(
                    "Patient %s already has another appointment [%s] scheduled from %s to %s",
                    patient.getFullName(),
                    conflict.getId(),
                    conflict.getAppointmentDateTime().format(TIME_FORMATTER),
                    conflict.getEndTime().format(TIME_FORMATTER)
            ));
        }
    }
}
