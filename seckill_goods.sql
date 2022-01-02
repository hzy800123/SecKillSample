/*
 Navicat Premium Data Transfer

 Source Server         : LocalMySQL
 Source Server Type    : MySQL
 Source Server Version : 50735
 Source Host           : localhost:3306
 Source Schema         : micro_video

 Target Server Type    : MySQL
 Target Server Version : 50735
 File Encoding         : 65001

 Date: 03/01/2022 00:22:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for seckill_goods
-- ----------------------------
DROP TABLE IF EXISTS `seckill_goods`;
CREATE TABLE `seckill_goods` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `secKillGoodsId` varchar(255) NOT NULL,
  `stockCount` int(255) NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of seckill_goods
-- ----------------------------
BEGIN;
INSERT INTO `seckill_goods` VALUES (3, '20211230_RedPaper_Round1', 10, '2021-12-30 19:07:30');
INSERT INTO `seckill_goods` VALUES (5, '20211230_RedPaper_Round2', 0, '2021-12-30 22:38:26');
INSERT INTO `seckill_goods` VALUES (6, '20211230_RedPaper_Round3', 30, '2021-12-30 22:39:05');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
