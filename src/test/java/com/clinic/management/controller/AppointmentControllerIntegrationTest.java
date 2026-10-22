package com.clinic.management.controller;

import com.clinic.management.dto.appointment.AppointmentRequest;
import com.clinic.management.dto.appointment.RescheduleRequest;
import com.clinic.management.entity.Appointment;
import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Patient;
import com.clinic.management.repository.AppointmentRepository;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.clinic.management.repository.QueueEntryRepository queueEntryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Patient savedPatient;
    private Doctor savedDoctor;
    private LocalDateTime validFutureTime;

    @BeforeEach
    void setUp() {
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();

        Patient patient = new Patient("PAT-2026-0001", "Emma", "Stone", LocalDate.of(1988, 11, 6), "Female", "+1-555-0301", "emma@test.com");
        savedPatient = patientRepository.save(patient);

        Doctor doctor = new Doctor("Dr. John Watson", "Internal Medicine", "Room 201", "+1-555-0302", "watson@clinic.com");
        savedDoctor = doctorRepository.save(doctor);

        validFutureTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("GET /api/v1/appointments - 401 Unauthorized when unauthenticated")
    void testGetAppointments_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/appointments - 400 Bad Request when validation fails (e.g. past date)")
    @WithMockUser(username = "reception", roles = {"RECEPTIONIST"})
    void testCreateAppointment_PastDate_ValidationFailure() throws Exception {
        AppointmentRequest invalidRequest = new AppointmentRequest(
                savedPatient.getId(),
                savedDoctor.getId(),
                LocalDateTime.now().minusDays(1), // Past date violates @Future
                30,
                "Checkup"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.validationErrors.appointmentDateTime", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/appointments - 201 Created on valid booking")
    @WithMockUser(username = "reception", roles = {"RECEPTIONIST"})
    void testCreateAppointment_Success() throws Exception {
        AppointmentRequest request = new AppointmentRequest(
                savedPatient.getId(),
                savedDoctor.getId(),
                validFutureTime,
                30,
                "Consultation for hypertension"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.doctorName", is("Dr. John Watson")))
                .andExpect(jsonPath("$.data.patientName", is("Emma Stone")))
                .andExpect(jsonPath("$.data.status", is("SCHEDULED")));
    }

    @Test
    @DisplayName("POST /api/v1/appointments - 409 Conflict when double booking the same doctor")
    @WithMockUser(username = "reception", roles = {"RECEPTIONIST"})
    void testCreateAppointment_DoubleBookingConflict() throws Exception {
        // Create initial booking from 14:00 to 14:30
        Appointment existing = new Appointment(savedPatient, savedDoctor, validFutureTime, validFutureTime.plusMinutes(30), "First slot");
        appointmentRepository.save(existing);

        // Attempt overlapping booking with another patient for the same doctor at 14:15
        Patient anotherPatient = patientRepository.save(
                new Patient("PAT-2026-0002", "Tom", "Hanks", LocalDate.of(1956, 7, 9), "Male", "+1-555-0303", "tom@test.com"));

        AppointmentRequest conflictingRequest = new AppointmentRequest(
                anotherPatient.getId(),
                savedDoctor.getId(),
                validFutureTime.plusMinutes(15), // Overlaps [14:00, 14:30)
                30,
                "Conflicting booking"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("is unavailable. Conflicting appointment")));
    }

    @Test
    @DisplayName("POST /api/v1/appointments/{id}/reschedule - 200 OK when moving to open time slot")
    @WithMockUser(username = "reception", roles = {"RECEPTIONIST"})
    void testRescheduleAppointment_Success() throws Exception {
        Appointment existing = new Appointment(savedPatient, savedDoctor, validFutureTime, validFutureTime.plusMinutes(30), "Initial slot");
        Appointment saved = appointmentRepository.save(existing);

        LocalDateTime newTime = validFutureTime.plusHours(2);
        RescheduleRequest reschedule = new RescheduleRequest(newTime, 30, "Shifted two hours later");

        mockMvc.perform(post("/api/v1/appointments/" + saved.getId() + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.appointmentDateTime", startsWith(newTime.toString().substring(0, 16))));
    }
}
