package com.example.booking;

import com.example.booking.dto.BookingRequest;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import com.example.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingPolicyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("TC1: Submit form rỗng phải hiển thị lỗi validation cạnh từng trường và không chuyển trang")
    void testEmptyFormSubmission() throws Exception {
        mockMvc.perform(post("/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeHasFieldErrors("booking", "roomName", "bookedBy", "startAt", "endAt", "purpose"))
                .andExpect(content().string(containsString("Tên phòng không được để trống")))
                .andExpect(content().string(containsString("Người đặt không được để trống")))
                .andExpect(content().string(containsString("Thời gian bắt đầu không được để trống")))
                .andExpect(content().string(containsString("Thời gian kết thúc không được để trống")))
                .andExpect(content().string(containsString("Mục đích sử dụng không được để trống")));
    }

    @Test
    @DisplayName("TC2: endAt trước hoặc bằng startAt phải báo lỗi cạnh trường endAt")
    void testEndAtBeforeStartAt() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0);
        LocalDateTime end = start.minusMinutes(30);

        mockMvc.perform(post("/bookings")
                        .param("roomName", "C303")
                        .param("bookedBy", "Tester A")
                        .param("startAt", start.toString())
                        .param("endAt", end.toString())
                        .param("purpose", "Test end before start"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeHasFieldErrorCode("booking", "endAt", "error.endAt"))
                .andExpect(content().string(containsString("Thời gian kết thúc phải sau thời gian bắt đầu")));
    }

    @Test
    @DisplayName("TC3: Thời lượng quá 120 phút phải báo lỗi cạnh trường endAt")
    void testDurationOver120Minutes() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(150); // 150 phút > 120 phút

        mockMvc.perform(post("/bookings")
                        .param("roomName", "C303")
                        .param("bookedBy", "Tester B")
                        .param("startAt", start.toString())
                        .param("endAt", end.toString())
                        .param("purpose", "Test duration over 120 mins"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeHasFieldErrorCode("booking", "endAt", "error.endAt"))
                .andExpect(content().string(containsString("Thời gian đặt phòng tối đa không được vượt quá 120 phút")));
    }

    @Test
    @DisplayName("TC4: Thời gian bắt đầu ở quá khứ phải báo lỗi cạnh trường startAt")
    void testStartInThePast() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = start.plusMinutes(60);

        mockMvc.perform(post("/bookings")
                        .param("roomName", "C303")
                        .param("bookedBy", "Tester C")
                        .param("startAt", start.toString())
                        .param("endAt", end.toString())
                        .param("purpose", "Test past start"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeHasFieldErrorCode("booking", "startAt", "error.startAt"))
                .andExpect(content().string(containsString("Thời gian bắt đầu không được ở trong quá khứ")));
    }

    @Test
    @DisplayName("TC5: Trùng giờ cùng một phòng (same room overlap) phải báo lỗi cạnh trường roomName")
    void testRoomOverlapRejected() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(8).withMinute(0);
        LocalDateTime end = start.plusMinutes(60);

        // Tạo booking 1 thành công
        BookingRequest req1 = new BookingRequest();
        req1.setRoomName("D404");
        req1.setBookedBy("User D1");
        req1.setStartAt(start);
        req1.setEndAt(end);
        req1.setPurpose("First booking");
        bookingService.create(req1);

        // Thử tạo booking 2 cùng phòng D404 nhưng trùng khoảng thời gian (start + 30m -> start + 90m)
        LocalDateTime start2 = start.plusMinutes(30);
        LocalDateTime end2 = start2.plusMinutes(60);

        mockMvc.perform(post("/bookings")
                        .param("roomName", "D404")
                        .param("bookedBy", "User D2")
                        .param("startAt", start2.toString())
                        .param("endAt", end2.toString())
                        .param("purpose", "Overlapping booking"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeHasFieldErrorCode("booking", "roomName", "error.roomName"))
                .andExpect(content().string(containsString("Phòng D404 đã có người đặt trong khoảng thời gian này")));
    }

    @Test
    @DisplayName("TC6: Trùng giờ nhưng khác phòng (different room same time) được phép thành công")
    void testDifferentRoomSameTimeAllowed() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(11).withHour(8).withMinute(0);
        LocalDateTime end = start.plusMinutes(60);

        // Tạo phòng D404
        BookingRequest req1 = new BookingRequest();
        req1.setRoomName("D404");
        req1.setBookedBy("User E1");
        req1.setStartAt(start);
        req1.setEndAt(end);
        req1.setPurpose("Booking Room D404");
        bookingService.create(req1);

        // Đặt phòng E505 cùng khung giờ -> phải redirect thành công
        mockMvc.perform(post("/bookings")
                        .param("roomName", "E505")
                        .param("bookedBy", "User E2")
                        .param("startAt", start.toString())
                        .param("endAt", end.toString())
                        .param("purpose", "Booking Room E505"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attribute("successMessage", "Đặt phòng thành công!"));
    }

    @Test
    @DisplayName("TC7: Một người đặt lịch thứ 3 khi đã giữ 2 booking CONFIRMED phải bị từ chối")
    void testMaxTwoConfirmedBookingsPerPerson() throws Exception {
        String student = "SV_TEST_LIMIT";
        LocalDateTime day1 = LocalDateTime.now().plusDays(15).withHour(9).withMinute(0);
        LocalDateTime day2 = LocalDateTime.now().plusDays(16).withHour(9).withMinute(0);
        LocalDateTime day3 = LocalDateTime.now().plusDays(17).withHour(9).withMinute(0);

        // Booking 1
        BookingRequest r1 = new BookingRequest();
        r1.setRoomName("F1");
        r1.setBookedBy(student);
        r1.setStartAt(day1);
        r1.setEndAt(day1.plusMinutes(60));
        r1.setPurpose("Session 1");
        bookingService.create(r1);

        // Booking 2
        BookingRequest r2 = new BookingRequest();
        r2.setRoomName("F2");
        r2.setBookedBy(student);
        r2.setStartAt(day2);
        r2.setEndAt(day2.plusMinutes(60));
        r2.setPurpose("Session 2");
        bookingService.create(r2);

        // Booking 3 -> từ chối
        mockMvc.perform(post("/bookings")
                        .param("roomName", "F3")
                        .param("bookedBy", student)
                        .param("startAt", day3.toString())
                        .param("endAt", day3.plusMinutes(60).toString())
                        .param("purpose", "Session 3 - Exceed limit"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeHasFieldErrorCode("booking", "bookedBy", "error.bookedBy"))
                .andExpect(content().string(containsString("đang giữ tối đa 2 lịch đặt phòng còn hiệu lực")));
    }

    @Test
    @DisplayName("TC8: Hủy phòng khi còn dưới 30 phút phải bị từ chối và ở lại trang list")
    void testCancelTooLateRejected() throws Exception {
        // Tạo booking bắt đầu sau 15 phút tính từ hiện tại (< 30 phút)
        LocalDateTime startSoon = LocalDateTime.now().plusMinutes(15);
        LocalDateTime endSoon = startSoon.plusMinutes(60);

        Booking booking = new Booking(null, "G707", "User G", startSoon, endSoon, "Urgent meeting", BookingStatus.CONFIRMED);
        Booking created = bookingService.create(BookingRequest.from(booking));

        // Gọi POST /bookings/{id}/cancel
        mockMvc.perform(post("/bookings/" + created.getId() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attribute("errorMessage", containsString("Chỉ được phép hủy trước giờ bắt đầu ít nhất 30 phút")));

        // Kiểm tra trạng thái vẫn là CONFIRMED (không bị hủy)
        assertEquals(BookingStatus.CONFIRMED, bookingService.findById(created.getId()).getStatus());
    }

    @Test
    @DisplayName("TC9: Hủy đúng hạn (>= 30 phút) thành công và cho phép người khác đặt lại khung giờ đó")
    void testCancelInTimeAndReuseSlot() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(60);

        BookingRequest req = new BookingRequest();
        req.setRoomName("H808");
        req.setBookedBy("User H1");
        req.setStartAt(start);
        req.setEndAt(end);
        req.setPurpose("Booking to be cancelled");
        Booking created = bookingService.create(req);

        // Hủy thành công vì cách thời gian bắt đầu nhiều ngày
        mockMvc.perform(post("/bookings/" + created.getId() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attribute("successMessage", containsString("thành công")));

        assertEquals(BookingStatus.CANCELLED, bookingService.findById(created.getId()).getStatus());

        // Người khác đặt lại đúng phòng H808 vào cùng khung giờ đó -> Thành công!
        mockMvc.perform(post("/bookings")
                        .param("roomName", "H808")
                        .param("bookedBy", "User H2")
                        .param("startAt", start.toString())
                        .param("endAt", end.toString())
                        .param("purpose", "Reuse freed slot"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attribute("successMessage", "Đặt phòng thành công!"));
    }
}
