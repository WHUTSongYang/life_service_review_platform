-- 用户表：手机号/邮箱、密码、昵称
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(32) UNIQUE,
  email VARCHAR(128) UNIQUE,
  password VARCHAR(200) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 管理员账号表：用户名、密码、昵称、是否超级管理员、是否启用
CREATE TABLE IF NOT EXISTS admin_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  super_admin TINYINT(1) NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 初始化超级管理员账号（若不存在）
INSERT INTO admin_accounts (username, password, nickname, super_admin, enabled)
SELECT 'superadmin', 'superadmin123', '超级管理员', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM admin_accounts WHERE username = 'superadmin');

-- 关注关系表：user_id 关注 follow_user_id
CREATE TABLE IF NOT EXISTS follows (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  follow_user_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_follow (user_id, follow_user_id)
);

-- 博客/动态表：用户发布的图文内容
CREATE TABLE IF NOT EXISTS blogs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  images TEXT,
  like_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 店铺表：名称、类型、店主、经纬度、评分、点评数
CREATE TABLE IF NOT EXISTS shops (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  type VARCHAR(50) NOT NULL,
  owner_user_id BIGINT,
  image VARCHAR(255),
  promotion TINYINT(1) NOT NULL DEFAULT 0,
  address VARCHAR(180),
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  avg_score DECIMAL(2,1) DEFAULT 0.0,
  review_count INT DEFAULT 0,
  CONSTRAINT fk_shops_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

SET @sql_owner_user_id = IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shops' AND COLUMN_NAME = 'owner_user_id') = 0,
  'ALTER TABLE shops ADD COLUMN owner_user_id BIGINT NULL',
  'SELECT 1'
);
PREPARE stmt_owner_user_id FROM @sql_owner_user_id;
EXECUTE stmt_owner_user_id;
DEALLOCATE PREPARE stmt_owner_user_id;

SET @sql_shop_image = IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shops' AND COLUMN_NAME = 'image') = 0,
  'ALTER TABLE shops ADD COLUMN image VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt_shop_image FROM @sql_shop_image;
EXECUTE stmt_shop_image;
DEALLOCATE PREPARE stmt_shop_image;

SET @sql_shop_promotion = IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shops' AND COLUMN_NAME = 'promotion') = 0,
  'ALTER TABLE shops ADD COLUMN promotion TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_shop_promotion FROM @sql_shop_promotion;
EXECUTE stmt_shop_promotion;
DEALLOCATE PREPARE stmt_shop_promotion;

-- 店铺分类表：编码、名称、排序号
CREATE TABLE IF NOT EXISTS shop_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL UNIQUE,
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'FOOD', '美食', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'FOOD');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'HOTEL', '酒店', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'HOTEL');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'ENTERTAINMENT', '娱乐', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'ENTERTAINMENT');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'MASSAGE', '按摩', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'MASSAGE');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'CINEMA', '电影院', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'CINEMA');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'FOOT_BATH', '足疗', 6, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'FOOT_BATH');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'BEAUTY', '丽人', 7, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'BEAUTY');

INSERT INTO shop_categories (code, name, sort_no, enabled)
SELECT 'SPORTS', '运动', 8, 1
WHERE NOT EXISTS (SELECT 1 FROM shop_categories WHERE code = 'SPORTS');

-- 店铺入驻申请表：申请人、店铺信息、审核状态
CREATE TABLE IF NOT EXISTS shop_apply_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  applicant_user_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  type VARCHAR(50) NOT NULL,
  image VARCHAR(255),
  address VARCHAR(180) NOT NULL,
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewer_user_id BIGINT,
  review_note TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME,
  CONSTRAINT fk_shop_apply_applicant FOREIGN KEY (applicant_user_id) REFERENCES users(id),
  CONSTRAINT fk_shop_apply_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users(id)
);

SET @sql_apply_image = IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_apply_requests' AND COLUMN_NAME = 'image') = 0,
  'ALTER TABLE shop_apply_requests ADD COLUMN image VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt_apply_image FROM @sql_apply_image;
EXECUTE stmt_apply_image;
DEALLOCATE PREPARE stmt_apply_image;

-- 点评表：店铺、用户、内容、图片、评分、点赞数
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  images TEXT,
  score TINYINT NOT NULL,
  like_count INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reviews_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
  CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 点评评论表：对某条点评的回复
CREATE TABLE IF NOT EXISTS review_comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  review_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_comments_review FOREIGN KEY (review_id) REFERENCES reviews(id),
  CONSTRAINT fk_review_comments_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 店铺商品表：名称、价格、库存、描述、是否上架
CREATE TABLE IF NOT EXISTS shop_products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  description TEXT,
  image VARCHAR(255),
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_shop_products_shop FOREIGN KEY (shop_id) REFERENCES shops(id)
);

-- 商品订单表：商品、数量、金额、状态（待支付/已支付/已取消）
CREATE TABLE IF NOT EXISTS product_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  shop_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_user_product UNIQUE (user_id, product_id),
  CONSTRAINT fk_product_orders_product FOREIGN KEY (product_id) REFERENCES shop_products(id),
  CONSTRAINT fk_product_orders_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
  CONSTRAINT fk_product_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 点赞记录表：用户对点评或博客的点赞，target_type 区分目标类型
CREATE TABLE IF NOT EXISTS like_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_like (user_id, target_type, target_id)
);
