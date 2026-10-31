package com.carehub.carehub.repository;

import com.carehub.carehub.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient_PatientIdOrderByAppointmentDateTimeDesc(Long patientId);
    List<Appointment> findByDoctor_DoctorIdOrderByAppointmentDateTimeDesc(Long doctorId);
    List<Appointment> findByStatusOrderByAppointmentDateTimeDesc(String status);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDateTime BETWEEN :start AND :end ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findAppointmentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDateTime BETWEEN :start AND :end")
    long countAppointmentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT FUNCTION('DATE_FORMAT', a.appointmentDateTime, '%Y-%m') AS ym, COUNT(a) " +
           "FROM Appointment a WHERE a.appointmentDateTime >= :since GROUP BY ym ORDER BY ym")
    List<Object[]> monthlyAppointmentsSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.status, COUNT(a) FROM Appointment a GROUP BY a.status")
    List<Object[]> countGroupByStatus();
}
