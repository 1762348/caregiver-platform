-- =============================================================
-- 优护 · 社区护工预约平台  V3 订单进度字段
-- 记录护工出发、到达、开始服务、完成的时间点，用于服务进度展示
-- =============================================================

ALTER TABLE orders ADD COLUMN departed_at DATETIME;
ALTER TABLE orders ADD COLUMN arrived_at DATETIME;
ALTER TABLE orders ADD COLUMN started_at DATETIME;
ALTER TABLE orders ADD COLUMN completed_at DATETIME;
ALTER TABLE orders ADD COLUMN cancel_reason VARCHAR(255);
