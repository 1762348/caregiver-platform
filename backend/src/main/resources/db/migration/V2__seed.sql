-- =============================================================
-- 优护 · 社区护工预约平台  V2 基础数据
-- 仅包含可上线的“字典/账号”数据；排班与订单由业务写入
-- 测试账号：13800000000(管理员) / 13800000001、13800000003(护工) / 13800000002(用户)
-- 验证码在开发环境固定为 123456（P2 接入短信 Mock）
-- =============================================================

-- ---------- 服务类别 ----------
INSERT INTO categories (id, code, name, description, base_price, status) VALUES
    (1, 'DAILY_CARE', '日常照料', '饮食起居、翻身拍背、日常陪伴', 200.00, 'ACTIVE'),
    (2, 'PROFESSIONAL_CARE', '专业护理', '术后护理、压疮护理、鼻饲导尿', 350.00, 'ACTIVE'),
    (3, 'REHAB_TRAINING', '康复训练', '肢体功能训练、语言康复、吞咽训练', 300.00, 'ACTIVE'),
    (4, 'ESCORT_SERVICE', '陪诊服务', '挂号取药、陪同检查、就医陪护', 150.00, 'ACTIVE'),
    (5, 'NIGHT_CARE', '夜间陪护', '夜间看护、起夜协助、突发应对', 280.00, 'ACTIVE');

-- ---------- 权限 ----------
INSERT INTO permissions (id, code, name, description) VALUES
    (1, 'user:view', '用户查看', '查看用户信息'),
    (2, 'user:edit', '用户编辑', '编辑用户信息'),
    (3, 'caregiver:view', '护工查看', '查看护工与排班信息'),
    (4, 'caregiver:edit', '护工编辑', '维护护工、排班与名额'),
    (5, 'order:view', '订单查看', '查看订单信息'),
    (6, 'order:edit', '订单编辑', '处理订单状态与退款'),
    (7, 'admin:all', '管理员全部权限', '系统配置与账号管理');

-- ---------- 角色权限 ----------
INSERT INTO role_permissions (role, permission_id) VALUES
    ('USER', 3),
    ('USER', 5),
    ('CAREGIVER', 3),
    ('CAREGIVER', 5),
    ('CAREGIVER', 6),
    ('ADMIN', 1),
    ('ADMIN', 2),
    ('ADMIN', 3),
    ('ADMIN', 4),
    ('ADMIN', 5),
    ('ADMIN', 6),
    ('ADMIN', 7);

-- ---------- 账号 ----------
INSERT INTO users (id, phone, name, role) VALUES
    (1, '13800000000', '平台管理员', 'ADMIN'),
    (2, '13800000001', '李秀兰', 'CAREGIVER'),
    (3, '13800000002', '张三', 'USER'),
    (4, '13800000003', '王芳', 'CAREGIVER');

-- ---------- 护工 ----------
INSERT INTO caregivers (id, user_id, code, name, gender, birth, phone, category_id, qualification, intro, price, status, rating, reviews) VALUES
    (1, 2, 'CG0001', '李秀兰', 'FEMALE', '1978-05-12', '13800000001', 1,
     '高级养老护理员证、健康证', '8 年居家照护经验，擅长失能老人日常照料与康复辅助。', 200.00, 'ACTIVE', 4.9, 128),
    (2, 4, 'CG0002', '王芳', 'FEMALE', '1983-09-03', '13800000003', 2,
     '护士执业证、老年照护专项技能证', '三甲医院护理背景，擅长术后护理、压疮护理与鼻饲。', 260.00, 'ACTIVE', 4.8, 96);

-- ---------- 基础时段 ----------
INSERT INTO caregiver_base_shifts (caregiver_id, start_time, end_time, price, status) VALUES
    (1, '08:00:00', '12:00:00', 200.00, 'ACTIVE'),
    (1, '14:00:00', '18:00:00', 220.00, 'ACTIVE'),
    (1, '19:00:00', '23:00:00', 260.00, 'ACTIVE'),
    (2, '08:00:00', '12:00:00', 260.00, 'ACTIVE'),
    (2, '14:00:00', '18:00:00', 280.00, 'ACTIVE');
