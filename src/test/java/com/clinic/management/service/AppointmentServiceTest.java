package com.clinic.management.service;

import com.clinic.management.dto.appointment.AppointmentRequest;
import com.clinic.management.dto.appointment.AppointmentResponse;
import com.clinic.management.dto.appointment.RescheduleRequest;
import com.clinic.management.entity.Appointment;
import com.clinic.management.entity.AppointmentStatus;
import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Patient;
import com.clinic.management.exception.AppointmentConflictException;
import com.clinic.management.exception.BadRequestException;
import com.clinic.management.repository.AppointmentRepository;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient patient;
    private Doctor doctor;
    private LocalDateTime futureTime;

    @BeforeEach
    void setUp() {
        patient = new Patient("PAT-2026-0001", "John", "Doe", LocalDate.of(1990, 1, 1), "Male", "+1-555-0101", "john@test.com");
        patient.setId(1L);

        doctor = new Doctor("Dr. Sarah Smith", "Cardiology", "Room 101", "+1-555-0201", "dr.smith@clinic.com");
        doctor.setId(10L);
        doctor.setActive(true);

        futureTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Successfully schedule appointment when doctor and patient have no conflicts")
    void testCreateAppointment_Success() {
        AppointmentRequest request = new AppointmentRequest(1L, 10L, futureTime, 30, "Heart consultation");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findDoctorConflictingAppointments(eq(10L), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.findPatientConflictingAppointments(eq(1L), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        Appointment savedAppointment = new Appointment(patient, doctor, futureTime, futureTime.plusMinutes(30), "Heart consultation");
        savedAppointment.setId(100L);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getDoctorName()).isEqualTo("Dr. Sarah Smith");
        assertThat(response.getPatientName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Throw AppointmentConflictException when doctor is already booked at overlapping time")
    void testCreateAppointment_DoctorConflict() {
        AppointmentRequest request = new AppointmentRequest(1L, 10L, futureTime, 30, "Follow up");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));

        Appointment existingAppointment = new Appointment(patient, doctor, futureTime, futureTime.plusMinutes(30), "Existing");
        existingAppointment.setId(99L);
        when(appointmentRepository.findDoctorConflictingAppointments(eq(10L), any(), any(), any(), isNull()))
                .thenReturn(List.of(existingAppointment));

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("Doctor Dr. Sarah Smith is unavailable");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Throw AppointmentConflictException when patient already has another appointment at overlapping time")
    void testCreateAppointment_PatientConflict() {
        AppointmentRequest request = new AppointmentRequest(1L, 10L, futureTime, 30, "Follow up");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findDoctorConflictingAppointments(eq(10L), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        Appointment existingPatientApp = new Appointment(patient, doctor, futureTime, futureTime.plusMinutes(30), "Dentist");
        existingPatientApp.setId(88L);
        when(appointmentRepository.findPatientConflictingAppointments(eq(1L), any(), any(), any(), isNull()))
                .thenReturn(List.of(existingPatientApp));

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("Patient John Doe already has another appointment");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Successfully reschedule appointment when new time slot is available")
    void testRescheduleAppointment_Success() {
        Appointment existing = new Appointment(patient, doctor, futureTime, futureTime.plusMinutes(30), "Original");
        existing.setId(100L);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(existing));

        LocalDateTime newTime = futureTime.plusDays(1);
        RescheduleRequest request = new RescheduleRequest(newTime, 30, "Patient requested delay");

        when(appointmentRepository.findDoctorConflictingAppointments(eq(10L), any(), any(), any(), eq(100L)))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.findPatientConflictingAppointments(eq(1L), any(), any(), any(), eq(100L)))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.rescheduleAppointment(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAppointmentDateTime()).isEqualTo(newTime);
        assertThat(response.getEndTime()).isEqualTo(newTime.plusMinutes(30));
        verify(appointmentRepository).save(existing);
    }

    @Test
    @DisplayName("Throw BadRequestException when trying to cancel an already COMPLETED appointment")
    void testCancelCompletedAppointment_ThrowsException() {
        Appointment completed = new Appointment(patient, doctor, futureTime.minusDays(1), futureTime.minusDays(1).plusMinutes(30), "Past");
        completed.setId(101L);
        completed.setStatus(AppointmentStatus.COMPLETED);

        when(appointmentRepository.findById(101L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(101L, "Not needed"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel an appointment that is already completed");
    }

    @Test
    @DisplayName("Successfully cancel a scheduled appointment")
    void testCancelAppointment_Success() {
        Appointment scheduled = new Appointment(patient, doctor, futureTime, futureTime.plusMinutes(30), "Upcoming");
        scheduled.setId(102L);

        when(appointmentRepository.findById(102L)).thenReturn(Optional.of(scheduled));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(102L, "Patient feeling better");

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(scheduled);
    }
}
