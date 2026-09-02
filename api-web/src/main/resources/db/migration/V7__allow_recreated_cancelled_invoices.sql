DO $$
DECLARE
    constraint_to_drop text;
BEGIN
    FOR constraint_to_drop IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON tc.constraint_name = kcu.constraint_name
            AND tc.table_schema = kcu.table_schema
        WHERE tc.table_schema = 'public'
            AND tc.table_name = 'invoices'
            AND tc.constraint_type = 'UNIQUE'
            AND kcu.column_name = 'trip_id'
    LOOP
        EXECUTE format('ALTER TABLE invoices DROP CONSTRAINT IF EXISTS %I', constraint_to_drop);
    END LOOP;
END $$;
