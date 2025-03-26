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
    breakfast_weight bigint,
    lunch            bigint,
    lunch_weight     bigint,
    dinner           bigint,
    dinner_weight    bigint,
    total_tdee       int,
    total_protein    int,
    total_fat        int,
    total_carbs      int
);

create index IF NOT EXISTS history_search on nutrition.history (login, date)
