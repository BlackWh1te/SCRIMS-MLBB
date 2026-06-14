-- DB-level time validation for scrims
-- Client-side validation is the primary defense; this is a safety net

-- 1. Prevent scrims scheduled in the past (by date)
-- Note: scheduled_date and scheduled_time are separate columns without explicit timezone.
-- The app stores them in the region's local time. This constraint catches obvious past dates.
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS valid_scheduled_date;
ALTER TABLE scrims ADD CONSTRAINT valid_scheduled_date
    CHECK (scheduled_date >= CURRENT_DATE);

-- 2. Prevent scrims scheduled more than 30 days in the future (by date)
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS valid_scheduled_date_max;
ALTER TABLE scrims ADD CONSTRAINT valid_scheduled_date_max
    CHECK (scheduled_date <= CURRENT_DATE + INTERVAL '30 days');
