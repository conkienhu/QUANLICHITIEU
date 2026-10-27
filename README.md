# Quản lý chi tiêu hàng tháng (Java Swing)

Yêu cầu: JDK 11 trở lên. Không cần thư viện ngoài.

Chạy: `./build.sh` (Linux/macOS) hoặc `run.bat` (Windows).
Hoặc mở thư mục trong IntelliJ / Eclipse / NetBeans, đặt `src` là source root, chạy `com.expense.Main`.

Chức năng: thêm/sửa/xóa giao dịch thu - chi, xem theo tháng/năm, lọc theo loại, tìm kiếm,
tổng thu - chi - còn lại, biểu đồ tròn theo danh mục, ngân sách theo danh mục (cảnh báo khi vượt),
xuất CSV (mở được bằng Excel). Dữ liệu tự lưu tại `~/.expense-manager/`.
