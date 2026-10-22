package com.clinic.management.service;

import com.clinic.management.dto.queue.CheckInRequest;
import com.clinic.management.dto.queue.QueueEntryResponse;
import com.clinic.management.dto.queue.QueueStatusUpdateRequest;
import com.clinic.management.dto.queue.QueueSummaryResponse;
import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Patient;
import com.clinic.management.entity.QueueEntry;
import com.clinic.management.entity.QueueStatus;
import com.clinic.management.exception.InvalidQueueOperationException;
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.AppointmentRepository;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.PatientRepository;
import com.clinic.management.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private QueueService queueService;

    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patient = new Patient("PAT-2026-0001", "Jane", "Doe", LocalDate.of(1995, 5, 10), "Female", "+1-555-0202", "jane@test.com");
        patient.setId(2L);

        doctor = new Doctor("Dr. Robert Jones", "General Medicine", "Room 102", "+1-555-0202", "dr.jones@clinic.com");
        doctor.setId(20L);
        doctor.setActive(true);
    }

    @Test
    @DisplayName("Successfully check in patient and assign sequential ticket number")
    void testCheckInPatient_Success() {
        CheckInRequest request = new CheckInRequest(2L, 20L, null, 0, "Mild flu symptoms");

        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(20L)).thenReturn(Optional.of(doctor));
        when(queueEntryRepository.findByPatientIdAndQueueDateAndStatusIn(eq(2L), any(LocalDate.class), any()))
                .thenReturn(Collections.emptyList());
        when(queueEntryRepository.findTopByQueueDateOrderBySequenceNumberDesc(any(LocalDate.class)))
                .thenReturn(Optional.empty());

        QueueEntry savedEntry = new QueueEntry("Q-001", LocalDate.now(), 1, patient, doctor, null);
        savedEntry.setId(50L);
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(savedEntry);
        when(queueEntryRepository.countByQueueDateAndDoctorIdAndStatus(any(LocalDate.class), eq(20L), eq(QueueStatus.WAITING)))
                .thenReturn(1L);

        QueueEntryResponse response = queueService.checkInPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getQueueNumber()).isEqualTo("Q-001");
        assertThat(response.getPositionInQueue()).isEqualTo(1);
        assertThat(response.getEstimatedWaitMinutes()).isEqualTo(15);
        assertThat(response.getStatus()).isEqualTo(QueueStatus.WAITING);
        verify(queueEntryRepository, times(1)).save(any(QueueEntry.class));
    }

    @Test
    @DisplayName("Throw InvalidQueueOperationException when patient is already waiting in today's queue")
    void testCheckInPatient_DuplicateActiveCheckIn() {
        CheckInRequest request = new CheckInRequest(2L, 20L, null, 0, "Duplicate attempt");

        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(20L)).thenReturn(Optional.of(doctor));

        QueueEntry activeTicket = new QueueEntry("Q-005", LocalDate.now(), 5, patient, doctor, null);
        when(queueEntryRepository.findByPatientIdAndQueueDateAndStatusIn(eq(2L), any(LocalDate.class), any()))
                .thenReturn(List.of(activeTicket));

        assertThatThrownBy(() -> queueService.checkInPatient(request))
                .isInstanceOf(InvalidQueueOperationException.class)
                .hasMessageContaining("Patient Jane Doe is already in today's active queue with number Q-005");

        verify(queueEntryRepository, never()).save(any(QueueEntry.class));
    }

    @Test
    @DisplayName("Successfully call next patient into consultation room (transition to IN_PROGRESS)")
    void testCallNextPatient_Success() {
        QueueEntry waitingPatient = new QueueEntry("Q-001", LocalDate.now(), 1, patient, doctor, null);
        waitingPatient.setId(10L);

        when(doctorRepository.findById(20L)).thenReturn(Optional.of(doctor));
        when(queueEntryRepository.findFirstByQueueDateAndDoctorIdAndStatusOrderByPriorityDescSequenceNumberAsc(
                any(LocalDate.class), eq(20L), eq(QueueStatus.WAITING)))
                .thenReturn(Optional.of(waitingPatient));

        when(queueEntryRepository.save(any(QueueEntry.class))).thenAnswer(i -> i.getArgument(0));

        QueueEntryResponse response = queueService.callNextPatient(20L);

        assertThat(response.getStatus()).isEqualTo(QueueStatus.IN_PROGRESS);
        assertThat(waitingPatient.getCalledTime()).isNotNull();
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when calling next patient on empty queue")
    void testCallNextPatient_EmptyQueue() {
        when(doctorRepository.findById(20L)).thenReturn(Optional.of(doctor));
        when(queueEntryRepository.findFirstByQueueDateAndDoctorIdAndStatusOrderByPriorityDescSequenceNumberAsc(
                any(LocalDate.class), eq(20L), eq(QueueStatus.WAITING)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.callNextPatient(20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No patients currently waiting in queue for doctor ID: 20");
    }

    @Test
    @DisplayName("Throw InvalidQueueOperationException when updating already COMPLETED queue entry")
    void testUpdateStatus_AlreadyCompleted_ThrowsException() {
        QueueEntry completedEntry = new QueueEntry("Q-001", LocalDate.now(), 1, patient, doctor, null);
        completedEntry.setId(100L);
        completedEntry.setStatus(QueueStatus.COMPLETED);

        when(queueEntryRepository.findById(100L)).thenReturn(Optional.of(completedEntry));

        QueueStatusUpdateRequest request = new QueueStatusUpdateRequest(QueueStatus.WAITING, "Try reopen");

        assertThatThrownBy(() -> queueService.updateQueueStatus(100L, request))
                .isInstanceOf(InvalidQueueOperationException.class)
                .hasMessageContaining("Cannot update status of a COMPLETED queue entry");
    }

    @Test
    @DisplayName("Successfully generate queue summary with positions and wait time calculations")
    void testGetQueueSummary() {
        QueueEntry active = new QueueEntry("Q-001", LocalDate.now(), 1, patient, doctor, null);
        active.setStatus(QueueStatus.IN_PROGRESS);

        QueueEntry waiting = new QueueEntry("Q-002", LocalDate.now(), 2, patient, doctor, null);
        waiting.setStatus(QueueStatus.WAITING);

        when(queueEntryRepository.countByQueueDate(any(LocalDate.class))).thenReturn(2L);
        when(queueEntryRepository.countByQueueDateAndStatus(any(LocalDate.class), eq(QueueStatus.WAITING))).thenReturn(1L);
        when(queueEntryRepository.countByQueueDateAndStatus(any(LocalDate.class), eq(QueueStatus.IN_PROGRESS))).thenReturn(1L);
        when(queueEntryRepository.countByQueueDateAndStatus(any(LocalDate.class), eq(QueueStatus.COMPLETED))).thenReturn(0L);
        when(queueEntryRepository.findByQueueDateOrderBySequenceNumberAsc(any(LocalDate.class)))
                .thenReturn(List.of(active, waiting));

        QueueSummaryResponse summary = queueService.getQueueSummary(LocalDate.now(), null);

        assertThat(summary.getTotalQueued()).isEqualTo(2);
        assertThat(summary.getCurrentlyServing()).hasSize(1);
        assertThat(summary.getWaitingList()).hasSize(1);
        assertThat(summary.getWaitingList().get(0).getPositionInQueue()).isEqualTo(1);
        assertThat(summary.getWaitingList().get(0).getEstimatedWaitMinutes()).isEqualTo(15);
    }
}
