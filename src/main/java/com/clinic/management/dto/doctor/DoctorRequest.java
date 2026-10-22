package com.clinic.management.dto.doctor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DoctorRequest {

    @NotBlank(message = "Doctor name is required")
    @Size(max = 100, message = "Doctor name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    private String specialization;

    private String consultationRoom;
    private String phone;
    private String email;
    private Long userId;

    public DoctorRequest() {
    }

    public DoctorRequest(String name, String specialization, String consultationRoom, String phone, String email) {
        this.name = name;
        this.specialization = specialization;
        this.consultationRoom = consultationRoom;
        this.phone = phone;
        this.email = email;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
