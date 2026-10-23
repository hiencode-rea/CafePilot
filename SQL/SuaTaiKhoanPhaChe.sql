/* Sửa tài khoản PhaChe bị thiếu nhân viên liên kết. */
USE [QuanLyCafe];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @TenDangNhap NVARCHAR(50) = N'nhanvien1';
    DECLARE @MaTK INT;

    SELECT @MaTK = MaTK
    FROM dbo.TaiKhoan
    WHERE TenDangNhap = @TenDangNhap;

    IF @MaTK IS NULL
        THROW 50001, N'Không tìm thấy tài khoản nhanvien1.', 1;

    UPDATE dbo.TaiKhoan
    SET MatKhau = N'123',
        VaiTro = N'PhaChe'
    WHERE MaTK = @MaTK;

    IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE MaTK = @MaTK)
    BEGIN
        INSERT INTO dbo.NhanVien (HoTen, SoDienThoai, MaTK, LoaiHopDong)
        VALUES (N'Nhân viên pha chế', NULL, @MaTK, N'FullTime');
    END
    ELSE
    BEGIN
        UPDATE dbo.NhanVien
        SET HoTen = N'Nhân viên pha chế'
        WHERE MaTK = @MaTK;
    END;

    COMMIT TRANSACTION;

    SELECT tk.MaTK, tk.TenDangNhap, tk.MatKhau, tk.VaiTro,
           nv.MaNV, nv.HoTen, nv.SoDienThoai, nv.LoaiHopDong
    FROM dbo.TaiKhoan tk
    INNER JOIN dbo.NhanVien nv ON nv.MaTK = tk.MaTK
    WHERE tk.MaTK = @MaTK;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO