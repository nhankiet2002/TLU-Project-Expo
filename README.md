# Findlt-TLU
Mô Tả Chi Tiết Bài Toán Ứng Dụng "FindIt@TLU" (Tìm Đồ Thất Lạc TLU)
1. Tên ứng dụng: FindIt@TLU (Tìm đồ thất lạc tại Đại học Thủy Lợi)
2. Nền tảng:
•	Back-end: ASP.NET Core Web API (sử dụng C#)
•	Front-end: Ứng dụng Web (ví dụ: sử dụng ASP.NET Core MVC/Razor Pages) hoặc Ứng dụng Di động (Android/iOS gọi API). Giả định trong mô tả này là một ứng dụng Web.
•	Ngôn ngữ Back-end: C#
3. Cơ sở dữ liệu: Microsoft SQL Server
4. Mô tả tổng quan: FindIt@TLU là một nền tảng trực tuyến giúp kết nối cộng đồng Đại học Thủy Lợi (sinh viên, cán bộ, giảng viên) trong việc thông báo và tìm kiếm đồ vật bị thất lạc hoặc nhặt được trong khuôn viên trường hoặc khu vực lân cận. Ứng dụng cho phép người dùng đăng tin về đồ vật bị mất hoặc nhặt được, tìm kiếm trong danh sách các tin đã đăng, và hỗ trợ quá trình liên hệ để trao trả đồ vật.
5. Yêu cầu chức năng chi tiết:
5.1. Quản lý Tài khoản Người dùng:
•	Đăng ký: 
o	Người dùng đăng ký tài khoản mới (có thể yêu cầu dùng email trường @tlu.edu.vn/@e.tlu.edu.vn để giới hạn cộng đồng hoặc cho phép email cá nhân).
o	Thông tin cần thiết: Email (duy nhất), Mật khẩu (băm và lưu an toàn), Họ và tên.
o	Có thể yêu cầu xác thực email.
•	Đăng nhập: 
o	Người dùng đăng nhập bằng Email và Mật khẩu.
o	Xác thực thông tin với bảng Users trong CSDL.
o	Có chức năng "Quên mật khẩu".
•	Thông tin liên hệ (Tùy chọn bảo mật): Người dùng có thể tùy chọn cung cấp số điện thoại hoặc phương thức liên lạc khác trong hồ sơ cá nhân, và có thể chọn hiển thị/ẩn thông tin này trên các tin đăng của mình.
5.2. Đăng Tin Đồ Vật:
•	Chọn loại tin đăng: Người dùng chọn đăng tin "Tôi làm mất đồ" hoặc "Tôi nhặt được đồ".
•	Nhập thông tin đồ vật: 
o	Tiêu đề tin: Mô tả ngắn gọn (ví dụ: "Mất ví màu đen gần thư viện", "Nhặt được thẻ sinh viên Nguyễn Văn A"). (Bắt buộc)
o	Mô tả chi tiết: Cung cấp thêm thông tin về đồ vật (đặc điểm, nhãn hiệu, tình trạng...), hoàn cảnh mất/tìm thấy. (Bắt buộc)
o	Danh mục đồ vật: Chọn từ danh sách có sẵn (ví dụ: Điện tử, Giấy tờ tùy thân, Ví/Túi xách, Quần áo/Phụ kiện, Sách vở, Khác...). (Bắt buộc)
o	Địa điểm: Mô tả địa điểm mất hoặc nhặt được (ví dụ: "Sân C1", "Nhà ăn", "Thư viện tầng 3", "Tuyến bus 21"). Có thể tích hợp bản đồ nếu muốn (phức tạp hơn). (Bắt buộc)
o	Ngày mất/nhặt được: Chọn ngày xảy ra sự việc. (Bắt buộc)
o	Ảnh đồ vật (Tùy chọn): Cho phép tải lên 1 hoặc nhiều ảnh minh họa. Ảnh sẽ được lưu trữ (ví dụ: trên server hoặc dịch vụ lưu trữ đám mây) và đường dẫn lưu vào CSDL.
o	Thông tin liên hệ: Người đăng có thể chọn hiển thị thông tin liên hệ nào (email/số điện thoại từ hồ sơ) hoặc cho phép liên hệ qua hệ thống nhắn tin nội bộ (nếu có).
•	Lưu tin đăng: Thông tin được lưu vào bảng Items trong CSDL với trạng thái tương ứng ("Lost" hoặc "Found"). Tin đăng sẽ liên kết với UserId của người đăng.
5.3. Tìm Kiếm và Duyệt Tin Đăng:
•	Hiển thị danh sách: Hiển thị danh sách các tin đăng đồ vật (cả mất và tìm thấy) dưới dạng thẻ hoặc danh sách. 
o	Thông tin hiển thị cơ bản: Ảnh (nếu có), Tiêu đề, Danh mục, Ngày đăng, Địa điểm (tóm tắt), Trạng thái (Mất/Tìm thấy/Đã trả).
•	Chức năng: 
o	Tìm kiếm: Tìm kiếm theo từ khóa trong Tiêu đề, Mô tả.
o	Lọc: Lọc theo trạng thái (Mất/Tìm thấy/Đã trả), theo Danh mục, theo Khu vực/Địa điểm (nếu có cấu trúc), theo Khoảng thời gian.
o	Sắp xếp: Sắp xếp theo ngày đăng mới nhất/cũ nhất.
•	Xem chi tiết tin đăng: 
o	Hiển thị đầy đủ thông tin đã nhập khi đăng tin.
o	Hiển thị thông tin liên hệ của người đăng (nếu họ cho phép) hoặc nút để bắt đầu liên hệ qua hệ thống (nếu có).
o	Hiển thị trạng thái hiện tại của tin (Mất/Tìm thấy/Đã trả).
5.4. Liên Hệ và Xác Nhận Trao Trả:
•	Cơ chế liên hệ: 
o	Cách 1 (Đơn giản): Hiển thị thông tin liên hệ (email/SĐT) mà người đăng đã chọn công khai. Người tìm thấy/làm mất sẽ tự liên hệ bên ngoài ứng dụng.
o	Cách 2 (Phức tạp hơn): Xây dựng hệ thống nhắn tin/bình luận đơn giản ngay dưới mỗi tin đăng để người dùng trao đổi ẩn danh (chỉ người đăng và người liên hệ thấy?). Cần cân nhắc kỹ về độ phức tạp.
•	Đánh dấu "Đã trả/Đã tìm thấy": 
o	Người đăng tin (cả mất và nhặt được) có quyền đánh dấu tin của mình là "Đã hoàn thành" hoặc "Đã trả lại".
o	Tin đăng được đánh dấu sẽ chuyển sang trạng thái "Returned" và có thể bị ẩn khỏi danh sách tìm kiếm chính nhưng vẫn có thể xem lại trong lịch sử của người đăng.
5.5. Quản Lý Tin Đăng Cá Nhân:
•	Người dùng đã đăng nhập có thể xem danh sách các tin mình đã đăng (cả mất và tìm thấy).
•	Cho phép người dùng chỉnh sửa thông tin tin đăng của mình (trừ một số thông tin cốt lõi như loại tin?).
•	Cho phép người dùng xóa tin đăng của mình.
•	Cho phép người dùng đánh dấu tin là đã hoàn thành/đã trả lại.
6. Cấu trúc cơ sở dữ liệu gợi ý (SQL Server):
•	Bảng Users: UserId (PK, INT, IDENTITY), Email (VARCHAR, UNIQUE), PasswordHash (NVARCHAR), FullName (NVARCHAR), PhotoUrl (NVARCHAR), PhoneNumber (VARCHAR), CreatedAt (DATETIME2). (Có thể không cần bảng Roles nếu chỉ có 1 loại người dùng).
•	Bảng Categories: CategoryId (PK, INT, IDENTITY), Name (NVARCHAR, NOT NULL, UNIQUE). (Ví dụ: Điện tử, Giấy tờ, Ví/Túi...).
•	Bảng Items: 
o	ItemId (PK, INT, IDENTITY): Khóa chính tin đăng.
o	UserId (FK -> Users, INT, NOT NULL): Người đăng tin.
o	CategoryId (FK -> Categories, INT, NOT NULL): Loại đồ vật.
o	Title (NVARCHAR(200), NOT NULL): Tiêu đề tin.
o	Description (NVARCHAR(MAX), NOT NULL): Mô tả chi tiết.
o	Location (NVARCHAR(500), NOT NULL): Địa điểm mất/tìm thấy.
o	ItemStatus (VARCHAR(10), NOT NULL): Trạng thái ('Lost', 'Found', 'Returned'). Cần tạo CHECK constraint.
o	DateLostOrFound (DATE, NOT NULL): Ngày mất/tìm thấy.
o	CreatedAt (DATETIME2, NOT NULL, DEFAULT GETDATE()): Ngày đăng tin.
o	UpdatedAt (DATETIME2, NULL): Ngày cập nhật tin.
o	IsContactInfoPublic (BIT, NOT NULL, DEFAULT 0): Cờ cho biết có hiển thị SĐT/Email của người đăng không.
•	Bảng ItemImages (Tùy chọn): 
o	ImageId (PK, INT, IDENTITY): Khóa chính ảnh.
o	ItemId (FK -> Items, INT, NOT NULL): Tin đăng chứa ảnh này.
o	ImageUrl (NVARCHAR(500), NOT NULL): Đường dẫn đến file ảnh.
•	(Tùy chọn) Bảng Messages hoặc Claims: Nếu muốn xây dựng hệ thống liên hệ/xác nhận trong ứng dụng. Cần thiết kế cẩn thận hơn.
7. Lưu ý quan trọng:
•	Bảo mật & Riêng tư: 
o	Băm mật khẩu an toàn.
o	Cẩn thận với việc hiển thị thông tin liên hệ cá nhân. Cần cho người dùng lựa chọn rõ ràng. Nếu có hệ thống nhắn tin nội bộ, cần đảm bảo an toàn.
o	Validate dữ liệu đầu vào kỹ lưỡng.
•	Hiệu suất: 
o	Tối ưu truy vấn tìm kiếm/lọc (sử dụng index cho CategoryId, ItemStatus, DateLostOrFound, full-text search cho Title/Description nếu cần).
o	Xử lý tải ảnh hiệu quả (nén ảnh, sử dụng CDN nếu cần).
o	Phân trang khi hiển thị danh sách tin đăng.
•	Trải nghiệm người dùng: 
o	Giao diện tìm kiếm, lọc, đăng tin phải đơn giản, dễ sử dụng.
o	Hiển thị ảnh rõ ràng.
o	Quy trình đánh dấu "đã trả" cần thuận tiện.
•	Quản trị (Tùy chọn): Có thể cần vai trò Admin để quản lý danh mục, xóa các tin không phù hợp, quản lý người dùng.
Ý tưởng "FindIt@TLU" này tập trung vào việc giải quyết một vấn đề thực tế trong môi trường học đường/cộng đồng, có các chức năng CRUD cơ bản và tiềm năng mở rộng, phù hợp cho một đồ án môn học.
