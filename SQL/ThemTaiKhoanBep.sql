/*
    Tạo tài khoản đăng nhập cho vai trò Bep.
    Database: QuanLyCafe

    Thông tin đăng nhập sau khi chạy:
        Tên đăng nhập: bep
        Mật khẩu:       123
        Vai trò:        Bep

    Script có thể chạy lại nhiều lần mà không tạo tài khoản trùng.
*/

USE [QuanLyCafe];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @TenDangNhap NVARCHAR(50) = N'bep';
    DECLARE @MatKhau NVARCHAR(100) = N'123';
    DECLARE @VaiTro NVARCHAR(20) = N'Bep';
    DECLARE @MaTK INT;

    SELECT @MaTK = MaTK
    FROM dbo.TaiKhoan
    WHERE TenDangNhap = @TenDangNhap;

    IF @MaTK IS NULL
    BEGIN
        INSERT INTO dbo.TaiKhoan (TenDangNhap, MatKhau, VaiTro)
        VALUES (@TenDangNhap, @MatKhau, @VaiTro);

        SET @MaTK = CONVERT(INT, SCOPE_IDENTITY());
    END
    ELSE
    BEGIN
        UPDATE dbo.TaiKhoan
        SET MatKhau = @MatKhau,
            VaiTro = @VaiTro
        WHERE MaTK = @MaTK;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.NhanVien
        WHERE MaTK = @MaTK
    )
    BEGIN
        INSERT INTO dbo.NhanVien (HoTen, SoDienThoai, MaTK, LoaiHopDong)
        VALUES (N'Nhân viên bếp', NULL, @MaTK, N'FullTime');
    END;

    COMMIT TRANSACTION;

    SELECT
        tk.MaTK,
        tk.TenDangNhap,
        tk.VaiTro,
        nv.MaNV,
        nv.HoTen,
        N'Tạo/cập nhật tài khoản Bep thành công.' AS ThongBao
    FROM dbo.TaiKhoan tk
    INNER JOIN dbo.NhanVien nv ON nv.MaTK = tk.MaTK
    WHERE tk.MaTK = @MaTK;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO