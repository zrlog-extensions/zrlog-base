CREATE TABLE IF NOT EXISTS `log_version`
(
    `id`              int(11) NOT NULL AUTO_INCREMENT,
    `log_id`          int(11) NOT NULL,
    `article_version` int(11) NOT NULL,
    `from_version`    int(11) NOT NULL,
    `patch_json`      longtext,
    `title`           varchar(255) DEFAULT NULL,
    `user_id`         int(11) DEFAULT NULL,
    `created_at`      datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `log_id_article_version` (`log_id`, `article_version`),
    KEY               `log_id` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;
