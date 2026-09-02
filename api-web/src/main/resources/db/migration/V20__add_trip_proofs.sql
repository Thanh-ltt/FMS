CREATE TABLE trip_proofs (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),

    trip_id VARCHAR(255) NOT NULL,

    recipient_name VARCHAR(255),
    recipient_phone VARCHAR(255),

    signature_base64 TEXT,
    photo_urls TEXT,

    notes VARCHAR(1000),

    signed_at TIMESTAMP(6) NOT NULL,

    CONSTRAINT uk_trip_proofs_trip UNIQUE (trip_id),
    CONSTRAINT fk_trip_proofs_trip
        FOREIGN KEY (trip_id)
        REFERENCES trips(id)
);