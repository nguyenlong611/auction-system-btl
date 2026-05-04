# ⚡ Adjugé! — Premium Auction Platform

**Adjugé!** là một ứng dụng đấu giá trực tuyến hiện đại được xây dựng bằng JavaFX, tập trung vào trải nghiệm người dùng mượt mà (SPA) và kiến trúc mã nguồn chuẩn hướng đối tượng (OOP).

![Demo Screenshot](https://raw.githubusercontent.com/William-Sommers/Bidscape_/main/resources/preview.png) *(Lưu ý: Bạn có thể thay link ảnh demo của mình vào đây)*

## ✨ Tính năng nổi bật

- **Kiến trúc Single Page Application (SPA):** Chuyển đổi mượt mà giữa các màn hình (Home, Search, Auction Detail, Dashboard) mà không cần load lại cửa sổ.
- **Hệ thống đấu giá thời gian thực:** Tự động đếm ngược và cập nhật trạng thái phiên đấu giá.
- **Tìm kiếm & Bộ lọc:** Tìm kiếm sản phẩm theo tên và lọc theo danh mục (Electronics, Art, Vehicles, Fashion...).
- **Dashboard người dùng:** Quản lý các phiên đang tham gia, lịch sử đặt giá và các sản phẩm đang rao bán.
- **Thiết kế hiện đại:** Giao diện tối (Dark Mode) với hiệu ứng Glassmorphism và Typography cao cấp.

## 🛠 Công nghệ sử dụng

- **Core:** Java 21+
- **UI Framework:** JavaFX 24
- **Database:** SQLite (với JDBC Driver)
- **Style:** Vanilla CSS (Custom styling cho JavaFX)
- **Build Tool:** Script chạy trực tiếp (`run.bat`) hỗ trợ tự động compile và cấu hình module-path.

## 🏗 Kiến trúc & Design Patterns

Dự án được xây dựng theo mô hình **MVC (Model-View-Controller)** kết hợp với **DAO (Data Access Object)**, áp dụng các mẫu thiết kế kinh điển:

1.  **Singleton:** Quản lý `DataStore` và `DatabaseManager` để đảm bảo duy nhất một instance trong suốt vòng đời ứng dụng.
2.  **Observer Pattern:** Cập nhật UI ngay lập tức khi có người đặt giá mới (Real-time bidding).
3.  **Factory Pattern:** Khởi tạo các loại sản phẩm khác nhau (Electronics, Art, etc.) một cách linh hoạt thông qua `ItemFactory`.
4.  **Polymorphism:** Tận dụng đa hình để xử lý các thuộc tính đặc thù của từng loại mặt hàng trên cùng một giao diện chung.
5.  **Strategy Pattern:** (Nếu có) Xử lý các logic tính toán giá hoặc phí đấu giá khác nhau.

## 📂 Cấu trúc thư mục

- `src/com/adjuge/model`: Định nghĩa các thực thể (Auction, User, Item...).
- `src/com/adjuge/controller`: Điều khiển logic giao diện.
- `src/com/adjuge/service`: Xử lý nghiệp vụ (Bidding, Auth, Timer).
- `src/com/adjuge/dao`: Lớp truy xuất dữ liệu từ SQLite.
- `resources/`: Chứa file FXML, CSS, Fonts và hình ảnh.
- `javafx/`: Chứa SDK JavaFX cần thiết để chạy dự án.

## 🚀 Hướng dẫn khởi chạy

Để chạy dự án trên Windows, bạn chỉ cần thực hiện các bước sau:

1.  Đảm bảo bạn đã cài đặt **JDK 21** trở lên.
2.  Mở thư mục gốc của dự án.
3.  Chạy file `run.bat`.

Script sẽ tự động:
- Compile mã nguồn Java.
- Copy tài nguyên vào thư mục `out`.
- Khởi chạy ứng dụng với cấu hình module-path cho JavaFX.

---
*Dự án được thực hiện cho học phần Lập trình hướng đối tượng (OOP).*
