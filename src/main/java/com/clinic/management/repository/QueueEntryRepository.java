package com.clinic.management.repository;

import com.clinic.management.entity.QueueEntry;
import com.clinic.management.entity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    List<QueueEntry> findByQueueDateOrderBySequenceNumberAsc(LocalDate date);

    List<QueueEntry> findByQueueDateAndDoctorIdOrderBySequenceNumberAsc(LocalDate date, Long doctorId);

    List<QueueEntry> findByQueueDateAndStatusOrderBySequenceNumberAsc(LocalDate date, QueueStatus status);

    List<QueueEntry> findByQueueDateAndDoctorIdAndStatusOrderBySequenceNumberAsc(
            LocalDate date, Long doctorId, QueueStatus status);

    Optional<QueueEntry> findTopByQueueDateOrderBySequenceNumberDesc(LocalDate date);

    List<QueueEntry> findByPatientIdAndQueueDateAndStatusIn(
            Long patientId, LocalDate date, List<QueueStatus> statuses);

    Optional<QueueEntry> findFirstByQueueDateAndDoctorIdAndStatusOrderByPriorityDescSequenceNumberAsc(
            LocalDate date, Long doctorId, QueueStatus status);

    Optional<QueueEntry> findFirstByQueueDateAndStatusOrderByPriorityDescSequenceNumberAsc(
            LocalDate date, QueueStatus status);

    long countByQueueDateAndStatus(LocalDate date, QueueStatus status);

    long countByQueueDateAndDoctorIdAndStatus(LocalDate date, Long doctorId, QueueStatus status);

    long countByQueueDate(LocalDate date);

    long countByQueueDateAndDoctorId(LocalDate date, Long doctorId);
}
