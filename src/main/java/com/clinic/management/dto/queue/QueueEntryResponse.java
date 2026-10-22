package com.clinic.management.dto.queue;

import com.clinic.management.entity.QueueEntry;
import com.clinic.management.entity.QueueStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueueEntryResponse {

    private Long id;
    private String queueNumber;
    private LocalDate queueDate;
    private int sequenceNumber;
    private Integer positionInQueue;
    private Integer estimatedWaitMinutes;

    private Long patientId;
    private String patientIdentifier;
    private String patientName;
    private String patientPhone;

    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String consultationRoom;

    private Long appointmentId;
    private QueueStatus status;
    private int priority;
    private LocalDateTime checkInTime;
    private LocalDateTime calledTime;
    private LocalDateTime completedTime;
    private String notes;

    public QueueEntryResponse() {
    }

    public static QueueEntryResponse fromEntity(QueueEntry entry) {
        return fromEntity(entry, null, null);
    }

    public static QueueEntryResponse fromEntity(QueueEntry entry, Integer position, Integer estimatedWaitMinutes) {
        QueueEntryResponse response = new QueueEntryResponse();
        response.setId(entry.getId());
        response.setQueueNumber(entry.getQueueNumber());
        response.setQueueDate(entry.getQueueDate());
        response.setSequenceNumber(entry.getSequenceNumber());
        response.setPositionInQueue(position);
        response.setEstimatedWaitMinutes(estimatedWaitMinutes);

        if (entry.getPatient() != null) {
            response.setPatientId(entry.getPatient().getId());
            response.setPatientIdentifier(entry.getPatient().getPatientIdentifier());
            response.setPatientName(entry.getPatient().getFullName());
            response.setPatientPhone(entry.getPatient().getPhone());
        }

        if (entry.getDoctor() != null) {
            response.setDoctorId(entry.getDoctor().getId());
            response.setDoctorName(entry.getDoctor().getName());
            response.setDoctorSpecialization(entry.getDoctor().getSpecialization());
            response.setConsultationRoom(entry.getDoctor().getConsultationRoom());
        }

        if (entry.getAppointment() != null) {
            response.setAppointmentId(entry.getAppointment().getId());
        }

        response.setStatus(entry.getStatus());
        response.setPriority(entry.getPriority());
        response.setCheckInTime(entry.getCheckInTime());
        response.setCalledTime(entry.getCalledTime());
        response.setCompletedTime(entry.getCompletedTime());
        response.setNotes(entry.getNotes());

        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(String queueNumber) {
        this.queueNumber = queueNumber;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getPositionInQueue() {
        return positionInQueue;
    }

    public void setPositionInQueue(Integer positionInQueue) {
        this.positionInQueue = positionInQueue;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorSpecialization() {
        return doctorSpecialization;
    }

    public void setDoctorSpecialization(String doctorSpecialization) {
        this.doctorSpecialization = doctorSpecialization;
    }

    public String getConsultationRoom() {
        return consultationRoom;
    }

    public void setConsultationRoom(String consultationRoom) {
        this.consultationRoom = consultationRoom;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCalledTime() {
        return calledTime;
    }

    public void setCalledTime(LocalDateTime calledTime) {
        this.calledTime = calledTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
