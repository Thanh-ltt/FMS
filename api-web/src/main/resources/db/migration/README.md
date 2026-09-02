# Database schema workflow

Flyway is the only component allowed to change the FMS database schema.
Hibernate uses `ddl-auto=validate`, so it checks that the migrated schema
matches the JPA entities and fails startup when a migration is missing.

## Existing and new databases

- Existing databases keep their applied `V1` through `V17` history.
- A brand-new empty database starts from `B17__complete_schema.sql`.
- Future versioned migrations start at `V18` and run in both cases.

The old `V1__baseline_marker.sql` comment describes the historical setup. Do
not edit any applied migration just to update that comment, because Flyway
checks their stored checksums.

## Making a schema change

1. Change the JPA entity.
2. Add the next immutable migration, for example
   `V18__add_trip_delivery_proof.sql`.
3. Start the application or run the schema verification against a disposable
   database.
4. Commit the entity and migration together.

Never edit an already-applied `V` or `B` migration. Never switch Hibernate back
to `update`, `create`, or `create-drop`.

Example:

```sql
ALTER TABLE trips
    ADD COLUMN delivery_proof_url TEXT;
```
