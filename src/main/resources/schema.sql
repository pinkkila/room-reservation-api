CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS room (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS reservation (
    id BIGSERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES room(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    CONSTRAINT start_before_end CHECK (start_time < end_time),
    CONSTRAINT reservation_overlap_excl EXCLUDE USING GIST (
        room_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    )
);
