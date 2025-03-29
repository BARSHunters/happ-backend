create TYPE Meal_Type AS ENUM ('BREAKFAST', 'LUNCH', 'DINNER');
create TYPE Wish AS ENUM ('KEEP', 'GAIN', 'LOSS');

create UNLOGGED table IF NOT EXISTS nutrition.cache_ration
(
    query_id  UUID primary key,
    login     VARCHAR(255) not null,
    wish      Wish,
    meal_type Meal_Type
);
