CREATE TABLE IF NOT EXISTS beacons (
    id TEXT NOT NULL,
    ts BIGINT NOT NULL,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    batt INTEGER,
    model TEXT,
    sdk INTEGER,
    PRIMARY KEY (id, ts)
);

ALTER TABLE beacons ENABLE ROW LEVEL SECURITY;

-- Allows the Android app to push data
CREATE POLICY "Allow anon insert" ON beacons FOR INSERT TO anon WITH CHECK (true);

-- Allows the dashboard to pull data
CREATE POLICY "Allow anon select" ON beacons FOR SELECT TO anon USING (true);
