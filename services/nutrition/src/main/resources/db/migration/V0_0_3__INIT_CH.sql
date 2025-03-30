create table IF NOT EXISTS nutrition.dish
(
    id Int64,
    name String,
    tdee UInt16 COMMENT 'calories per 100g',
    protein UInt16 COMMENT 'protein per 100g',
    fat UInt16 COMMENT 'fat per 100g',
    carbs UInt16 COMMENT 'carbs per 100g',
    type Enum('BREAKFAST' = 0, 'LUNCH' = 1, 'DINNER' = 2, 'LUNCH_OR_DINNER' = 3, 'ANY' = 4),
    photoId Nullable(Int64),
    recipeId Nullable(Int64)
) ENGINE = MergeTree()
ORDER BY (type)
;
