# Kubernetes Manifests (mTLS + Observability)

This directory contains baseline manifests for mTLS-enabled gRPC and HTTP ingress.

- `cert-manager/`: ClusterIssuer + Certificate examples (short-lived mTLS certs).
- `core-server/`: Deployment/Service/Ingress with gRPC mTLS secret wiring.
- `device-gateway/`: Example gateway deployment with client certs.

Apply with your preferred tooling (kubectl, kustomize, Helm). Adjust namespaces and image names as needed.
