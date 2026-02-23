CREATE TABLE roles (
  id UUID PRIMARY KEY,
  name VARCHAR(64) UNIQUE NOT NULL
);

CREATE TABLE users (
  id UUID PRIMARY KEY,
  username VARCHAR(128) UNIQUE NOT NULL,
  password_hash VARCHAR(256) NOT NULL,
  active BOOLEAN NOT NULL,
  created_at BIGINT NOT NULL
);

CREATE TABLE user_roles (
  user_id UUID REFERENCES users(id),
  role_id UUID REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE devices (
  id UUID PRIMARY KEY,
  serial_number VARCHAR(128) UNIQUE NOT NULL,
  firmware_version VARCHAR(64) NOT NULL,
  paired_at BIGINT,
  active BOOLEAN NOT NULL
);

CREATE TABLE device_pairings (
  id UUID PRIMARY KEY,
  device_id UUID REFERENCES devices(id),
  challenge VARCHAR(512) NOT NULL,
  response VARCHAR(512),
  status VARCHAR(32) NOT NULL,
  created_at BIGINT NOT NULL,
  completed_at BIGINT
);

CREATE TABLE device_health (
  id UUID PRIMARY KEY,
  device_id UUID REFERENCES devices(id),
  status VARCHAR(64) NOT NULL,
  details TEXT,
  created_at BIGINT NOT NULL
);

CREATE TABLE firmware_versions (
  id UUID PRIMARY KEY,
  version VARCHAR(64) UNIQUE NOT NULL,
  signed_hash VARCHAR(256) NOT NULL,
  created_at BIGINT NOT NULL
);

CREATE TABLE bioink_profiles (
  id UUID PRIMARY KEY,
  name VARCHAR(128) UNIQUE NOT NULL,
  manufacturer VARCHAR(128),
  viscosity_model TEXT NOT NULL,
  created_at BIGINT NOT NULL
);

CREATE TABLE bioink_batches (
  id UUID PRIMARY KEY,
  profile_id UUID REFERENCES bioink_profiles(id),
  lot_number VARCHAR(128) NOT NULL,
  manufacturer VARCHAR(128) NOT NULL,
  created_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL
);

CREATE TABLE print_jobs (
  id UUID PRIMARY KEY,
  device_id UUID REFERENCES devices(id),
  operator_id UUID REFERENCES users(id),
  bioink_batch_id UUID REFERENCES bioink_batches(id),
  created_at BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL
);

CREATE TABLE print_job_parameters (
  id UUID PRIMARY KEY,
  job_id UUID REFERENCES print_jobs(id),
  parameters_json TEXT NOT NULL
);

CREATE TABLE print_job_events (
  id UUID PRIMARY KEY,
  job_id UUID REFERENCES print_jobs(id),
  event_type VARCHAR(64) NOT NULL,
  payload_json TEXT NOT NULL,
  created_at BIGINT NOT NULL
);

CREATE TABLE telemetry_records (
  id UUID PRIMARY KEY,
  job_id UUID REFERENCES print_jobs(id),
  device_id UUID REFERENCES devices(id),
  timestamp BIGINT NOT NULL,
  payload_json TEXT NOT NULL
);

CREATE TABLE digital_twin_metrics (
  id UUID PRIMARY KEY,
  job_id UUID REFERENCES print_jobs(id),
  timestamp BIGINT NOT NULL,
  payload_json TEXT NOT NULL
);

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  job_id UUID REFERENCES print_jobs(id),
  actor_id UUID REFERENCES users(id),
  device_id UUID REFERENCES devices(id),
  event_type VARCHAR(64) NOT NULL,
  payload_hash VARCHAR(256) NOT NULL,
  hash VARCHAR(256) NOT NULL,
  prev_hash VARCHAR(256),
  timestamp BIGINT NOT NULL
);

CREATE TABLE hospital_integrations (
  id UUID PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  status VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL
);

CREATE TABLE integration_events (
  id UUID PRIMARY KEY,
  integration_id UUID REFERENCES hospital_integrations(id),
  event_type VARCHAR(64) NOT NULL,
  payload_json TEXT NOT NULL,
  created_at BIGINT NOT NULL
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_devices_serial ON devices(serial_number);
CREATE INDEX idx_device_health_device ON device_health(device_id);
CREATE INDEX idx_jobs_device ON print_jobs(device_id);
CREATE INDEX idx_jobs_operator ON print_jobs(operator_id);
CREATE INDEX idx_telemetry_job_time ON telemetry_records(job_id, timestamp);
CREATE INDEX idx_twin_job_time ON digital_twin_metrics(job_id, timestamp);
CREATE INDEX idx_audit_job_time ON audit_logs(job_id, timestamp);
CREATE INDEX idx_audit_device_time ON audit_logs(device_id, timestamp);
