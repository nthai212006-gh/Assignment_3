package com.example.booking.dto;

import com.example.booking.model.Booking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class BookingRequest {

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 50, message = "Tên phòng tối đa 50 ký tự")
    private String roomName;

    @NotBlank(message = "Người đặt không được để trống")
    @Size(max = 100, message = "Tên người đặt tối đa 100 ký tự")
    private String bookedBy;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @NotBlank(message = "Mục đích sử dụng không được để trống")
    @Size(max = 500, message = "Mục đích tối đa 500 ký tự")
    private String purpose;

    public BookingRequest() {
    }

    public static BookingRequest from(Booking booking) {
        BookingRequest req = new BookingRequest();
        req.setRoomName(booking.getRoomName());
        req.setBookedBy(booking.getBookedBy());
        req.setStartAt(booking.getStartAt());
        req.setEndAt(booking.getEndAt());
        req.setPurpose(booking.getPurpose());
        return req;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(String bookedBy) {
        this.bookedBy = bookedBy;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
