package com.example.booking.service;

import com.example.booking.dto.BookingRequest;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingService {

    private final List<Booking> store = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public BookingService() {
        // Khởi tạo một vài dữ liệu mẫu cho tương lai để dễ dàng kiểm thử giao diện
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        store.add(new Booking(
                sequence.getAndIncrement(),
                "A101",
                "Nguyen Van A",
                tomorrow,
                tomorrow.plusMinutes(90),
                "Họp đồ án tốt nghiệp",
                BookingStatus.CONFIRMED
        ));

        LocalDateTime nextDay = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        store.add(new Booking(
                sequence.getAndIncrement(),
                "B202",
                "Tran Thi B",
                nextDay,
                nextDay.plusMinutes(60),
                "Phỏng vấn ứng viên kỹ thuật",
                BookingStatus.CONFIRMED
        ));
    }

    public List<Booking> findAll() {
        return new ArrayList<>(store);
    }

    public Booking findById(Long id) {
        return store.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch đặt phòng với ID: " + id));
    }

    public Booking create(BookingRequest req) {
        Booking booking = new Booking(
                sequence.getAndIncrement(),
                req.getRoomName().trim(),
                req.getBookedBy().trim(),
                req.getStartAt(),
                req.getEndAt(),
                req.getPurpose().trim(),
                BookingStatus.CONFIRMED
        );
        store.add(booking);
        return booking;
    }

    public Booking update(Long id, BookingRequest req) {
        Booking booking = findById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Không thể chỉnh sửa lịch đặt phòng đã bị hủy.");
        }
        booking.setRoomName(req.getRoomName().trim());
        booking.setBookedBy(req.getBookedBy().trim());
        booking.setStartAt(req.getStartAt());
        booking.setEndAt(req.getEndAt());
        booking.setPurpose(req.getPurpose().trim());
        return booking;
    }

    public void cancel(Long id) {
        Booking booking = findById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Lịch đặt phòng này đã được hủy trước đó.");
        }

        LocalDateTime now = LocalDateTime.now();
        // Kiểm tra quy tắc: Chỉ được hủy khi còn ít nhất 30 phút trước thời gian bắt đầu
        if (now.isAfter(booking.getStartAt().minusMinutes(30))) {
            throw new IllegalStateException("Không thể hủy lịch đặt phòng: Chỉ được phép hủy trước giờ bắt đầu ít nhất 30 phút.");
        }

        // Đổi trạng thái sang CANCELLED, KHÔNG xóa bản ghi
        booking.setStatus(BookingStatus.CANCELLED);
    }

    public boolean hasRoomOverlap(String roomName, LocalDateTime startAt, LocalDateTime endAt, Long currentBookingId) {
        if (roomName == null || startAt == null || endAt == null) {
            return false;
        }
        return store.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> currentBookingId == null || !b.getId().equals(currentBookingId))
                .filter(b -> b.getRoomName().equalsIgnoreCase(roomName.trim()))
                .anyMatch(b -> startAt.isBefore(b.getEndAt()) && endAt.isAfter(b.getStartAt()));
    }

    public long countActiveBookingsByPerson(String bookedBy, Long currentBookingId) {
        if (bookedBy == null) {
            return 0;
        }
        return store.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> currentBookingId == null || !b.getId().equals(currentBookingId))
                .filter(b -> b.getBookedBy().equalsIgnoreCase(bookedBy.trim()))
                .count();
    }
}
