CREATE TABLE vehicle_locations (
    id VARCHAR(255) PRIMARY KEY,

    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),

    vehicle_id VARCHAR(255) NOT NULL,
    trip_id VARCHAR(255),

    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,

    recorded_at TIMESTAMP(6) NOT NULL,

    CONSTRAINT fk_vehicle_locations_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id),

    CONSTRAINT fk_vehicle_locations_trip
        FOREIGN KEY (trip_id)
        REFERENCES trips(id)
);

CREATE INDEX idx_vehicle_locations_vehicle
    ON vehicle_locations(vehicle_id);

CREATE INDEX idx_vehicle_locations_trip
    ON vehicle_locations(trip_id);

CREATE INDEX idx_vehicle_locations_recorded_at
    ON vehicle_locations(recorded_at);
