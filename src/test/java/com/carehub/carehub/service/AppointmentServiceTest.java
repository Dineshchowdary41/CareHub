package com.carehub.carehub.service;

import com.carehub.carehub.entity.Appointment;
import com.carehub.carehub.entity.Doctor;
import com.carehub.carehub.entity.Patient;
import com.carehub.carehub.repository.AppointmentRepository;
import com.carehub.carehub.repository.DoctorRepository;
import com.carehub.carehub.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient testPatient;
    private Doctor testDoctor;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        testPatient = new Patient();
        testPatient.setPatientId(1L);
        testPatient.setName("John Doe");

        testDoctor = new Doctor();
        testDoctor.setDoctorId(2L);
        testDoctor.setName("Dr. Sarah Smith");
        testDoctor.setSpecialization("Cardiology");

        testAppointment = new Appointment();
        testAppointment.setAppointmentId(100L);
        testAppointment.setPatient(testPatient);
        testAppointment.setDoctor(testDoctor);
        testAppointment.setAppointmentDateTime(LocalDateTime.now().plusDays(1));
        testAppointment.setReason("General Checkup");
        testAppointment.setStatus("Scheduled");
    }

    @Test
    @DisplayName("Should successfully book new appointment and create audit log")
    void testBookAppointment() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(testDoctor));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> {
            Appointment a = i.getArgument(0);
            a.setAppointmentId(101L);
            return a;
        });

        Appointment created = appointmentService.create(testAppointment);

        assertNotNull(created);
        assertEquals("Scheduled", created.getStatus());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(auditLogService, times(1)).log(eq("CREATE"), eq("Appointment"), any(), any());
    }

    @Test
    @DisplayName("Should transition status and record status change")
    void testUpdateStatus() {
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment updated = appointmentService.updateStatus(100L, "Confirmed");

        assertEquals("Confirmed", updated.getStatus());
        verify(auditLogService, times(1)).log(eq("UPDATE"), eq("Appointment"), any(), any());
    }

    @Test
    @DisplayName("Should calculate appointment stats correctly")
    void testGetStats() {
        when(appointmentRepository.countByStatus("Scheduled")).thenReturn(5L);
        when(appointmentRepository.countByStatus("Confirmed")).thenReturn(3L);
        when(appointmentRepository.countByStatus("Completed")).thenReturn(10L);
        when(appointmentRepository.countByStatus("Cancelled")).thenReturn(2L);
        when(appointmentRepository.count()).thenReturn(20L);

        Map<String, Object> stats = appointmentService.getAppointmentStats();

        assertEquals(5L, stats.get("scheduledCount"));
        assertEquals(3L, stats.get("confirmedCount"));
        assertEquals(10L, stats.get("completedCount"));
        assertEquals(2L, stats.get("cancelledCount"));
        assertEquals(20L, stats.get("totalCount"));
    }
}
