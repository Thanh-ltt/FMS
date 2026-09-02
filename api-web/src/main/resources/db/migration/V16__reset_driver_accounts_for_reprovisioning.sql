-- Keep driver profiles and operational history, but remove all existing
-- DRIVER credentials so administrators can provision fresh accounts.
UPDATE drivers
SET user_id = NULL
WHERE user_id IS NOT NULL;

DELETE FROM users
WHERE role = 'DRIVER';
