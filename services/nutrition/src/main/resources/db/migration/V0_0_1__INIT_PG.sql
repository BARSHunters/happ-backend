create UNLOGGED table IF NOT EXISTS nutrition.cache_ration
(
    query_id UUID,
    login    VARCHAR(255),
    wish     TEXT
);


create table IF NOT EXISTS nutrition.history
(
    login            VARCHAR(255) not null,
    date             date         not null,
    breakfast        bigint,
    breakfast_weight int,
    lunch            bigint,
    lunch_weight     int,
    dinner           bigint,
    dinner_weight    int,
    total_tdee       double precision,
    total_protein    double precision,
    total_fat        double precision,
    total_carbs      double precision
);

create index IF NOT EXISTS history_search on nutrition.history (login, date)
