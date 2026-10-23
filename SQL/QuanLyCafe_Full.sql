USE [master]
GO

/****** Object:  Database [QuanLyCafe]    Script Date: 08/10/2026 ******/
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'QuanLyCafe')
BEGIN
    CREATE DATABASE [QuanLyCafe]
    CONTAINMENT = NONE
    ON  PRIMARY 
    ( NAME = N'QuanLyCafe', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL15.SQLEXPRESS\MSSQL\DATA\QuanLyCafe.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
    LOG ON 
    ( NAME = N'QuanLyCafe_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL15.SQLEXPRESS\MSSQL\DATA\QuanLyCafe_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
    WITH CATALOG_COLLATION = DATABASE_DEFAULT
END
GO

ALTER DATABASE [QuanLyCafe] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [QuanLyCafe] SET MULTI_USER 
GO

USE [QuanLyCafe]
GO

-- 1. Bảng TaiKhoan
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[TaiKhoan]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[TaiKhoan](
    [MaTK] [int] IDENTITY(1,1) PRIMARY KEY,
    [TenDangNhap] [nvarchar](50) NOT NULL UNIQUE,
    [MatKhau] [nvarchar](100) NOT NULL,
    [VaiTro] [nvarchar](20) NOT NULL
)
END
GO

-- 2. Bảng NhanVien
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[NhanVien]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[NhanVien](
    [MaNV] [int] IDENTITY(1,1) PRIMARY KEY,
    [HoTen] [nvarchar](100) NOT NULL,
    [SoDienThoai] [varchar](15),
    [MaTK] [int] NOT NULL,
    [LoaiHopDong] [nvarchar](50),
    CONSTRAINT [FK_NhanVien_TaiKhoan] FOREIGN KEY([MaTK]) REFERENCES [dbo].[TaiKhoan]([MaTK])
)
END
GO

-- 3. Bảng SanPham
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[SanPham]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[SanPham](
    [MaSP] [int] IDENTITY(1,1) PRIMARY KEY,
    [TenSP] [nvarchar](100) NOT NULL,
    [NhomSP] [nvarchar](50),
    [GiaBan] [float] NOT NULL,
    [Size] [nvarchar](20),
    [MoTa] [nvarchar](255),
    [LoaiXuLy] [nvarchar](20) NULL
)
END
GO

-- 4. Bảng ChiTietCombo
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ChiTietCombo]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[ChiTietCombo](
    [MaSP] [int] NOT NULL,
    [MaSPThanhPhan] [int] NOT NULL,
    [SoLuong] [int] NOT NULL,
    CONSTRAINT [PK_ChiTietCombo] PRIMARY KEY ([MaSP], [MaSPThanhPhan]),
    CONSTRAINT [FK_Combo_SP] FOREIGN KEY([MaSP]) REFERENCES [dbo].[SanPham]([MaSP]),
    CONSTRAINT [FK_Combo_ThanhPhan] FOREIGN KEY([MaSPThanhPhan]) REFERENCES [dbo].[SanPham]([MaSP])
)
END
GO

-- 5. Bảng KhuVuc
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[KhuVuc]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[KhuVuc](
    [MaKV] [int] IDENTITY(1,1) PRIMARY KEY,
    [TenKV] [nvarchar](50) NOT NULL
)
END
GO

-- 6. Bảng Ban
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Ban]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[Ban](
    [MaBan] [int] IDENTITY(1,1) PRIMARY KEY,
    [MaKV] [int] NOT NULL,
    [SoHieuBan] [nvarchar](10) NOT NULL,
    [TrangThai] [nvarchar](20) DEFAULT N'Trong',
    [SucChua] [int] DEFAULT 4,
    CONSTRAINT [FK_Ban_KhuVuc] FOREIGN KEY([MaKV]) REFERENCES [dbo].[KhuVuc]([MaKV])
)
END
GO

-- Bổ sung tọa độ để cho phép kéo thả bàn trên sơ đồ mặt bằng.
IF COL_LENGTH('dbo.Ban', 'ViTriX') IS NULL
BEGIN
    ALTER TABLE [dbo].[Ban] ADD [ViTriX] [float] NULL, [ViTriY] [float] NULL
END
GO

-- 7. Bảng DonHang
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[DonHang]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[DonHang](
    [MaDH] [int] IDENTITY(1,1) PRIMARY KEY,
    [MaBan] [int] NOT NULL,
    [MaNV] [int] NOT NULL,
    [ThoiGianTao] [datetime] DEFAULT GETDATE(),
    [TrangThai] [nvarchar](20) DEFAULT N'DangXuLy',
    [TongTien] [float] DEFAULT 0,
    [YeuCauThanhToan] [bit] DEFAULT 0,
    [ThoiGianYeuCau] [datetime] NULL,
    CONSTRAINT [FK_DonHang_Ban] FOREIGN KEY([MaBan]) REFERENCES [dbo].[Ban]([MaBan]),
    CONSTRAINT [FK_DonHang_NhanVien] FOREIGN KEY([MaNV]) REFERENCES [dbo].[NhanVien]([MaNV])
)
END
GO

-- Bổ sung số người hiện tại trong đơn mở để hiển thị số ghế đang có khách.
IF COL_LENGTH('dbo.DonHang', 'SoNguoi') IS NULL
BEGIN
    ALTER TABLE [dbo].[DonHang] ADD [SoNguoi] [int] NOT NULL CONSTRAINT [DF_DonHang_SoNguoi] DEFAULT 0
END
GO

-- 8. Bảng ChiTietDonHang
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ChiTietDonHang]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[ChiTietDonHang](
    [MaDH] [int] NOT NULL,
    [MaSP] [int] NOT NULL,
    [SoLuong] [int] NOT NULL,
    [DonGia] [float] NOT NULL,
    [DaThongBaoBep] [bit] DEFAULT 0,
    [GhiChu] [nvarchar](255) NULL,
    [TrangThaiMon] [nvarchar](20) NOT NULL DEFAULT N'CHUA_XU_LY',
    CONSTRAINT [PK_ChiTietDonHang] PRIMARY KEY ([MaDH], [MaSP]),
    CONSTRAINT [FK_CTDH_DonHang] FOREIGN KEY([MaDH]) REFERENCES [dbo].[DonHang]([MaDH]),
    CONSTRAINT [FK_CTDH_SanPham] FOREIGN KEY([MaSP]) REFERENCES [dbo].[SanPham]([MaSP])
)
END
GO

-- 9. Bảng CaLamViec
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[CaLamViec]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[CaLamViec](
    [MaCa] [int] IDENTITY(1,1) PRIMARY KEY,
    [MaNV] [int] NOT NULL,
    [Ngay] [date] NOT NULL,
    [GioBatDau] [time](0) NOT NULL,
    [GioKetThuc] [time](0) NOT NULL,
    CONSTRAINT [FK_CaLamViec_NhanVien] FOREIGN KEY([MaNV]) REFERENCES [dbo].[NhanVien]([MaNV])
)
END
GO

-- 10. Bảng ChamCong
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[ChamCong]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[ChamCong](
    [MaCC] [int] IDENTITY(1,1) PRIMARY KEY,
    [MaCa] [int] NOT NULL,
    [GioVao] [datetime] NOT NULL,
    [GioRa] [datetime] NULL,
    CONSTRAINT [FK_ChamCong_CaLamViec] FOREIGN KEY([MaCa]) REFERENCES [dbo].[CaLamViec]([MaCa])
)
END
GO

-- 11. Bảng Luong
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Luong]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[Luong](
    [MaLuong] [int] IDENTITY(1,1) PRIMARY KEY,
    [MaNV] [int] NOT NULL,
    [Thang] [int] NOT NULL,
    [Nam] [int] NOT NULL,
    [TongGioLam] [float] DEFAULT 0,
    [DonGiaGio] [float] DEFAULT 0,
    [LuongCoBan] [float] DEFAULT 0,
    [PhuCap] [float] DEFAULT 0,
    [Thuong] [float] DEFAULT 0,
    [Phat] [float] DEFAULT 0,
    [ThucLinh] [float] DEFAULT 0,
    CONSTRAINT [FK_Luong_NhanVien] FOREIGN KEY([MaNV]) REFERENCES [dbo].[NhanVien]([MaNV])
)
END
GO

-- 12. Bảng LichSuHuyMon
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[LichSuHuyMon]') AND type in (N'U'))
BEGIN
CREATE TABLE [dbo].[LichSuHuyMon](
    [MaLichSu] [int] IDENTITY(1,1) PRIMARY KEY,
    [TenSP] [nvarchar](100) NOT NULL,
    [SoLuong] [int] NOT NULL,
    [LyDo] [nvarchar](255),
    [NguoiThucHien] [nvarchar](100),
    [ThoiGian] [datetime] DEFAULT GETDATE()
)
END
GO

-- Dữ liệu mẫu ban đầu
INSERT INTO [dbo].[TaiKhoan] ([TenDangNhap], [MatKhau], [VaiTro]) VALUES (N'admin', N'123', N'QuanLy');
INSERT INTO [dbo].[TaiKhoan] ([TenDangNhap], [MatKhau], [VaiTro]) VALUES (N'thunngan', N'123', N'ThuNgan');
INSERT INTO [dbo].[TaiKhoan] ([TenDangNhap], [MatKhau], [VaiTro]) VALUES (N'nhanvien1', N'123', N'PhaChe');
INSERT INTO [dbo].[TaiKhoan] ([TenDangNhap], [MatKhau], [VaiTro]) VALUES (N'nhanvien2', N'123', N'PhucVu');
INSERT INTO [dbo].[TaiKhoan] ([TenDangNhap], [MatKhau], [VaiTro]) VALUES (N'bep', N'123', N'Bep');
GO