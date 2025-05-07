# TLU Project Expo
Mô Tả Chi Tiết Bài Toán Ứng Dụng "TLU Project Expo" (Triển lãm Dự án TLU)
1. Tên ứng dụng: TLU Project Expo (Nền tảng Trưng bày Dự án Sinh viên TLU)
2. Nền tảng:
•	Back-end: ASP.NET Core Web API (sử dụng C#)
•	Front-end: Ứng dụng Web (ví dụ: sử dụng ASP.NET Core MVC/Razor Pages)
•	Ngôn ngữ Back-end: C#
3. Cơ sở dữ liệu: Microsoft SQL Server
4. Mô tả tổng quan: TLU Project Expo là một nền tảng trực tuyến hoạt động như một "triển lãm số", nơi sinh viên Đại học Thủy Lợi có thể giới thiệu các dự án (đồ án môn học, dự án cá nhân, nghiên cứu khoa học...) mà họ đã thực hiện. Nền tảng cho phép sinh viên tạo hồ sơ dự án chi tiết, bao gồm mô tả, công nghệ sử dụng, thành viên nhóm, liên kết đến mã nguồn hoặc sản phẩm demo. Những người dùng khác (sinh viên, giảng viên, nhà tuyển dụng tiềm năng) có thể duyệt, tìm kiếm và xem các dự án, để lại bình luận hoặc phản hồi, tạo cơ hội học hỏi, giao lưu và giới thiệu năng lực bản thân.
5. Yêu cầu chức năng chi tiết:
5.1. Quản lý Tài khoản Người dùng:
•	Đăng ký/Đăng nhập: Tương tự các ứng dụng trước (Email - có thể yêu cầu email trường, Mật khẩu băm, Họ tên). Xác thực email. Quên mật khẩu.
•	Hồ sơ người dùng (Profile): Hiển thị thông tin cơ bản (Tên, Khoa/Lớp, Ảnh đại diện), danh sách các dự án đã đăng/tham gia.
•	Vai trò người dùng: 
o	Student: Có thể tạo/quản lý dự án của mình, tham gia dự án của người khác, xem và bình luận các dự án khác.
o	Faculty (Giảng viên - Tùy chọn): Có thể xem các dự án, xác nhận/bảo trợ cho các dự án thuộc môn học mình phụ trách (tùy chọn), tìm kiếm sinh viên theo dự án.
o	Admin: Quản lý người dùng, quản lý danh mục công nghệ/lĩnh vực, quản lý/kiểm duyệt nội dung dự án/bình luận, làm nổi bật các dự án tiêu biểu.
o	Guest (Khách - Tùy chọn): Có thể chỉ cho phép xem danh sách và chi tiết dự án mà không cần đăng nhập.
5.2. Quản lý Dự án (Student):
•	Tạo Dự án Mới: 
o	Thông tin cơ bản: Tên dự án (Bắt buộc), Mô tả chi tiết (Bắt buộc), Ảnh đại diện/Thumbnail cho dự án (Tùy chọn).
o	Phân loại: Chọn Lĩnh vực/Chủ đề (ví dụ: Web Development, Mobile App, AI/ML, IoT, Data Science...) và các Công nghệ/Ngôn ngữ lập trình đã sử dụng (ví dụ: C#, ASP.NET Core, Java, Python, React...). Danh sách này có thể do Admin quản lý hoặc cho phép người dùng thêm mới (cần kiểm duyệt).
o	Môn học liên quan (Tùy chọn): Chọn môn học mà dự án này thuộc về (nếu là đồ án môn học).
o	Thành viên nhóm: Mời hoặc thêm các thành viên khác (đã có tài khoản trên hệ thống) vào dự án. Xác định vai trò (ví dụ: Leader, Member).
o	Liên kết: Cung cấp các đường link liên quan: 
	Link mã nguồn (GitHub, GitLab...).
	Link sản phẩm chạy thử (Live Demo URL).
	Link video giới thiệu (YouTube...).
o	Trạng thái dự án: Đang thực hiện, Đã hoàn thành, Tạm dừng...
•	Tải lên Media: Cho phép tải lên thêm hình ảnh, hoặc nhúng video mô tả dự án.
•	Quản lý Dự án: 
o	Xem danh sách các dự án mình đã tạo hoặc tham gia.
o	Chỉnh sửa thông tin dự án.
o	Quản lý thành viên nhóm (thêm/xóa thành viên - chỉ Leader/Admin dự án).
o	Xóa dự án.
5.3. Duyệt và Tìm kiếm Dự án:
•	Xem danh sách dự án: Hiển thị các dự án (có thể là các dự án nổi bật, mới nhất...) dưới dạng thẻ (card) hoặc danh sách. Thông tin tóm tắt: Ảnh, Tên dự án, Mô tả ngắn, Người tạo/Nhóm, Lĩnh vực/Công nghệ chính.
•	Chức năng: 
o	Tìm kiếm: Theo Tên dự án, Mô tả, Công nghệ sử dụng, Tên thành viên.
o	Lọc: Lọc theo Lĩnh vực/Chủ đề, theo Công nghệ, theo Môn học (nếu có), theo Trạng thái.
o	Sắp xếp: Theo ngày đăng, theo tên, theo lượt xem/bình chọn (nếu có).
•	Xem chi tiết dự án: 
o	Hiển thị đầy đủ các thông tin người dùng đã nhập khi tạo dự án.
o	Hiển thị hình ảnh/video.
o	Hiển thị danh sách thành viên nhóm (có thể link đến profile của họ).
o	Hiển thị các bình luận/phản hồi.
5.4. Tương tác và Phản hồi:
•	Bình luận/Phản hồi: Người dùng đã đăng nhập có thể để lại bình luận, câu hỏi hoặc phản hồi mang tính xây dựng dưới mỗi dự án.
•	Bình chọn (Upvote - Tùy chọn): Người dùng có thể bình chọn (upvote) cho các dự án họ thấy hay và hữu ích. Số lượt vote có thể dùng để xếp hạng hoặc làm nổi bật dự án.
•	Theo dõi dự án/người dùng (Tùy chọn): Cho phép người dùng theo dõi các dự án hoặc tác giả cụ thể để nhận thông báo khi có cập nhật.
5.5. Quản lý Hệ thống (Admin):
•	Quản lý người dùng: Xem danh sách người dùng, phân quyền, khóa/mở khóa tài khoản.
•	Quản lý Danh mục/Thẻ: Quản lý danh sách các Lĩnh vực, Công nghệ, Môn học dùng để phân loại dự án.
•	Kiểm duyệt nội dung: Xem xét và duyệt/xóa các dự án hoặc bình luận không phù hợp, vi phạm quy định.
•	Làm nổi bật dự án: Chọn các dự án tiêu biểu để hiển thị ở trang chủ hoặc mục riêng.
6. Cấu trúc cơ sở dữ liệu gợi ý (SQL Server):
•	Bảng Users, Roles, UserRoles: Tương tự các ví dụ trước. Roles có thể là 'Admin', 'Faculty', 'Student'.
•	Bảng Categories (Lĩnh vực): CategoryId (PK), Name.
•	Bảng Technologies: TechnologyId (PK), Name (NVARCHAR, UNIQUE).
•	Bảng Courses (Tùy chọn): CourseId (PK), CourseCode, CourseName.
•	Bảng Projects: 
o	ProjectId (PK, INT, IDENTITY).
o	Title (NVARCHAR, NOT NULL).
o	Description (NVARCHAR(MAX)).
o	ThumbnailUrl (NVARCHAR, NULL).
o	ProjectUrl (NVARCHAR, NULL): Link GitHub/GitLab...
o	DemoUrl (NVARCHAR, NULL): Link chạy thử.
o	VideoUrl (NVARCHAR, NULL).
o	Status (VARCHAR: 'InProgress', 'Completed', 'Paused').
o	CourseId (FK -> Courses, NULL).
o	CreatorUserId (FK -> Users, INT, NOT NULL).
o	CreatedAt (DATETIME2).
o	UpdatedAt (DATETIME2, NULL).
o	IsApproved (BIT, DEFAULT 0): Trạng thái kiểm duyệt (nếu cần).
o	VoteCount (INT, DEFAULT 0) (Nếu có tính năng vote).
•	Bảng ProjectCategories: ProjectId (FK), CategoryId (FK). (PK gồm ProjectId, CategoryId).
•	Bảng ProjectTechnologies: ProjectId (FK), TechnologyId (FK). (PK gồm ProjectId, TechnologyId).
•	Bảng ProjectMembers: ProjectId (FK), UserId (FK), RoleInProject (VARCHAR: 'Leader', 'Member'). (PK gồm ProjectId, UserId).
•	Bảng ProjectImages (Tùy chọn): ImageId (PK), ProjectId (FK), ImageUrl, Caption.
•	Bảng Comments: CommentId (PK, INT, IDENTITY), ProjectId (FK -> Projects), AuthorUserId (FK -> Users), Content (NVARCHAR(MAX)), CreatedAt, ParentCommentId (FK -> Comments, NULL - để làm trả lời bình luận).
•	Bảng Votes (Tùy chọn): VoteId (PK), UserId (FK), ProjectId (FK, NULL), CommentId (FK, NULL), VoteType (BIT/SMALLINT: 1 for upvote). UNIQUE (UserId, ProjectId), UNIQUE (UserId, CommentId).
7. Lưu ý quan trọng:
•	Xử lý Media: Cần có cơ chế tải lên, lưu trữ (trên server hoặc cloud storage như Azure Blob Storage, AWS S3) và hiển thị hình ảnh/video hiệu quả.
•	Tìm kiếm & Lọc: Chức năng tìm kiếm và lọc cần được thiết kế tốt và tối ưu (sử dụng index, có thể cân nhắc Full-Text Search).
•	Quản lý Thẻ/Danh mục: Quyết định cơ chế quản lý (Admin tạo trước hay cho người dùng tự do thêm - cần kiểm duyệt).
•	Quyền riêng tư: Cân nhắc quyền xem dự án (công khai, chỉ thành viên TLU, chỉ thành viên nhóm?).
•	Kiểm duyệt: Cần có quy trình kiểm duyệt nội dung nếu cho phép người dùng đăng tải tự do để tránh nội dung không phù hợp.
Đề tài "TLU Project Expo" tạo ra một không gian giá trị để sinh viên thể hiện năng lực, học hỏi lẫn nhau và có thể là cầu nối với giảng viên hoặc nhà tuyển dụng. Nó bao gồm đủ các yếu tố CRUD, quản lý quan hệ và các tính năng tương tác thú vị.

