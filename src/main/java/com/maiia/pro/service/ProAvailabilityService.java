package com.maiia.pro.service;

import com.maiia.pro.dto.AvailabilityDTO;
import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.TimeSlot;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProAvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }

    public List<AvailabilityDTO> generateAvailabilities(Integer practitionerId) {
        List<TimeSlot> allTimeSlots = timeSlotRepository.findByPractitionerId(practitionerId);
        List<Appointment> bookedAppointments = appointmentRepository.findByPractitionerId(practitionerId);
        Duration availabilityDuration = Duration.ofMinutes(15);

        return allTimeSlots.stream()
            .flatMap(slot -> splitTimeSlotIntoAvailabilities(slot, availabilityDuration, bookedAppointments, practitionerId).stream())
            .collect(Collectors.toList());
    }

    private List<AvailabilityDTO> splitTimeSlotIntoAvailabilities(TimeSlot slot, Duration duration,
        List<Appointment> bookedAppointments,
        Integer practitionerId) {
        List<AvailabilityDTO> availabilities = new ArrayList<>();
        var slotStartDate = slot.getStartDate();
        var slotEndDate = slot.getEndDate();

        while (slotStartDate.isBefore(slotEndDate)) {
            var currentStartDate = slotStartDate;
            var currentEndDate = slotStartDate.plus(duration);


            Optional<Appointment> overlappingAppointment = bookedAppointments.stream()
                .filter(appointment -> overlaps(appointment.getStartDate(), appointment.getEndDate(), currentStartDate, currentEndDate))
                .findFirst();

            if (overlappingAppointment.isPresent()) {
                slotStartDate = overlappingAppointment.get().getEndDate();
            } else {
                availabilities.add(AvailabilityDTO.builder()
                    .practitionerId(practitionerId)
                    .startDate(currentStartDate)
                    .endDate(currentEndDate)
                    .build());

                slotStartDate = currentEndDate;
            }
        }
        return availabilities;
    }

    private boolean overlaps(LocalDateTime startDate1, LocalDateTime endDate1,
        LocalDateTime startDate2, LocalDateTime endDate2) {
        return startDate1.isBefore(endDate2) && startDate2.isBefore(endDate1);
    }
}
