# Changelog

File ghi lại những thay đổi của dự án.
Định dạng dựa theo [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### 2026-08-22 - View Statistics and Payment History

**Người thực hiện:** [Trần Trung Hiếu]

#### Added

- Endpoint `GET /api/admin/statistics/overview`: thống kê tổng số user, số booking thành công và số venue đang hoạt động
- Endpoint `GET /api/admin/statistics/revenue`: thống kê tổng doanh thu theo năm và doanh thu chi tiết theo từng tháng
- Endpoint `GET /api/admin/payments`: lấy toàn bộ lịch sử thanh toán trên hệ thống, sắp xếp giao dịch mới nhất trước
- `StatisticsOverviewResponse`: DTO phản hồi dữ liệu thống kê tổng quan hệ thống
- `RevenueStatisticsResponse`: DTO phản hồi tổng doanh thu và doanh thu theo 12 tháng
- `PaymentResponse`: DTO phản hồi thông tin lịch sử thanh toán
- `PaymentRepository`: bổ sung truy vấn thống kê tổng doanh thu theo năm, doanh thu theo tháng và lấy danh sách payment theo thời gian thanh toán giảm dần
- `StatisticsService` & `StatisticsServiceImpl`: xử lý logic thống kê tổng quan, thống kê doanh thu và lịch sử thanh toán
- `AdminStatisticsController`: cung cấp API thống kê tổng quan và thống kê doanh thu cho Admin
- `AdminPaymentController`: cung cấp API xem toàn bộ lịch sử thanh toán cho Admin
- `StatisticsServiceImplTest`: unit test cho thống kê tổng quan, thống kê doanh thu và lịch sử thanh toán

#### Changed

- Giới hạn các API thống kê và lịch sử thanh toán chỉ cho tài khoản có vai trò `ADMIN`
- Doanh thu theo năm được trả về kèm đầy đủ 12 tháng, các tháng không có giao dịch có giá trị doanh thu bằng `0`
### 2026-08-22 - Register/Upgrade to HOST (POST /api/users/me/roles/host) (#99269)

**Người thực hiện:** [Huỳnh Trương Thảo Duyên]

#### Added

- Endpoint `POST /api/users/me/roles/host`: cho phép user đã đăng nhập (role `USER`) upload `businessLicense` (`multipart/form-data`, tái sử dụng `@ValidImage(required = false)`) và tự động nâng cấp lên role `HOST` khi đủ **cả 3** điều kiện: `status == ACTIVE`, `isIdentityVerified == true`, `isBusinessVerified == true`. Không nhận `role`/`isBusinessVerified`/`isIdentityVerified`/`status`/`userId` từ client — toàn bộ lấy từ user hiện tại qua `@AuthenticationPrincipal` và dữ liệu đã lưu trong DB
- DTO `BecomeHostRequest` (field `businessLicense` duy nhất) và `HostUpgradeResponse` (bọc `UserProfileResponse` + cờ `alreadyHost` để Controller chọn đúng message)
- `UserService.becomeHost(...)`/`UserServiceImpl`: thứ tự kiểm tra rõ ràng — user tồn tại → đã là HOST chưa (idempotent, không tạo role trùng) → `status == ACTIVE` → có `business_license_url` → `isIdentityVerified` → `isBusinessVerified` → gán role `HOST`
- Cột `business_license_hash` (SHA-256 hex nội dung file) trên `User` entity: dùng để phân biệt "upload lại đúng file cũ" và "upload file mới thật sự" (xem mục Fixed)
- Message key mới (en/vi): `host.upgrade.success`, `host.already`, `host.status.not.active`, `host.license.required`, `host.business.pending`, `host.identity.required`, `role.not.found`
- Test: `UserServiceImplTest.BecomeHostTests` (unit, mock repository, đủ các case theo đúng thứ tự kiểm tra ở trên) và `UserControllerTest.BecomeHostEndpointTests` (mock service, kiểm tra response HTTP/message)
- **`BecomeHostIntegrationTest`** (mới): test end-to-end với DB thật + Spring transaction thật (chỉ mock `FileStorageService` để không gọi Supabase thật) — xác nhận license được lưu thật ngay cả khi bị từ chối, và luồng lên HOST hoạt động đúng qua HTTP thật

#### Fixed

- **[Nghiêm trọng]** `business_license_url` bị NULL vĩnh viễn sau khi upload dù file đã lên Supabase thành công: do toàn bộ `becomeHost()` nằm trong một `@Transactional` duy nhất, và method luôn `throw AppException` ngay sau khi `save()` license (vì vừa upload thì verification bị reset về `false`, chưa thể đủ điều kiện thành HOST ngay). Mặc định Spring **rollback toàn bộ transaction** khi có `RuntimeException` thoát ra khỏi method, cuốn theo cả câu `save()` license vừa chạy trước đó — khiến DB không bao giờ thực sự lưu được URL. Sửa bằng `@Transactional(noRollbackFor = AppException.class)`: transaction vẫn **commit** khi bị từ chối do business rule, chỉ rollback khi có lỗi hệ thống thật. Lỗi này không bị unit test cũ (mock `UserRepository`) phát hiện vì mock không mô phỏng rollback thật — phải viết `BecomeHostIntegrationTest` với DB thật mới tái hiện và xác nhận đã sửa
- Việc set `is_business_verified = true` thủ công trong DB bị "mất tác dụng" (API vẫn trả 403 pending) — hệ quả trực tiếp của lỗi rollback ở trên, vì `business_license_url` chưa từng lưu thật nên điều kiện luôn thất bại ở bước "cần license"

#### Lưu ý quan trọng — quy tắc reset xác minh khi upload

- Mỗi khi upload một giấy phép **mới** (khác nội dung với file đang lưu), `is_business_verified` luôn bị đặt lại `false` để bắt buộc duyệt lại — đúng theo thiết kế ban đầu, **không đổi**.
- Tuy nhiên nếu client **tải lại đúng cùng một ảnh** (so khớp bằng SHA-256 qua `business_license_hash`, ví dụ Swagger UI vẫn còn giữ sẵn file cũ khi bấm Execute lần nữa để kiểm tra lại điều kiện) thì hệ thống **không** reset `is_business_verified` — tránh vòng lặp "vừa được duyệt xong lại bị reset về pending" chỉ vì gọi lại API với cùng file.

#### Cách sử dụng `POST /api/users/me/roles/host`

1. `POST /api/auth/login` lấy `accessToken`, bấm **Authorize** trên Swagger UI.
2. Gọi `POST /api/users/me/roles/host` kèm file `businessLicense` (JPEG/PNG/WEBP) — nếu user chưa `ACTIVE`/chưa verify thì nhận `403` kèm message tương ứng, nhưng `business_license_url` đã được lưu thật trong DB (kiểm tra bằng `SELECT business_license_url FROM users WHERE email = '...'`).
3. Set thủ công trong DB: `status = 'ACTIVE'`, `is_identity_verified = true`, `is_business_verified = true` cho user đó (chưa có chức năng moderator duyệt qua UI).
4. Gọi lại API (không cần đính kèm file nữa, hoặc đính kèm lại đúng file cũ đều được) → `200 "You have successfully become a Host."`, `data.roles` chứa `HOST`.
5. Gọi lại lần nữa → `200 "You are already a Host."` (không tạo role `HOST` trùng).

---

### 2026-08-22 - Update Current User Profile (PUT /api/users/me) (#99271)

**Người thực hiện:** Huỳnh Trương Thảo Duyên

#### Added

- Endpoint `PUT /api/users/me`: cho phép user đã đăng nhập cập nhật `name`, `phone`, `cccdImage` của chính mình, dạng `multipart/form-data`, tất cả field đều optional (partial update - field nào không gửi thì giữ nguyên giá trị cũ, không bị ghi đè bằng null/rỗng); không nhận `userId` từ client, lấy user qua `@AuthenticationPrincipal` (Spring Security context do `JwtAuthenticationFilter` xác thực sẵn)
- DTO `UpdateUserRequest`: `name` (`@Size(max=150)`, trim + kiểm tra rỗng nếu có gửi), `phone` (tái sử dụng `@ValidPhone`), `cccdImage` (tái sử dụng `@ValidImage`, nay optional) - không cho sửa `id`/`email`/`password`/`role`/`status`/`refreshToken`/`createdAt`
- `UserService.updateMyProfile(...)`/`UserServiceImpl`: kiểm tra `phone` trùng user khác qua `UserRepository.existsByPhoneAndIdNot(...)` (cho phép trùng chính mình); upload CCCD mới qua `FileStorageService.storeFile(...)` (tái sử dụng đúng logic Supabase từ signup) - chỉ cập nhật `cccdUrl` khi upload thành công; toàn bộ nằm trong 1 `@Transactional` nên nếu upload lỗi thì rollback, giữ nguyên `cccdUrl` cũ và cả các field khác chưa lưu, lỗi được `GlobalExceptionHandler` trả về kèm message rõ ràng
- `UserRepository.existsByPhoneAndIdNot(...)`: kiểm tra trùng số điện thoại loại trừ chính user hiện tại
- Message key `user.phone.exists` (en/vi)
- `UserControllerTest`: unit test xác nhận upload file không đúng định dạng JPEG/PNG/WEBP bị chặn ở tầng validation, trả về `400` kèm message rõ ràng và **không** gọi tới service/Supabase; kèm test happy-path cập nhật thành công

#### Changed

- `ValidPhone`: bỏ `@NotBlank` khỏi annotation gộp (chỉ giữ `@Pattern`, vốn coi `null` là hợp lệ) để tái sử dụng được cho field optional - đổi lại `SignupRequest` khai báo tường minh `@NotBlank` trên field `phone` để giữ nguyên hành vi bắt buộc khi đăng ký
- `ValidImage`/`ImageFileValidator`: thêm thuộc tính `required` (mặc định `true`, không đổi hành vi signup); dùng `required = false` cho `cccdImage` ở `PUT /me` - vẫn áp dụng đúng rule size/định dạng khi có file, chỉ bỏ qua khi không gửi file
- `UserServiceImpl`: tách `buildProfileResponse(...)` dùng chung giữa `getMyProfile` và `updateMyProfile`, tránh lặp code build response

#### Cách sử dụng `PUT /api/users/me`

1. `POST /api/auth/login` lấy `accessToken`, bấm **Authorize** trên Swagger UI (giống `GET /me`).
2. Mở `PUT /api/users/me` - 3 field: `name`, `phone` (text) và `cccdImage` (nút **Choose File**), tất cả optional. Điền field muốn đổi, để trống field muốn giữ nguyên rồi Execute.
3. Giải thích nút/checkbox **"Send empty value"** mà Swagger UI tự hiện cạnh mỗi field optional (do field không đánh dấu `required`):
   - **Không tick**, để trống field → Swagger **không đưa field đó vào request** → server nhận `null` → giữ nguyên giá trị cũ. Đây là cách test đúng "field không gửi thì giữ nguyên".
   - **Có tick** rồi để trống → Swagger vẫn gửi field lên với giá trị rỗng (`""` với `name`/`phone`) → server hiểu là "cố tình cập nhật thành rỗng" → trả lỗi validation `400` (`validation.name.required` / `validation.phone.invalid`). Dùng để test case lỗi "không được rỗng nếu có gửi".
   - Với `cccdImage` (kiểu file): tick hay không không tạo khác biệt nếu không chọn file - phần file rỗng luôn được coi là "không gửi", `cccdUrl` cũ được giữ nguyên.
4. Response trả `UserProfileResponse` mới nhất (không có `password`/`refreshToken`), `cccdUrl` là signed URL truy cập được ngay.
5. Test lỗi: `phone` trùng user khác → `409` (`user.phone.exists`); `phone` sai định dạng hoặc `name` rỗng khi có gửi → `400` kèm map lỗi theo field; ảnh sai định dạng/quá 5MB → `400`; không Authorize → `403`.

---

### 2026-08-22 - Create User Profile API (GET /api/users/me) (#99270)

**Người thực hiện:** Huỳnh Trương Thảo Duyên

#### Added

- Endpoint `GET /api/users/me`: lấy thông tin profile đầy đủ của user hiện tại dựa trên access token (không trả `password`/`refreshToken`)
- `UserController`, `UserProfileResponse` DTO, `UserService.getMyProfile(...)`/`UserServiceImpl`: lấy user thông qua `@AuthenticationPrincipal` (Spring Security context đã được `JwtAuthenticationFilter` xác thực sẵn), không tự parse lại JWT ở tầng controller/service
- Trả về URL đã ký (signed URL qua `FileStorageService.createSignedUrl`, hết hạn sau 3600 giây) cho ảnh CCCD (`cccdUrl`) và giấy phép kinh doanh (`businessLicenseUrl`) lưu trên Supabase Storage Private Bucket
- Claim `tokenType` (`access` / `refresh`) trong `JwtTokenProvider.generateAccessToken`/`generateRefreshToken` — phân biệt được access token và refresh token ngay trong payload JWT
- `JwtAuthenticationFilter`: bổ sung kiểm tra `jwtTokenProvider.isAccessToken(token)` — chặn refresh token bị dùng như access token trên **mọi** endpoint được bảo vệ, không chỉ riêng `/me`
- `UserMeSecurityIntegrationTest`: test tích hợp end-to-end (DB thật, filter chain thật, không mock) xác nhận: không có token → `403`; dùng refresh token → `403`; access token hợp lệ → `200` đúng user; sau khi logout dùng lại access token cũ → `403`; và HTTP session không được dùng để khôi phục danh tính khi request không có token hợp lệ
- Message key `user.profile.fetched` (en/vi)

#### Changed

- `AuthController`: thêm `@SecurityRequirements` rỗng cho `logout` — trước đó endpoint này vừa có tham số `Authorization` tường minh vừa được bảo vệ bởi security scheme `BearerAuth` toàn cục, khiến Swagger UI có thể tự động ghi đè token người dùng gõ vào ô tham số bằng token đang lưu ở nút Authorize
- `application.yml`: thêm `springdoc.swagger-ui.persist-authorization: false` — không lưu token Authorize qua các lần reload trang Swagger
- `JwtAuthenticationFilterTest`: cập nhật các test hiện có để khớp với check `isAccessToken` mới, bổ sung test case cho trường hợp refresh token bị từ chối

#### Fixed

- **[Bảo mật]** `SecurityConfig`: thêm `.securityContext(securityContext -> securityContext.securityContextRepository(new NullSecurityContextRepository()))`. Trước đây `SessionCreationPolicy.IF_REQUIRED` khiến Spring Security dùng `HttpSessionSecurityContextRepository` mặc định, tự lưu `SecurityContext` đã xác thực vào HTTP session — khiến các request sau có thể được xác thực lại từ cookie `JSESSIONID` cũ dù **không** gửi token, dù token đã bị blacklist sau logout, hoặc dù đã đổi sang token của user khác (bug được tái hiện và xác nhận bằng test trước khi sửa, và test pass sau khi sửa)

#### Cách sử dụng `GET /api/users/me`

1. `POST /api/auth/login` với email/mật khẩu hợp lệ → lấy `accessToken` trong response.
2. Trên Swagger UI: bấm nút **Authorize** (góc trên phải) → dán `accessToken` (không cần tiền tố `Bearer`, Swagger tự thêm) → Authorize → Close → Execute `/me` như bình thường.
3. Response mẫu:

   ```json
   {
     "code": 200,
     "message": "Lấy thông tin người dùng thành công",
     "data": {
       "id": 3,
       "name": "Nguyen Van A",
       "email": "user@example.com",
       "phone": "0912345678",
       "status": "ACTIVE",
       "isIdentityVerified": true,
       "isBusinessVerified": false,
       "language": "vi",
       "cccdUrl": "https://.../storage/v1/object/sign/coworking-space/cccd/uuid.jpg?token=...",
       "businessLicenseUrl": null,
       "roles": ["USER"]
     },
     "timestamp": "2026-08-22T10:00:00"
   }
   ```

5. Lưu ý:
   - Không gửi token, gửi refresh token, hoặc gửi access token đã logout → `403 Forbidden`.
   - `cccdUrl`/`businessLicenseUrl` là signed URL hết hạn sau 1 giờ — không nên cache lâu dài ở client, gọi lại `/me` để lấy URL mới khi cần.

---

### 2026-08-21 - Co-working Space Booking API

**Người thực hiện:** Nguyễn Minh An

#### Added

- Endpoint `POST /api/bookings` hỗ trợ đặt chỗ Co-working space với trạng thái khởi tạo `PENDING`
- Request DTO `BookingRequest` và Response DTO `BookingResponse` kèm MapStruct `BookingMapper`
- Enum `BookingStatus` (`PENDING`, `APPROVED`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `COMPLETED`) và `PriceUnit` (`HOUR`, `DAY`, `MONTH`)
- Cơ chế khóa chống ghi đè race condition (TOCTOU) bằng `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByIdForUpdate`) trên `SpaceRepository`
- Logic kiểm tra trùng lịch `existsActiveOverlap` loại trừ các booking đã bị `CANCELLED` hoặc `REJECTED` (chặn trùng lịch cả `PENDING` và `APPROVED`)
- Kiểm tra điều kiện thời gian hợp lệ (`startTime < endTime`, `startTime >= now`) và khung giờ hoạt động (`openTime`, `closeTime`) của Space
- Logic tính toán tổng tiền chính xác theo số phút thực tế cho các loại đơn vị giá `HOUR`, `DAY` và `MONTH` (làm tròn lên)
- Tiêm Spring `Clock` bean vào `BookingServiceImpl` hỗ trợ testability và chuẩn hóa timezone
- Unit test suite toàn diện cho `BookingServiceImplTest` và `BookingControllerTest`

### 2026-08-21 - Account Confirmation and Password Reset via OTP

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- Endpoint `POST /api/auth/confirm-account` xác thực mã OTP 6 chữ số và kích hoạt tài khoản (`INACTIVE` sang `ACTIVE`)
- Endpoint `POST /api/auth/reset-password` xác thực OTP và đổi mật khẩu mới (mã hóa bcrypt) cho tài khoản đang `ACTIVE`
- DTO `ConfirmAccountRequest` và `ResetPasswordRequest` kèm validation (định dạng email, OTP đúng 6 chữ số, độ dài mật khẩu)
- Phương thức `OtpService.confirmAccount(...)` và `OtpService.resetPassword(...)` bảo mật (so khớp hash OTP, kiểm tra hạn dùng theo Clock, xóa token sau khi sử dụng để chống tấn công replay)
- Method `OtpTokenRepository.findByUserAndPurpose(...)` hỗ trợ tra cứu OTP theo người dùng và mục đích
- Cơ chế giới hạn số lần nhập sai OTP: Hủy và vô hiệu hóa mã OTP ngay lập tức khi nhập sai >= 5 lần (`failed_attempts` trong entity `OtpToken`)
- Cơ chế Cooldown gửi OTP: Chặn và trả về HTTP 429 nếu yêu cầu gửi lại OTP trong vòng 60 giây kể từ lần gửi gần nhất
- Thu hồi và vô hiệu hóa toàn bộ JWT Token cũ khi đặt lại mật khẩu thành công thông qua `passwordChangedAt` và `TokenBlacklistService` trong `JwtAuthenticationFilter`
- Bổ sung cấu hình `app.otp.resend-cooldown-seconds` và `app.otp.max-failed-attempts` có thể tùy biến qua biến môi trường
- Thông điệp đa ngôn ngữ i18n tiếng Anh và tiếng Việt cho xác nhận tài khoản, đổi mật khẩu, cooldown và lỗi vượt quá số lần nhập sai OTP
- Unit test đầy đủ cho `OtpServiceImpl`, `AuthController`, `JwtAuthenticationFilter` bao gồm các trường hợp thành công, OTP hết hạn, cooldown, sai mã OTP, giới hạn số lần thử và thu hồi token

### 2026-08-20 - Change User Role

**Người thực hiện:** [Trần Trung Hiếu]

#### Added

- Chức năng cho phép `ADMIN` thay đổi role của user trong hệ thống
- Xử lý cập nhật quan hệ giữa user và role theo mô hình phân quyền RBAC
- Bổ sung logic service phục vụ chức năng thay đổi quyền người dùng
- Bổ sung xử lý truy cập dữ liệu role và quan hệ `user_roles`

#### Changed

- Cập nhật thông tin phân quyền của user sau khi Admin thay đổi role
- Giới hạn chức năng thay đổi role chỉ cho tài khoản có vai trò `ADMIN`

### 2026-08-20 - Booking Status Email Notification

**Người thực hiện:** [Kaio]

#### Added

- Template HTML Thymeleaf thông báo trạng thái Booking thay đổi
- `BookingService.changeStatus(...)` chuẩn hóa và lưu trạng thái trước khi gửi email cho người đặt
- `BookingRepository` tải sẵn user và space phục vụ nội dung email trong transaction
- Unit test cho nội dung template, trigger gửi email và trường hợp trạng thái không đổi

#### Changed

- Không lưu lại hoặc gửi email trùng khi trạng thái Booking không thay đổi

### 2026-08-20 - Sign Up and Password Reset Email Integration

**Người thực hiện:** [Kaio]

#### Added

- Endpoint `POST /api/auth/signup` tạo user `INACTIVE`, mã hóa mật khẩu, gán role `USER` và gửi OTP xác nhận
- Endpoint `POST /api/auth/forgot-password` gửi OTP reset cho tài khoản `ACTIVE` và luôn trả `202 Accepted`
- Template HTML Thymeleaf riêng cho email xác nhận tài khoản và reset password
- Unit test cho auth service, OTP reset, template email và controller

#### Changed

- Chuẩn hóa email trước khi tra cứu và dùng thời hạn OTP từ cấu hình trong nội dung email

#### Fixed

- Cho phép Spring Boot xử lý `/error` để validation và lỗi email của các API auth không bị chuyển thành `403 Forbidden`

### 2026-08-20 - Send Account Confirmation OTP API

**Người thực hiện:** [Kaio]

#### Added

- Endpoint `POST /api/auth/send-confirm` nhận email hợp lệ và trả `202 Accepted`
- Sinh OTP 6 chữ số bằng `SecureRandom`, hash trước khi lưu và hết hạn sau 5 phút
- Lưu OTP xác nhận theo user, thay thế OTP cũ và gửi mã qua `EmailService`
- Unit test cho OTP generator, service và controller

### 2026-08-20 - Base Email Service

**Người thực hiện:** [Kaio]

#### Added

- Cấu hình SMTP qua biến môi trường và file cấu hình local
- `EmailService` hỗ trợ gửi email plain text và HTML qua `JavaMailSender`
- Xử lý lỗi gửi mail tập trung bằng `EmailSendingException`
- Unit test cho nội dung email, validation và lỗi SMTP

### 2026-08-20 - Search & Filter Co-working spaces API

**Người thực hiện:** Nguyễn Minh An

#### Added

- `SpaceRepository` & `BookingRepository`: repository interfaces cho `Space` (hỗ trợ `JpaSpecificationExecutor`) và `Booking`
- `SpaceSearchRequest`: DTO tiếp nhận tham số tìm kiếm (`name`, `city`, `street`, `address`, `type`, `minPrice`, `maxPrice`, `priceUnit`, `openTime`, `closeTime`, `bookingStart`, `bookingEnd`, phân trang)
- `SpaceResponse`: DTO phản hồi thông tin chi tiết không gian và venue
- `PageResponse` & `ApiResponse`: DTO envelope bọc dữ liệu phản hồi và phân trang
- `SpaceSpecification`: JPA Specification xây dựng query động theo nhiều tiêu chí (tên, địa chỉ, loại không gian, khoảng giá, giờ hoạt động, loại trừ lịch trùng booking)
- `SpaceMapper`: MapStruct mapper chuyển đổi `Space` entity sang `SpaceResponse` DTO
- `SpaceService` & `SpaceServiceImpl`: xử lý logic nghiệp vụ tìm kiếm và phân trang
- `SpaceController`: REST API endpoint `GET /api/spaces/search`, áp dụng phân quyền `@PreAuthorize` (cho vai trò `USER`, `HOST`, `MODERATOR`, `ADMIN`) và tài liệu Swagger OpenAPI

---

### 2026-08-20 - Authentication and Authorization APIs (#99251)

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- `POST /api/auth/signup`: API đăng ký tài khoản kèm upload ảnh CCCD lên Supabase Storage (Private Bucket), chuẩn hóa họ tên, hash mật khẩu BCrypt, gán quyền `USER` và trạng thái `INACTIVE`
- `POST /api/auth/login`: API đăng nhập xác thực email/mật khẩu qua `AuthenticationManager`, trả về JWT Access Token (1 ngày) và Refresh Token (7 ngày)
- `POST /api/auth/logout`: API đăng xuất vô hiệu hóa token trên server với cơ chế Token Blacklist (SunLint S041) và dọn dẹp định kỳ `@Scheduled`
- `FileStorageService` & `FileStorageServiceImpl`: Service lưu trữ file nhị phân lên Supabase Storage Private Bucket qua REST API, sinh tên file ngẫu nhiên bằng UUID (SunLint S036)
- `TokenBlacklistService` & `TokenBlacklistServiceImpl`: Quản lý danh sách token đã đăng xuất bằng `ConcurrentHashMap` an toàn đa luồng
- `OpenApiConfig`: Cấu hình Swagger OpenAPI tích hợp nút Authorize (Bearer JWT)
- `ValidImage` & `ImageFileValidator`: Custom validator kiểm tra file ảnh không rỗng, đúng định dạng (JPEG, PNG, WEBP) và dung lượng <= 5MB
- `AppException` & `FileStorageException`: Custom Exception classes kế thừa từ `RuntimeException` theo chuẩn SunLint C030
- `UserStatus`: Enum định nghĩa 3 trạng thái của tài khoản người dùng: `ACTIVE`, `INACTIVE`, `BLOCKED`
- `AuthServiceImplTest`: 9 unit test kiểm thử toàn diện các luồng nghiệp vụ Signup, Login, Logout
- `AuthControllerTest`: 5 unit test kiểm thử Controller cho các endpoint `/signup`, `/login`, `/logout` với MockMvc

#### Changed

- `User`: Chuyển trường `status` từ kiểu `String` sang Enum `UserStatus` kèm `@Enumerated(EnumType.STRING)`
- `SignupResponse`: Cập nhật trường `status` sang kiểu Enum `UserStatus`
- `ValidPassword`: Tăng cường regex validation mật khẩu mạnh, bắt buộc chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt
- `messages_vi.properties` & `messages.properties`: Cập nhật thông báo định dạng số điện thoại chi tiết (đầu số 03, 05, 07, 08, 09) và thêm thông báo khóa tài khoản `auth.account.blocked`
- `AuthServiceImpl` & `CustomUserDetailsService`: Xử lý tài khoản có trạng thái `BLOCKED`, trả về HTTP 403 Forbidden và thông báo "Tài khoản của bạn đã bị khóa"
- `GlobalExceptionHandler`: Bổ sung handler cho `DisabledException` và `LockedException` để xử lý các tài khoản bị vô hiệu hóa / khóa
- `AuthController`: Bổ sung `@Parameter` mô tả chi tiết định dạng `Bearer <accessToken>` trên Swagger UI cho API Logout
- `ApiResponse`: Chuẩn hóa 2 phương thức `success` và 2 phương thức `error` cân đối, hỗ trợ mã HTTP status 201 cho Signup và 200 cho Login/Logout
- `application.yml`: Đổi port kết nối Supabase Pooler từ `5432` sang `6543` để giải quyết tình trạng bị chặn port hoặc ngắt kết nối
- `UserRepository`: Thêm `@EntityGraph(attributePaths = {"roles"})` tối ưu truy vấn nạp roles trong 1 câu SQL JOIN, ngăn ngừa `LazyInitializationException`

---


### 2026-08-20 - Unit test for JwtAuthenticationFilter

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- `JwtAuthenticationFilterTest`: 14 unit test cho `JwtAuthenticationFilter` dùng Mockito
  - Không có token / header sai prefix → SecurityContext rỗng, filter chain vẫn được gọi
  - Token không hợp lệ / hết hạn → không gọi `loadUserByUsername`, không set Authentication
  - Token hợp lệ → Authentication set đúng principal, authorities và credentials null
  - User nhiều role → tất cả role đều có trong Authentication

---

### 2026-08-20 - Fix CORS allowed-origins config format

**Người thực hiện:** [Huỳnh Trương Thảo Duyên]

#### Fixed

- `application.yml`: sửa `app.cors.allowed-origins` từ dạng YAML list sang chuỗi phân tách bởi dấu phẩy (`http://localhost:3000, http://localhost:5173`) — `SecurityConfig.corsAllowedOrigins` dùng `@Value("${app.cors.allowed-origins}")` bind vào `List<String>`, chỉ parse đúng khi giá trị là chuỗi comma-separated chứ không phải YAML list nhiều dòng

---

### 2026-08-20 - Security Layer (JWT + Spring Security)

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- `SecurityConfig`: cấu hình `SecurityFilterChain`, phân quyền URL, CORS, `BCryptPasswordEncoder`, `DaoAuthenticationProvider`
- `JwtTokenProvider`: generate/validate access token (1 ngày) và refresh token (7 ngày)
- `JwtAuthenticationFilter`: xác thực JWT mỗi request, set `SecurityContext`
- `CustomUserDetailsService`: load user từ DB, map `Set<Role>` → `List<GrantedAuthority>`
- `JwtProperties`: bind JWT config từ `application.yml` qua `@ConfigurationProperties`

#### Changed

- `application.yml`: thêm config `app.jwt` (secret từ env `JWT_SECRET`), `app.cors.allowed-origins`

---

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- Kết nối database Supabase (PostgreSQL) qua HikariCP
- Tạo 8 JPA Entity theo ERD thiết kế:
  - `Role`
  - `Amenity`
  - `User`
  - `Venue`
  - `Space`
  - `Booking`
  - `Payment`
  - `Message`
- Cấu hình i18n với ngôn ngữ mặc định tiếng Việt (`messages_vi.properties`)
- Cấu hình Swagger UI tại `/swagger-ui.html`

#### Changed

- Tên bảng `user` → `users` (tránh reserved keyword trong PostgreSQL)
- `ddl-auto: update` để Hibernate tự tạo/cập nhật bảng

#### Notes

- Bảng junction được JPA tự tạo: `user_roles`, `venue_amenities`, `space_host`
- Các thay đổi so với ERD gốc: xem [Entity Design Decisions](#entity-design-decisions)

---

## Entity Design Decisions

| # | Chỗ thay đổi | ERD gốc | Code thực tế | Lý do |
| --- | --- | --- | --- | --- |
| 1 | Bảng User | `user` | `users` | `user` là reserved keyword trong PostgreSQL |
| 2 | latitude / longitude | `decimal(10,8)` | `BigDecimal` | Tránh sai số float |
| 3 | description | `text` | `columnDefinition = "TEXT"` | JPA mặc định dùng VARCHAR(255) |
| 4 | capacity | `int` | `Integer` | Wrapper class hỗ trợ giá trị null |
| 5 | open_time / close_time | `time` | `LocalTime` | Java type mapping cho PostgreSQL time |
| 6 | Các timestamp | `timestamp` | `LocalDateTime` | Java type mapping cho PostgreSQL timestamp |
| 7 | payment.booking_id | FK | `@OneToOne` + `unique = true` | Đảm bảo ràng buộc 1-1 ở tầng DB |

---

_Template cho các lần cập nhật tiếp theo:_

```md
## [Unreleased]

### YYYY-MM-DD - [Tên tính năng]

**Người thực hiện:** [Tên thành viên]

#### Added

- ...

#### Changed

- ...

#### Fixed

- ...

#### Removed

- ...