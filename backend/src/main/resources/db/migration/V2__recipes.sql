CREATE TABLE recipes (
  id UUID PRIMARY KEY,
  name VARCHAR(128) UNIQUE NOT NULL,
  description TEXT,
  parameters_json TEXT NOT NULL,
  active BOOLEAN NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL
);

CREATE INDEX idx_recipes_active ON recipes(active);
