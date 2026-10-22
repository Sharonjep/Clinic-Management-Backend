package com.clinic.management.dto.doctor;

import com.clinic.management.entity.Doctor;

public class DoctorResponse {

    private Long id;
    private String name;
    private String specialization;
    private String consultationRoom;
    private String phone;
    private String email;
    private boolean active;
    private Long userId;

    public DoctorResponse() {
    }

    public static DoctorResponse fromEntity(Doctor doctor) {
        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());
        response.setName(doctor.getName());
        response.setSpecialization(doctor.getSpecialization());
        response.setConsultationRoom(doctor.getConsultationRoom());
        response.setPhone(doctor.getPhone());
        response.setEmail(doctor.getEmail());
        response.setActive(doctor.isActive());
        if (doctor.getUser() != null) {
            response.setUserId(doctor.getUser().getId());
        }
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getConsultationRoom() {
        return consultationRoom;
    }

    public void setConsultationRoom(String consultationRoom) {
        this.consultationRoom = consultationRoom;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
