package com.carehub.carehub.service;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Appointment;
import com.carehub.carehub.entity.Doctor;
import com.carehub.carehub.entity.Patient;
import com.carehub.carehub.repository.AppointmentRepository;
import com.carehub.carehub.repository.DoctorRepository;
import com.carehub.carehub.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Appointment> getAll() {
        return appointmentRepository.findAll();
    }

    public Appointment getById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id " + id));
    }

    public List<Appointment> getByPatient(Long patientId) {
        return appointmentRepository.findByPatient_PatientIdOrderByAppointmentDateTimeDesc(patientId);
    }

    public List<Appointment> getByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctor_DoctorIdOrderByAppointmentDateTimeDesc(doctorId);
    }

    public List<Appointment> getTodayAppointments() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return appointmentRepository.findAppointmentsBetween(startOfDay, endOfDay);
    }

    public Appointment create(Appointment appointment) {
        if (appointment.getPatient() == null || appointment.getPatient().getPatientId() == null) {
            throw new IllegalArgumentException("Patient is required for appointment");
        }
        if (appointment.getDoctor() == null || appointment.getDoctor().getDoctorId() == null) {
            throw new IllegalArgumentException("Doctor is required for appointment");
        }
        if (appointment.getAppointmentDateTime() == null) {
            throw new IllegalArgumentException("Appointment date and time is required");
        }

        Patient patient = patientRepository.findById(appointment.getPatient().getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Doctor doctor = doctorRepository.findById(appointment.getDoctor().getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        if (appointment.getStatus() == null) {
            appointment.setStatus("Scheduled");
        }

        Appointment saved = appointmentRepository.save(appointment);
        auditLogService.log("CREATE", "Appointment",
                "Appointment booked for " + patient.getName() + " with Dr. " + doctor.getName() + " on " + appointment.getAppointmentDateTime(),
                String.valueOf(saved.getAppointmentId()));
        return saved;
    }

    public Appointment update(Long id, Appointment updated) {
        Appointment existing = getById(id);

        if (updated.getPatient() != null && updated.getPatient().getPatientId() != null) {
            existing.setPatient(patientRepository.findById(updated.getPatient().getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found")));
        }
        if (updated.getDoctor() != null && updated.getDoctor().getDoctorId() != null) {
            existing.setDoctor(doctorRepository.findById(updated.getDoctor().getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found")));
        }
        if (updated.getAppointmentDateTime() != null) {
            existing.setAppointmentDateTime(updated.getAppointmentDateTime());
        }
        if (updated.getReason() != null) {
            existing.setReason(updated.getReason());
        }
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getNotes() != null) {
            existing.setNotes(updated.getNotes());
        }

        Appointment saved = appointmentRepository.save(existing);
        auditLogService.log("UPDATE", "Appointment", "Appointment updated: ID " + id + ", Status: " + saved.getStatus(), String.valueOf(id));
        return saved;
    }

    public Appointment updateStatus(Long id, String status) {
        Appointment existing = getById(id);
        existing.setStatus(status);
        Appointment saved = appointmentRepository.save(existing);
        auditLogService.log("UPDATE", "Appointment", "Appointment status changed to " + status + " for ID " + id, String.valueOf(id));
        return saved;
    }

    public void delete(Long id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Appointment not found with id " + id);
        }
        appointmentRepository.deleteById(id);
        auditLogService.log("DELETE", "Appointment", "Appointment deleted with ID " + id, String.valueOf(id));
    }

    public Map<String, Object> getAppointmentStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", appointmentRepository.countAppointmentsBetween(startOfDay, endOfDay));
        stats.put("scheduledCount", appointmentRepository.countByStatus("Scheduled"));
        stats.put("confirmedCount", appointmentRepository.countByStatus("Confirmed"));
        stats.put("completedCount", appointmentRepository.countByStatus("Completed"));
        stats.put("cancelledCount", appointmentRepository.countByStatus("Cancelled"));
        stats.put("totalCount", appointmentRepository.count());
        return stats;
    }
}
