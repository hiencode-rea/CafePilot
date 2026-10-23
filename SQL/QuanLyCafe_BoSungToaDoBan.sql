/*
    Migration: bổ sung tọa độ bàn để hỗ trợ kéo thả bàn trên mini-map.
    Chạy file này trên database QuanLyCafe bằng SQL Server Management Studio.
    Có thể chạy nhiều lần, không tạo thêm cột nếu cột đã tồn tại.
*/

USE [QuanLyCafe];
GO

IF OBJECT_ID(N'[dbo].[Ban]', N'U') IS NULL
BEGIN
    THROW 50001, N'Không tìm thấy bảng dbo.Ban trong database QuanLyCafe.', 1;
END
GO

IF COL_LENGTH(N'dbo.Ban', N'ViTriX') IS NULL
BEGIN
    ALTER TABLE [dbo].[Ban]
        ADD [ViTriX] [float] NULL;
END
GO

IF COL_LENGTH(N'dbo.Ban', N'ViTriY') IS NULL
BEGIN
    ALTER TABLE [dbo].[Ban]
        ADD [ViTriY] [float] NULL;
END
GO

/* Gán vị trí mặc định cho các bàn chưa có tọa độ.
   800x640 là kích thước vùng mini-map hiện tại. */
;WITH DanhSachBan AS
(
    SELECT
        MaBan,
        ROW_NUMBER() OVER (ORDER BY MaKV, SoHieuBan, MaBan) - 1 AS ThuTu
    FROM [dbo].[Ban]
    WHERE ViTriX IS NULL OR ViTriY IS NULL
)
UPDATE b
SET
    ViTriX = 24 + ((d.ThuTu % 3) * 276),
    ViTriY = 24 + (((d.ThuTu / 3) % 3) * 226)
FROM [dbo].[Ban] b
INNER JOIN DanhSachBan d ON d.MaBan = b.MaBan;
GO

/* Kiểm tra kết quả */
SELECT
    MaBan,
    MaKV,
    SoHieuBan,
    ViTriX,
    ViTriY
FROM [dbo].[Ban]
ORDER BY MaKV, SoHieuBan, MaBan;
GO