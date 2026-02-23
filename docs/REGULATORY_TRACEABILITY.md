# Regulatory Traceability

Evidence Suite:
- Every event produces a payload hash.
- Chain hash: H(i) = SHA256(timestamp|actor|device|job|eventType|payloadHash|prevHash).
- Audit logs stored immutably in DB and exported with hash manifest.

Evidence package includes:
- `manifest.json` (counts + metadata)
- `data.json` (events)
- `hash` (package integrity)
