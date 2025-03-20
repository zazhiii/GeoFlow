-- 创建数据库
CREATE DATABASE IF NOT EXISTS `geo_flow`;
USE `geo_flow`;

-- ================== 以下是创建表 ==================

-- 用户表
DROP TABLE IF EXISTS `user`;
create table user
(
    id              int AUTO_INCREMENT primary key comment '用户ID',
    username        varchar(64)  not null unique comment '用户名',
    password        varchar(128) not null comment '密码',
    avatar          varchar(128) comment '头像',
    email           varchar(128) unique comment '邮箱地址',
    phone_number    varchar(11)  unique comment '电话号码',
    last_login_time timestamp comment '上次登录时间',
    update_time     timestamp default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    create_time     timestamp default CURRENT_TIMESTAMP comment '创建时间'
) comment '用户表';


-- 文件信息表（file）
DROP TABLE IF EXISTS `geo_file`;
CREATE TABLE `geo_file` (
    `id` int AUTO_INCREMENT primary key COMMENT '文件ID',
    `user_id` int NOT NULL COMMENT '关联用户ID',
    `file_path` VARCHAR(500) NOT NULL COMMENT '完整存储路径',
    `file_size` BIGINT UNSIGNED NOT NULL COMMENT '文件大小(字节 Byte)',
    `file_type` VARCHAR(50) NOT NULL COMMENT '文件类型(MIME/扩展名)',
    `description` TEXT COMMENT '文件描述',
    `status` varchar(50) NOT NULL DEFAULT 'uploading' COMMENT '文件状态(uploading, completed, failed)',
    `upload_task_id` VARCHAR(36) COMMENT '上传任务ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间'
) COMMENT='文件信息表';

