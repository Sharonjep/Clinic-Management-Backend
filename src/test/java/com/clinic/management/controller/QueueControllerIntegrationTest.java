package com.clinic.management.controller;

import com.clinic.management.dto.queue.CheckInRequest;
import com.clinic.management.dto.queue.QueueStatusUpdateRequest;
import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Patient;
import com.clinic.management.entity.QueueEntry;
import com.clinic.management.entity.QueueStatus;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.PatientRepository;
import com.clinic.management.repository.QueueEntryRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private com.clinic.management.repository.AppointmentRepository appointmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Patient savedPatient;
    private Doctor savedDoctor;

    @BeforeEach
    void setUp() {
        queueEntryRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();

        Patient patient = new Patient("PAT-2026-0005", "Lucas", "Scott", LocalDate.of(1993, 3, 15), "Male", "+1-555-0401", "lucas@test.com");
        savedPatient = patientRepository.save(patient);

        Doctor doctor = new Doctor("Dr. Meredith Grey", "General Surgery", "Room 301", "+1-555-0402", "grey@clinic.com");
        savedDoctor = doctorRepository.save(doctor);
    }

    @Test
    @DisplayName("GET /api/v1/queues/display - Public unauthenticated access for waiting room display")
    void testGetDisplayBoard_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/queues/display"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.queueDate", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/queues/check-in - Receptionist checks in patient and gets queue number")
    @WithMockUser(username = "reception", roles = {"RECEPTIONIST"})
    void testCheckInPatient_Success() throws Exception {
        CheckInRequest request = new CheckInRequest(savedPatient.getId(), savedDoctor.getId(), null, 0, "Walk-in checkup");

        mockMvc.perform(post("/api/v1/queues/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.queueNumber", is("Q-001")))
                .andExpect(jsonPath("$.data.status", is("WAITING")))
                .andExpect(jsonPath("$.data.positionInQueue", is(1)))
                .andExpect(jsonPath("$.data.estimatedWaitMinutes", is(15)));
    }

    @Test
    @DisplayName("POST /api/v1/queues/call-next - Doctor calls next patient into room")
    @WithMockUser(username = "dr_grey", roles = {"DOCTOR"})
    void testCallNextPatient_Success() throws Exception {
        // Pre-seed a waiting patient
        QueueEntry waiting = new QueueEntry("Q-001", LocalDate.now(), 1, savedPatient, savedDoctor, null);
        queueEntryRepository.save(waiting);

        mockMvc.perform(post("/api/v1/queues/call-next")
                        .param("doctorId", savedDoctor.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.queueNumber", is("Q-001")))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.data.calledTime", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/v1/queues/{id}/status - Update patient queue status to COMPLETED")
    @WithMockUser(username = "dr_grey", roles = {"DOCTOR"})
    void testUpdateQueueStatus_Complete() throws Exception {
        QueueEntry inProgress = new QueueEntry("Q-001", LocalDate.now(), 1, savedPatient, savedDoctor, null);
        inProgress.setStatus(QueueStatus.IN_PROGRESS);
        QueueEntry saved = queueEntryRepository.save(inProgress);

        QueueStatusUpdateRequest request = new QueueStatusUpdateRequest(QueueStatus.COMPLETED, "Prescribed antibiotics");

        mockMvc.perform(patch("/api/v1/queues/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.completedTime", notNullValue()));
    }
}
