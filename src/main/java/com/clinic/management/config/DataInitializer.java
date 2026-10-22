package com.clinic.management.config;

import com.clinic.management.entity.*;
import com.clinic.management.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${clinic.initial-seed:true}")
    private boolean seedEnabled;

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository,
                           AppointmentRepository appointmentRepository,
                           QueueEntryRepository queueEntryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            logger.info("Data seeding skipped (seedEnabled={}, userCount={})", seedEnabled, userRepository.count());
            return;
        }

        logger.info("Seeding initial fictional clinic data for development and demonstration...");

        // 1. Create Staff Accounts
        User adminUser = new User("admin", "admin@clinic.com", passwordEncoder.encode("Admin@123"), "System Administrator", Role.ROLE_ADMIN);
        User receptionUser = new User("reception", "reception@clinic.com", passwordEncoder.encode("Reception@123"), "Clara Vance (Reception)", Role.ROLE_RECEPTIONIST);
        User drSmithUser = new User("dr.smith", "dr.smith@clinic.com", passwordEncoder.encode("Doctor@123"), "Dr. Sarah Smith", Role.ROLE_DOCTOR);
        User drJonesUser = new User("dr.jones", "dr.jones@clinic.com", passwordEncoder.encode("Doctor@123"), "Dr. Robert Jones", Role.ROLE_DOCTOR);

        userRepository.save(adminUser);
        userRepository.save(receptionUser);
        userRepository.save(drSmithUser);
        userRepository.save(drJonesUser);

        // 2. Create Doctors
        Doctor drSmith = new Doctor("Dr. Sarah Smith", "Cardiology", "Room 101", "+1-555-0201", "dr.smith@clinic.com");
        drSmith.setUser(drSmithUser);

        Doctor drJones = new Doctor("Dr. Robert Jones", "General Medicine", "Room 102", "+1-555-0202", "dr.jones@clinic.com");
        drJones.setUser(drJonesUser);

        Doctor drChen = new Doctor("Dr. Emily Chen", "Pediatrics", "Room 103", "+1-555-0203", "dr.chen@clinic.com");

        drSmith = doctorRepository.save(drSmith);
        drJones = doctorRepository.save(drJones);
        drChen = doctorRepository.save(drChen);

        // 3. Create Fictional Patients
        Patient pat1 = new Patient("PAT-2026-0001", "John", "Doe", LocalDate.of(1985, 4, 12), "Male", "+1-555-0101", "john.doe@example.com");
        pat1.setAddress("124 Elm Street, Springfield");
        pat1.setEmergencyContact("Mary Doe (+1-555-0199)");
        pat1.setMedicalNotes("Mild hypertension; allergic to Penicillin.");

        Patient pat2 = new Patient("PAT-2026-0002", "Jane", "Miller", LocalDate.of(1992, 9, 21), "Female", "+1-555-0102", "jane.miller@example.com");
        pat2.setAddress("45 Maple Avenue, Springfield");
        pat2.setEmergencyContact("Peter Miller (+1-555-0198)");

        Patient pat3 = new Patient("PAT-2026-0003", "Alice", "Cooper", LocalDate.of(1978, 11, 5), "Female", "+1-555-0103", "alice.cooper@example.com");
        pat3.setAddress("78 Oak Lane, Springfield");

        Patient pat4 = new Patient("PAT-2026-0004", "David", "Wilson", LocalDate.of(2001, 2, 18), "Male", "+1-555-0104", "david.wilson@example.com");

        pat1 = patientRepository.save(pat1);
        pat2 = patientRepository.save(pat2);
        pat3 = patientRepository.save(pat3);
        pat4 = patientRepository.save(pat4);

        // 4. Create Sample Appointments
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        Appointment app1 = new Appointment(pat1, drSmith, tomorrow.withHour(10), tomorrow.withHour(10).plusMinutes(30), "Routine Cardiology Follow-up");
        Appointment app2 = new Appointment(pat2, drJones, tomorrow.withHour(11), tomorrow.withHour(11).plusMinutes(30), "Annual General Health Checkup");

        appointmentRepository.save(app1);
        appointmentRepository.save(app2);

        // 5. Seed Today's Queue
        LocalDate today = LocalDate.now();

        QueueEntry q1 = new QueueEntry("Q-001", today, 1, pat1, drSmith, null);
        q1.setStatus(QueueStatus.IN_PROGRESS);
        q1.setCalledTime(LocalDateTime.now().minusMinutes(10));

        QueueEntry q2 = new QueueEntry("Q-002", today, 2, pat2, drSmith, null);
        q2.setStatus(QueueStatus.WAITING);

        QueueEntry q3 = new QueueEntry("Q-003", today, 3, pat3, drJones, null);
        q3.setStatus(QueueStatus.WAITING);

        queueEntryRepository.save(q1);
        queueEntryRepository.save(q2);
        queueEntryRepository.save(q3);

        logger.info("Fictional data seeded successfully: 4 Users, 3 Doctors, 4 Patients, 2 Appointments, 3 Active Queue Tickets.");
    }
}
