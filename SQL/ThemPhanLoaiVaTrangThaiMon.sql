/* Migration cho hàng đợi Bếp/Pha chế và trạng thái món. */
USE [QuanLyCafe];
GO

IF COL_LENGTH('dbo.SanPham', 'LoaiXuLy') IS NULL
BEGIN
    ALTER TABLE dbo.SanPham ADD LoaiXuLy NVARCHAR(20) NULL;
END;
GO

IF COL_LENGTH('dbo.ChiTietDonHang', 'TrangThaiMon') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietDonHang ADD TrangThaiMon NVARCHAR(20) NOT NULL
        CONSTRAINT DF_ChiTietDonHang_TrangThaiMon DEFAULT N'CHUA_XU_LY';
END;
GO

/* Món đã báo bếp trước migration được đưa vào hàng đợi chờ làm. */
UPDATE dbo.ChiTietDonHang
SET TrangThaiMon = N'CHO_LAM'
WHERE DaThongBaoBep = 1 AND (TrangThaiMon IS NULL OR TrangThaiMon = N'CHUA_XU_LY');
GO

/* Phân loại mặc định theo nhóm hiện có; cần rà soát lại trong quản lý sản phẩm. */
UPDATE dbo.SanPham
SET LoaiXuLy = CASE
    WHEN LOWER(ISNULL(NhomSP, N'')) LIKE N'%nước%'
      OR LOWER(ISNULL(NhomSP, N'')) LIKE N'%nuoc%'
      OR LOWER(ISNULL(NhomSP, N'')) LIKE N'%cafe%'
      OR LOWER(ISNULL(NhomSP, N'')) LIKE N'%cà phê%'
      OR LOWER(ISNULL(NhomSP, N'')) LIKE N'%ca phe%'
      OR LOWER(ISNULL(NhomSP, N'')) LIKE N'%trà%'
      OR LOWER(ISNULL(NhomSP, N'')) LIKE N'%tra%'
    THEN N'PHA_CHE'
    ELSE N'BEP'
END
WHERE LoaiXuLy IS NULL OR LTRIM(RTRIM(LoaiXuLy)) = N'';
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_SanPham_LoaiXuLy')
BEGIN
    ALTER TABLE dbo.SanPham ADD CONSTRAINT CK_SanPham_LoaiXuLy
        CHECK (LoaiXuLy IN (N'BEP', N'PHA_CHE', N'KHONG_CAN_XU_LY'));
END;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_ChiTietDonHang_TrangThaiMon')
BEGIN
    ALTER TABLE dbo.ChiTietDonHang DROP CONSTRAINT CK_ChiTietDonHang_TrangThaiMon;
END;
GO

ALTER TABLE dbo.ChiTietDonHang ADD CONSTRAINT CK_ChiTietDonHang_TrangThaiMon
    CHECK (TrangThaiMon IN (N'CHUA_XU_LY', N'CHO_LAM', N'DANG_LAM', N'DA_RA_MON', N'HET_MON'));
GO