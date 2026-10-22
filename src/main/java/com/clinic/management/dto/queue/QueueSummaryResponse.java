package com.clinic.management.dto.queue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class QueueSummaryResponse {

    private LocalDate queueDate;
    private Long doctorId;
    private String doctorName;
    private long totalQueued;
    private long waitingCount;
    private long inProgressCount;
    private long completedCount;
    private List<QueueEntryResponse> currentlyServing = new ArrayList<>();
    private List<QueueEntryResponse> waitingList = new ArrayList<>();
    private List<QueueEntryResponse> recentCompleted = new ArrayList<>();

    public QueueSummaryResponse() {
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
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

    public long getTotalQueued() {
        return totalQueued;
    }

    public void setTotalQueued(long totalQueued) {
        this.totalQueued = totalQueued;
    }

    public long getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(long waitingCount) {
        this.waitingCount = waitingCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public List<QueueEntryResponse> getCurrentlyServing() {
        return currentlyServing;
    }

    public void setCurrentlyServing(List<QueueEntryResponse> currentlyServing) {
        this.currentlyServing = currentlyServing;
    }

    public List<QueueEntryResponse> getWaitingList() {
        return waitingList;
    }

    public void setWaitingList(List<QueueEntryResponse> waitingList) {
        this.waitingList = waitingList;
    }

    public List<QueueEntryResponse> getRecentCompleted() {
        return recentCompleted;
    }

    public void setRecentCompleted(List<QueueEntryResponse> recentCompleted) {
        this.recentCompleted = recentCompleted;
    }
}
