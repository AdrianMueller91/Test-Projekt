CREATE TABLE note (
    id         UUID                     PRIMARY KEY,
    text       VARCHAR(500)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX note_created_at_idx ON note (created_at DESC);
