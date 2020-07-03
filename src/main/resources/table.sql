CREATE TABLE IF NOT EXISTS `players`
(
    `uuid`     varchar(255),
    `kills`    bigint(255),
    `deaths`   bigint(255),
    `xp`       bigint(255),
    `level`    bigint(255),
    `lastseen` DATETIME,
    PRIMARY KEY (`uuid`)
)
ENGINE = InnoDB
DEFAULT CHARSET = latin1;