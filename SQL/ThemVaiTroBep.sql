/* Thêm vai trò Bep vào ràng buộc phân quyền của bảng TaiKhoan. */
USE [QuanLyCafe];
GO

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.TaiKhoan')
      AND name = N'CK__TaiKhoan__VaiTro__25869641'
)
BEGIN
    ALTER TABLE dbo.TaiKhoan
    DROP CONSTRAINT [CK__TaiKhoan__VaiTro__25869641];
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.TaiKhoan')
      AND name = N'CK_TaiKhoan_VaiTro'
)
BEGIN
    ALTER TABLE dbo.TaiKhoan
    ADD CONSTRAINT [CK_TaiKhoan_VaiTro]
        CHECK ([VaiTro] IN (N'QuanLy', N'ThuNgan', N'PhaChe', N'PhucVu', N'Bep'));
END;
GO

SELECT cc.name AS TenRangBuoc, OBJECT_DEFINITION(cc.object_id) AS DinhNghia
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID(N'dbo.TaiKhoan');
GO