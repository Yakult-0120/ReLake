-- ============================================================
-- ReLake - 源数据库初始化脚本
-- 模拟业务库，用于测试 CDC 数据采集
-- ============================================================

-- 创建 Canal 复制用户
CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;

-- 测试业务表：用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 测试业务表：订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PAID/SHIPPED/COMPLETED/CANCELLED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 测试业务表：商品表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 插入测试数据
INSERT INTO users (username, email, phone, status) VALUES
('张三', 'zhangsan@example.com', '13800000001', 1),
('李四', 'lisi@example.com', '13800000002', 1),
('王五', 'wangwu@example.com', '13800000003', 1),
('赵六', 'zhaoliu@example.com', '13800000004', 0);

INSERT INTO products (name, category, price, stock) VALUES
('华为Mate 60 Pro', '手机', 6999.00, 100),
('iPhone 15 Pro', '手机', 8999.00, 80),
('MacBook Pro 14', '笔记本', 14999.00, 50),
('ThinkPad X1', '笔记本', 9999.00, 30);

INSERT INTO orders (order_no, user_id, amount, status) VALUES
('ORD20240101001', 1, 16998.00, 'COMPLETED'),
('ORD20240101002', 2, 8999.00, 'PAID'),
('ORD20240101003', 1, 14999.00, 'PENDING'),
('ORD20240101004', 3, 6999.00, 'CANCELLED');
