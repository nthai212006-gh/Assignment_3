# Meeting Room Booking Application (Assignment 3)

Ứng dụng Quản lý Đặt phòng họp xây dựng bằng **Spring Boot 3 + Thymeleaf + In-Memory Store (`ArrayList`)**, chuẩn kiến trúc MVC và giao diện hiện đại với **Bootstrap 5 & Icons**.

---

## Hướng Dẫn Chạy Trên VS Code

### Cách 1: Sử dụng giao diện VS Code (Khuyên dùng)
1. Cài đặt extension pack: **Extension Pack for Java** và **Spring Boot Extension Pack** trên VS Code.
2. Mở file [BookingApplication.java](file:///c:/Users/T14%20GEN2/Documents/LAB_3/src/main/java/com/example/booking/BookingApplication.java).
3. Nhấn vào nút **Run** hoặc **Debug** (hoặc phím tắt **F5**).
4. Mở trình duyệt và truy cập: [http://localhost:8080](http://localhost:8080) (tự động chuyển hướng về `/bookings`).

### Cách 2: Sử dụng Terminal trong VS Code
Mở Terminal (`Ctrl + ` `) và chạy lệnh:
```bash
mvn spring-boot:run
```

Để chạy toàn bộ bộ kiểm thử tự động (9/9 Test cases pass):
```bash
mvn test
```

---

##  Cấu Trúc Dự Án (Bám sát kiến trúc Lab 1)

```text
src/main/java/com/example/booking/
├── BookingApplication.java               # File khởi chạy ứng dụng
├── model/
│   ├── Booking.java                      # Model lưu trữ dữ liệu (có id, status)
│   └── BookingStatus.java                # Enum trạng thái (CONFIRMED, CANCELLED)
├── dto/
│   └── BookingRequest.java               # Form DTO + Bean Validation (không chứa id, status)
├── service/
│   └── BookingService.java               # In-memory store (ArrayList), kiểm tra Booking Policy
└── controller/
    ├── BookingController.java            # Xử lý HTTP Request, BindingResult, Policy rules
    └── HomeController.java               # Điều hướng "/" về "/bookings"

src/main/resources/
├── application.properties                # Cấu hình cổng 8080, tắt thymeleaf cache
└── templates/bookings/
    ├── list.html                         # Giao diện danh sách đặt phòng hiện đại
    └── form.html                         # Giao diện tạo mới / chỉnh sửa kèm inline error
```

---

## Các Quy Tắc Nghiệp Vụ (Booking Policy) Đã Triển Khai

| Quy tắc | Hành vi xử lý | Vị trí hiển thị lỗi |
| :--- | :--- | :--- |
| **Validation rỗng** | `@NotBlank`, `@NotNull` trên các trường | Cạnh từng trường tương ứng (`th:errors`) |
| **Thời gian kết thúc sau bắt đầu** | `endAt` phải sau `startAt` | Cạnh trường **Thời gian kết thúc** |
| **Thời lượng tối đa 120 phút** | Khoảng cách `startAt` và `endAt` $\le$ 120 phút | Cạnh trường **Thời gian kết thúc** |
| **Thời gian bắt đầu ở quá khứ** | `startAt` không được nhỏ hơn thời điểm hiện tại | Cạnh trường **Thời gian bắt đầu** |
| **Trùng phòng cùng giờ** | Không được trùng giờ trên cùng một phòng với các booking `CONFIRMED` khác | Cạnh trường **Tên phòng họp** |
| **Giới hạn 2 booking/người** | Một người chỉ được giữ tối đa 2 booking `CONFIRMED` còn hiệu lực | Cạnh trường **Người đặt** |
| **Quy định hủy trước 30 phút** | Chỉ hủy được trước giờ bắt đầu $\ge$ 30 phút, hủy bằng POST, đổi sang `CANCELLED` (không xóa) | Alert banner trên **Trang danh sách** |
