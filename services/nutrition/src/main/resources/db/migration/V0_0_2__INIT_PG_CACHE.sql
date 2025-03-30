create TYPE nutrition.Meal_Type AS ENUM ('BREAKFAST', 'LUNCH', 'DINNER');
create TYPE nutrition.Wish AS ENUM ('KEEP', 'GAIN', 'LOSS', 'REMAIN');

create UNLOGGED table IF NOT EXISTS nutrition.cache_ration
(
    query_id       UUID primary key,
    login          VARCHAR(255) not null,
    wish           nutrition.Wish,
    meal_type      nutrition.Meal_Type,
    activity_index float
);
