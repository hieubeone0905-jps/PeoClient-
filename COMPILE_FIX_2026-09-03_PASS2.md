# Compile fix pass 2 — Minecraft 1.21.4

Đã sửa các lỗi compile được báo trong log Gradle mới nhất, chỉ thay mapping/API sai và không xóa chức năng gốc.

## AutoBlockReload.java
- `method_31560(...)` → `method_5745(...)` (Entity.raycast 1.21.4).
- `Vec3d.normalize()` intermediary → `method_1029()`.

## NukerBypassUltimateV2.java
- `BlockPos.toImmutable()` bỏ vì `BlockPos` (`class_2338`) đã là kiểu immutable; giữ nguyên target.
- `MathHelper.method_15340(float,...)` sai overload → `method_15348(float,float,float)` cho step-towards với float.
- `field_6228` sai/private → `method_24828()` để lấy onGround.
- `Vec3d.normalize()` → `method_1029()`.
- Packet gửi trực tiếp qua `ClientWorld.method_8522(Packet)` thay vì truy cập field private `field_6214` và method không tồn tại `method_52787`.

## PeoClient.java
- `class_2850` → `class_2885` (`PlayerInteractBlockC2SPacket` 1.21.4).
- Không truy cập `field_6214` private nữa.
- Gửi packet bằng `ClientWorld.method_8522(...)`.
- Giữ nguyên ghost-block recovery path và không xóa nó.

## Kiểm tra
- Không còn các symbol lỗi tương ứng với 17 lỗi trong log mới nhất.
- Đã kiểm tra cân bằng ngoặc Java bằng bộ phân tích lexical đơn giản.
- Đã thử `./gradlew compileJava --no-daemon`; môi trường hiện tại không có mạng nên Gradle wrapper không tải được Gradle 8.12.1 (`UnknownHostException: services.gradle.org`). Vì vậy không ghi nhận đây là build thành công.
