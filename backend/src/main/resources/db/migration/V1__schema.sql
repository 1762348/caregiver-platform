-- =============================================================
-- 优护 · 社区护工预约平台  V1 初始库结构
-- 兼容：MySQL 8.0 / H2 2.x (MODE=MySQL)
-- 约定：
--   * 金额统一 DECIMAL(10,2)，评分 DECIMAL(2,1)
--   * 状态/枚举使用大写编码，中文文案由接口层翻译
--   * 时间字段不使用 ON UPDATE（由应用层维护 updated_at）
-- =============================================================

-- ---------- 用户 ----------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(11) NOT NULL,
    name VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    avatar VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'CAREGIVER', 'ADMIN'))
);

-- ---------- 服务类别 ----------
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    base_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_categories_code UNIQUE (code),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_categories_price CHECK (base_price >= 0)
);

-- ---------- 护工 ----------
CREATE TABLE caregivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    birth DATE,
    phone VARCHAR(11) NOT NULL,
    category_id BIGINT,
    qualification VARCHAR(1000),
    intro VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rating DECIMAL(2, 1) DEFAULT 5.0,
    reviews INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_caregivers_user UNIQUE (user_id),
    CONSTRAINT uk_caregivers_code UNIQUE (code),
    CONSTRAINT fk_caregivers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_caregivers_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_caregivers_gender CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_caregivers_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_caregivers_price CHECK (price >= 0),
    CONSTRAINT ck_caregivers_rating CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5))
);

-- ---------- 护工基础时段（排班模板） ----------
CREATE TABLE caregiver_base_shifts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    caregiver_id BIGINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_base_shifts_caregiver FOREIGN KEY (caregiver_id) REFERENCES caregivers (id) ON DELETE CASCADE,
    CONSTRAINT ck_base_shifts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_base_shifts_price CHECK (price >= 0)
);

-- ---------- 护工排班（可预约的具体日期时段） ----------
CREATE TABLE caregiver_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    caregiver_id BIGINT NOT NULL,
    shift_id BIGINT,
    service_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    total_quota INT NOT NULL DEFAULT 1,
    remaining_quota INT NOT NULL DEFAULT 1,
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_schedules_slot UNIQUE (caregiver_id, service_date, start_time),
    CONSTRAINT fk_schedules_caregiver FOREIGN KEY (caregiver_id) REFERENCES caregivers (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedules_shift FOREIGN KEY (shift_id) REFERENCES caregiver_base_shifts (id) ON DELETE SET NULL,
    CONSTRAINT ck_schedules_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_schedules_quota CHECK (total_quota > 0 AND remaining_quota >= 0 AND remaining_quota <= total_quota),
    CONSTRAINT ck_schedules_price CHECK (price >= 0)
);

CREATE INDEX idx_schedules_date ON caregiver_schedules (service_date, status);

-- ---------- 被照顾人 ----------
CREATE TABLE receivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    id_card VARCHAR(18) NOT NULL,
    phone VARCHAR(11),
    relation VARCHAR(20),
    health VARCHAR(1000),
    special VARCHAR(1000),
    gender VARCHAR(10),
    birth DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_receivers_user_id_card UNIQUE (user_id, id_card),
    CONSTRAINT fk_receivers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_receivers_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_receivers_id_card CHECK (CHAR_LENGTH(id_card) = 18)
);

-- ---------- 订单 ----------
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    caregiver_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    service_date DATE NOT NULL,
    service_time VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    pay_time DATETIME,
    cancel_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_no UNIQUE (order_no),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_receiver FOREIGN KEY (receiver_id) REFERENCES receivers (id),
    CONSTRAINT fk_orders_caregiver FOREIGN KEY (caregiver_id) REFERENCES caregivers (id),
    CONSTRAINT fk_orders_schedule FOREIGN KEY (schedule_id) REFERENCES caregiver_schedules (id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'IN_SERVICE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_orders_amount CHECK (amount >= 0)
);

CREATE INDEX idx_orders_user_status ON orders (user_id, status);
CREATE INDEX idx_orders_caregiver_status ON orders (caregiver_id, status);

-- ---------- 站内消息 ----------
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    to_user_id BIGINT,
    to_caregiver_id BIGINT,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    deeplink VARCHAR(100),
    is_read INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_user FOREIGN KEY (to_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_caregiver FOREIGN KEY (to_caregiver_id) REFERENCES caregivers (id) ON DELETE CASCADE,
    CONSTRAINT ck_messages_recipient CHECK (
        (to_user_id IS NULL AND to_caregiver_id IS NOT NULL)
        OR (to_user_id IS NOT NULL AND to_caregiver_id IS NULL)
    ),
    CONSTRAINT ck_messages_type CHECK (type IN ('ORDER', 'PAYMENT', 'SERVICE', 'SYSTEM')),
    CONSTRAINT ck_messages_read CHECK (is_read IN (0, 1))
);

CREATE INDEX idx_messages_user_read ON messages (to_user_id, is_read);
CREATE INDEX idx_messages_caregiver_read ON messages (to_caregiver_id, is_read);

-- ---------- 权限（RBAC） ----------
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE TABLE role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_permissions UNIQUE (role, permission_id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE,
    CONSTRAINT ck_role_permissions_role CHECK (role IN ('USER', 'CAREGIVER', 'ADMIN'))
);

-- ---------- 支付流水 ----------
CREATE TABLE payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_no VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME,
    raw_payload VARCHAR(2000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_no UNIQUE (payment_no),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_payments_channel CHECK (channel IN ('WECHAT', 'ALIPAY', 'MOCK')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0)
);

CREATE INDEX idx_payments_order ON payment_records (order_id);

-- ---------- 订单状态流转日志 ----------
CREATE TABLE order_status_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    operator_id BIGINT,
    operator_role VARCHAR(20),
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_logs_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_logs_order ON order_status_logs (order_id);
