-- 1. Add new column
ALTER TABLE restaurants
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- 2. Migrate old data
UPDATE restaurants
SET status = CASE
                 WHEN approved = true THEN 'APPROVED'
                 ELSE 'PENDING'
    END;

-- 3. Drop old column
ALTER TABLE restaurants
DROP COLUMN approved;