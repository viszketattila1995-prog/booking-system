package com.attila.bookingsystem.controller;

import com.attila.bookingsystem.dto.booking.BookingResponse;
import com.attila.bookingsystem.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/api/time-slots/{timeSlotId}/book")
    public ResponseEntity<BookingResponse> book(@PathVariable UUID timeSlotId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.book(timeSlotId));
    }

    @GetMapping("/api/bookings/me")
    public List<BookingResponse> listMine() {
        return bookingService.listMine();
    }

    // Nincs role-restrikció: vagy a vendég (saját foglalása), vagy a provider
    // (saját idősávjához tartozó foglalás) mondhatja le - ezt a service réteg
    // ownership-checkje dönti el, nem egy statikus @PreAuthorize szerepkör-szabály.
    @DeleteMapping("/api/bookings/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        bookingService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
