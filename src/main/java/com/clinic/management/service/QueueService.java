package com.clinic.management.service;

import com.clinic.management.dto.queue.CheckInRequest;
import com.clinic.management.dto.queue.QueueEntryResponse;
import com.clinic.management.dto.queue.QueueStatusUpdateRequest;
import com.clinic.management.dto.queue.QueueSummaryResponse;
import com.clinic.management.entity.*;
import com.clinic.management.exception.BadRequestException;
import com.clinic.management.exception.InvalidQueueOperationException;
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.AppointmentRepository;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.PatientRepository;
import com.clinic.management.repository.QueueEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class QueueService {

    private static final int ESTIMATED_MINUTES_PER_PATIENT = 15;

    private final QueueEntryRepository queueEntryRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public QueueService(QueueEntryRepository queueEntryRepository,
                        PatientRepository patientRepository,
                        DoctorRepository doctorRepository,
                        AppointmentRepository appointmentRepository) {
        this.queueEntryRepository = queueEntryRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public QueueEntryResponse checkInPatient(CheckInRequest request) {
        LocalDate today = LocalDate.now();

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        if (!doctor.isActive()) {
            throw new BadRequestException("Doctor " + doctor.getName() + " is currently inactive and cannot accept queue check-ins");
        }

        // Prevent duplicate active queue entry for the same patient today
        List<QueueStatus> activeStatuses = Arrays.asList(QueueStatus.WAITING, QueueStatus.IN_PROGRESS);
        List<QueueEntry> activeEntries = queueEntryRepository.findByPatientIdAndQueueDateAndStatusIn(
                patient.getId(), today, activeStatuses);

        if (!activeEntries.isEmpty()) {
            QueueEntry existing = activeEntries.get(0);
            throw new InvalidQueueOperationException(String.format(
                    "Patient %s is already in today's active queue with number %s (Status: %s)",
                    patient.getFullName(),
                    existing.getQueueNumber(),
                    existing.getStatus()
            ));
        }

        // Determine next sequence number for today
        int nextSequence = queueEntryRepository.findTopByQueueDateOrderBySequenceNumberDesc(today)
                .map(last -> last.getSequenceNumber() + 1)
                .orElse(1);

        String queueNumber = String.format("Q-%03d", nextSequence);

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", request.getAppointmentId()));

            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                throw new BadRequestException("Cannot check in against a CANCELLED appointment");
            }
            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                throw new BadRequestException("Cannot check in against an already COMPLETED appointment");
            }

            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
        }

        QueueEntry queueEntry = new QueueEntry(queueNumber, today, nextSequence, patient, doctor, appointment);
        if (request.getPriority() != null) {
            queueEntry.setPriority(request.getPriority());
        }
        queueEntry.setNotes(request.getNotes());

        QueueEntry saved = queueEntryRepository.save(queueEntry);

        // Compute queue position
        long waitingAhead = queueEntryRepository.countByQueueDateAndDoctorIdAndStatus(today, doctor.getId(), QueueStatus.WAITING);
        int position = (int) waitingAhead;
        int estimatedWait = Math.max(0, position * ESTIMATED_MINUTES_PER_PATIENT);

        return QueueEntryResponse.fromEntity(saved, position, estimatedWait);
    }

    @Transactional
    public QueueEntryResponse callNextPatient(Long doctorId) {
        LocalDate today = LocalDate.now();

        QueueEntry nextPatient;
        if (doctorId != null) {
            doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));

            nextPatient = queueEntryRepository
                    .findFirstByQueueDateAndDoctorIdAndStatusOrderByPriorityDescSequenceNumberAsc(today, doctorId, QueueStatus.WAITING)
                    .orElseThrow(() -> new ResourceNotFoundException("No patients currently waiting in queue for doctor ID: " + doctorId));
        } else {
            nextPatient = queueEntryRepository
                    .findFirstByQueueDateAndStatusOrderByPriorityDescSequenceNumberAsc(today, QueueStatus.WAITING)
                    .orElseThrow(() -> new ResourceNotFoundException("No patients currently waiting in the clinic queue"));
        }

        nextPatient.setStatus(QueueStatus.IN_PROGRESS);
        nextPatient.setCalledTime(LocalDateTime.now());

        if (nextPatient.getAppointment() != null) {
            nextPatient.getAppointment().setStatus(AppointmentStatus.IN_CONSULTATION);
            appointmentRepository.save(nextPatient.getAppointment());
        }

        QueueEntry updated = queueEntryRepository.save(nextPatient);
        return QueueEntryResponse.fromEntity(updated, 0, 0);
    }

    @Transactional
    public QueueEntryResponse updateQueueStatus(Long id, QueueStatusUpdateRequest request) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QueueEntry", "id", id));

        if (entry.getStatus() == QueueStatus.COMPLETED) {
            throw new InvalidQueueOperationException("Cannot update status of a COMPLETED queue entry");
        }

        if (entry.getStatus() == QueueStatus.CANCELLED) {
            throw new InvalidQueueOperationException("Cannot update status of a CANCELLED queue entry");
        }

        QueueStatus newStatus = request.getStatus();
        entry.setStatus(newStatus);

        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            entry.setNotes(request.getNotes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == QueueStatus.IN_PROGRESS && entry.getCalledTime() == null) {
            entry.setCalledTime(now);
            if (entry.getAppointment() != null) {
                entry.getAppointment().setStatus(AppointmentStatus.IN_CONSULTATION);
                appointmentRepository.save(entry.getAppointment());
            }
        } else if (newStatus == QueueStatus.COMPLETED) {
            entry.setCompletedTime(now);
            if (entry.getAppointment() != null) {
                entry.getAppointment().setStatus(AppointmentStatus.COMPLETED);
                appointmentRepository.save(entry.getAppointment());
            }
        } else if (newStatus == QueueStatus.CANCELLED || newStatus == QueueStatus.NO_SHOW) {
            if (entry.getAppointment() != null && entry.getAppointment().getStatus() != AppointmentStatus.COMPLETED) {
                entry.getAppointment().setStatus(newStatus == QueueStatus.CANCELLED ?
                        AppointmentStatus.CANCELLED : AppointmentStatus.NO_SHOW);
                appointmentRepository.save(entry.getAppointment());
            }
        }

        QueueEntry updated = queueEntryRepository.save(entry);
        return QueueEntryResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public QueueSummaryResponse getQueueSummary(LocalDate date, Long doctorId) {
        LocalDate queryDate = date != null ? date : LocalDate.now();

        QueueSummaryResponse summary = new QueueSummaryResponse();
        summary.setQueueDate(queryDate);
        summary.setDoctorId(doctorId);

        if (doctorId != null) {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
            summary.setDoctorName(doctor.getName());
            summary.setTotalQueued(queueEntryRepository.countByQueueDateAndDoctorId(queryDate, doctorId));
            summary.setWaitingCount(queueEntryRepository.countByQueueDateAndDoctorIdAndStatus(queryDate, doctorId, QueueStatus.WAITING));
            summary.setInProgressCount(queueEntryRepository.countByQueueDateAndDoctorIdAndStatus(queryDate, doctorId, QueueStatus.IN_PROGRESS));
            summary.setCompletedCount(queueEntryRepository.countByQueueDateAndDoctorIdAndStatus(queryDate, doctorId, QueueStatus.COMPLETED));

            List<QueueEntry> entries = queueEntryRepository.findByQueueDateAndDoctorIdOrderBySequenceNumberAsc(queryDate, doctorId);
            populateSummaryLists(summary, entries);
        } else {
            summary.setDoctorName("All Doctors / General Queue");
            summary.setTotalQueued(queueEntryRepository.countByQueueDate(queryDate));
            summary.setWaitingCount(queueEntryRepository.countByQueueDateAndStatus(queryDate, QueueStatus.WAITING));
            summary.setInProgressCount(queueEntryRepository.countByQueueDateAndStatus(queryDate, QueueStatus.IN_PROGRESS));
            summary.setCompletedCount(queueEntryRepository.countByQueueDateAndStatus(queryDate, QueueStatus.COMPLETED));

            List<QueueEntry> entries = queueEntryRepository.findByQueueDateOrderBySequenceNumberAsc(queryDate);
            populateSummaryLists(summary, entries);
        }

        return summary;
    }

    private void populateSummaryLists(QueueSummaryResponse summary, List<QueueEntry> entries) {
        List<QueueEntryResponse> currentlyServing = new ArrayList<>();
        List<QueueEntryResponse> waitingList = new ArrayList<>();
        List<QueueEntryResponse> recentCompleted = new ArrayList<>();

        int position = 1;
        for (QueueEntry entry : entries) {
            if (entry.getStatus() == QueueStatus.IN_PROGRESS) {
                currentlyServing.add(QueueEntryResponse.fromEntity(entry, 0, 0));
            } else if (entry.getStatus() == QueueStatus.WAITING) {
                int waitTime = position * ESTIMATED_MINUTES_PER_PATIENT;
                waitingList.add(QueueEntryResponse.fromEntity(entry, position, waitTime));
                position++;
            } else if (entry.getStatus() == QueueStatus.COMPLETED) {
                recentCompleted.add(QueueEntryResponse.fromEntity(entry));
            }
        }

        summary.setCurrentlyServing(currentlyServing);
        summary.setWaitingList(waitingList);
        summary.setRecentCompleted(recentCompleted);
    }
}
