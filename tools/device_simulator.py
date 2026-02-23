import argparse
import asyncio
import os
import sys
import time
from pathlib import Path

import grpc
from grpc_tools import protoc

ROOT = Path(__file__).resolve().parents[1]
PROTO_DIR = ROOT / "firmware-protocol" / "src" / "main" / "proto"
GEN_DIR = Path(__file__).resolve().parent / "_gen"
GEN_DIR.mkdir(parents=True, exist_ok=True)

sys.path.insert(0, str(GEN_DIR))

PROTO_FILE = PROTO_DIR / "firmware.proto"

protoc.main([
    "",
    f"-I{PROTO_DIR}",
    f"--python_out={GEN_DIR}",
    f"--grpc_python_out={GEN_DIR}",
    str(PROTO_FILE),
])

import firmware_pb2
import firmware_pb2_grpc

async def telemetry_stream(job_id: str, device_id: str, rate_ms: int, host: str, port: int):
    async def request_iter():
        seq = 0
        while True:
            ts = int(time.time() * 1000)
            frame = firmware_pb2.TelemetryFrame(
                job_id=job_id,
                device_id=device_id,
                timestamp_ms=ts,
                pressure_kpa=120.0 + (seq % 5),
                displacement_um=10.0,
                flow_rate_ul_s=5.0,
                temperature_c=30.0,
                viscosity_pas=1.2,
                pid_p=1.0,
                pid_i=0.1,
                pid_d=0.01,
                mpc_horizon_ms=50,
                mpc_predicted_pressure_kpa=118.0,
            )
            seq += 1
            yield frame
            await asyncio.sleep(rate_ms / 1000.0)

    async with grpc.aio.insecure_channel(f"{host}:{port}") as channel:
        stub = firmware_pb2_grpc.FirmwareBridgeStub(channel)
        async for cmd in stub.TelemetryStream(request_iter()):
            print(f"[CMD] {cmd.command} {cmd.parameters_json}")

async def main():
    parser = argparse.ArgumentParser(description="NeoGenesis device telemetry simulator")
    parser.add_argument("--job-id", default=os.getenv("SIM_JOB_ID", "00000000-0000-0000-0000-000000000005"))
    parser.add_argument("--device-id", default=os.getenv("SIM_DEVICE_ID", "00000000-0000-0000-0000-000000000002"))
    parser.add_argument("--rate-ms", type=int, default=int(os.getenv("SIM_RATE_MS", "10")))
    parser.add_argument("--host", default=os.getenv("SIM_GRPC_HOST", "localhost"))
    parser.add_argument("--port", type=int, default=int(os.getenv("SIM_GRPC_PORT", "9090")))
    args = parser.parse_args()
    await telemetry_stream(args.job_id, args.device_id, args.rate_ms, args.host, args.port)

if __name__ == "__main__":
    asyncio.run(main())
