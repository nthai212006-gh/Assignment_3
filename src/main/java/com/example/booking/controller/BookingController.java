package com.example.booking.controller;

import com.example.booking.dto.BookingRequest;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("booking", new BookingRequest());
        model.addAttribute("bookingId", null);
        return "bookings/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("booking") BookingRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        // Kiểm tra các quy tắc nghiệp vụ (Booking Policy) và gắn lỗi vào từng trường
        validateBookingPolicy(request, result, null);

        if (result.hasErrors()) {
            model.addAttribute("bookingId", null);
            return "bookings/form";
        }

        bookingService.create(request);
        redirectAttributes.addFlashAttribute("successMessage", "Đặt phòng thành công!");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chỉnh sửa lịch đặt phòng đã bị hủy.");
            return "redirect:/bookings";
        }

        model.addAttribute("booking", BookingRequest.from(booking));
        model.addAttribute("bookingId", id);
        return "bookings/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("booking") BookingRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        validateBookingPolicy(request, result, id);

        if (result.hasErrors()) {
            model.addAttribute("bookingId", id);
            return "bookings/form";
        }

        try {
            bookingService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch đặt phòng thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy lịch đặt phòng #" + id + " thành công.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    /**
     * Kiểm tra các chính sách đặt phòng và bind lỗi về đúng field bị vi phạm:
     * 1. startAt không được ở quá khứ
     * 2. endAt phải sau startAt
     * 3. Thời lượng tối đa 120 phút
     * 4. Không được trùng giờ cùng phòng đối với các booking CONFIRMED
     * 5. Mỗi người chỉ được giữ tối đa 2 booking CONFIRMED
     */
    private void validateBookingPolicy(BookingRequest req, BindingResult result, Long currentBookingId) {
        LocalDateTime start = req.getStartAt();
        LocalDateTime end = req.getEndAt();

        // 1. Kiểm tra thời gian bắt đầu ở quá khứ
        if (start != null && start.isBefore(LocalDateTime.now())) {
            result.rejectValue("startAt", "error.startAt", "Thời gian bắt đầu không được ở trong quá khứ.");
        }

        // 2 & 3. Kiểm tra thứ tự thời gian và thời lượng tối đa 120 phút
        if (start != null && end != null) {
            if (!end.isAfter(start)) {
                result.rejectValue("endAt", "error.endAt", "Thời gian kết thúc phải sau thời gian bắt đầu.");
            } else {
                long minutes = Duration.between(start, end).toMinutes();
                if (minutes > 120) {
                    result.rejectValue("endAt", "error.endAt", "Thời gian đặt phòng tối đa không được vượt quá 120 phút (hiện tại: " + minutes + " phút).");
                }
            }
        }

        // 4. Kiểm tra trùng phòng (chỉ tính các booking CONFIRMED)
        if (req.getRoomName() != null && !req.getRoomName().isBlank() && start != null && end != null && end.isAfter(start)) {
            boolean hasOverlap = bookingService.hasRoomOverlap(req.getRoomName(), start, end, currentBookingId);
            if (hasOverlap) {
                result.rejectValue("roomName", "error.roomName", "Phòng " + req.getRoomName() + " đã có người đặt trong khoảng thời gian này.");
            }
        }

        // 5. Kiểm tra giới hạn tối đa 2 booking CONFIRMED cho 1 người
        if (req.getBookedBy() != null && !req.getBookedBy().isBlank()) {
            long activeCount = bookingService.countActiveBookingsByPerson(req.getBookedBy(), currentBookingId);
            if (activeCount >= 2) {
                result.rejectValue("bookedBy", "error.bookedBy", "Người này (" + req.getBookedBy() + ") đang giữ tối đa 2 lịch đặt phòng còn hiệu lực.");
            }
        }
    }
}
