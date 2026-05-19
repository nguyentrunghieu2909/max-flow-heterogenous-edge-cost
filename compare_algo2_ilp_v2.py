import argparse
import csv
import random
import subprocess
import sys
from pathlib import Path


def run(cmd, cwd=None, input_text=None):
    p = subprocess.run(cmd, cwd=cwd, input=input_text, text=True, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(
            f"Command failed: {' '.join(map(str, cmd))}\nSTDOUT:\n{p.stdout}\nSTDERR:\n{p.stderr}"
        )
    return p.stdout


def parse_result(path):
    total = None
    per_source = {}
    mode = None

    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line:
                continue
            if line.startswith("TOTAL_FLOW"):
                total = int(line.split()[1])
                continue
            if line == "PER_SOURCE":
                mode = "per_source"
                continue
            if line in ("EDGE_FLOW_BY_SOURCE", "TOTAL_EDGE_FLOW", "WEIGHTED_EDGE_USAGE", "OPTIMAL"):
                mode = None
                continue
            if mode == "per_source":
                a, b = line.split()[:2]
                per_source[int(a)] = int(b)

    return total, per_source


def java_compile(workdir):
    run(["javac", "SensorNetworkGenerator.java", "SensorToFlowNetworkConverter.java", "Algorithm2MWFHMaxFlow.java"], cwd=workdir)


def generate_sensor(workdir, args, seed, sensor_file):
    cmd = [
        "java", "SensorNetworkGenerator",
        str(args.width), str(args.length), str(args.nodes), str(args.dgs), str(args.storage),
        str(args.min_data), str(args.max_data), str(args.min_storage), str(args.max_storage),
        str(args.min_energy), str(args.max_energy), str(args.min_cost), str(args.max_cost),
        str(args.min_priority), str(args.max_priority), str(args.range), str(seed), str(sensor_file)
    ]
    run(cmd, cwd=workdir)


def run_converter(workdir, sensor_file, flow_file, inf_cap):
    input_text = f"{sensor_file}\n{flow_file}\n{inf_cap}\n"
    run(["java", "SensorToFlowNetworkConverter"], cwd=workdir, input_text=input_text)


def run_algo2(workdir, flow_file, out_file):
    run(["java", "Algorithm2MWFHMaxFlow", str(flow_file), str(out_file)], cwd=workdir)


def run_ilp(workdir, flow_file, out_file):
    run([sys.executable, "ilp_mwfh_maxflow_v2.py", str(flow_file), str(out_file)], cwd=workdir)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs", type=int, default=3)
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--workdir", type=str, default=".")
    parser.add_argument("--width", type=int, default=100)
    parser.add_argument("--length", type=int, default=100)
    parser.add_argument("--nodes", type=int, default=20)
    parser.add_argument("--dgs", type=int, default=5)
    parser.add_argument("--storage", type=int, default=5)
    parser.add_argument("--min-data", type=int, default=3)
    parser.add_argument("--max-data", type=int, default=8)
    parser.add_argument("--min-storage", type=int, default=3)
    parser.add_argument("--max-storage", type=int, default=8)
    parser.add_argument("--min-energy", type=int, default=10)
    parser.add_argument("--max-energy", type=int, default=50)
    parser.add_argument("--min-cost", type=int, default=1)
    parser.add_argument("--max-cost", type=int, default=10)
    parser.add_argument("--min-priority", type=int, default=1)
    parser.add_argument("--max-priority", type=int, default=10)
    parser.add_argument("--range", type=float, default=35.0)
    parser.add_argument("--inf-cap", type=int, default=1000000000)
    args = parser.parse_args()

    workdir = Path(args.workdir).resolve()
    batch_seed = args.seed if args.seed is not None else random.SystemRandom().randint(1, 10**9)
    batch_dir = workdir / "batch_out" / f"batch_{batch_seed}"
    batch_dir.mkdir(parents=True, exist_ok=True)

    print("BATCH_BASE_SEED", batch_seed)
    print("OUTPUT_DIR", batch_dir)

    java_compile(workdir)

    rows = []
    for i in range(args.runs):
        seed = batch_seed + i
        print(f"RUN {i+1}/{args.runs} SEED {seed}")

        sensor_file = batch_dir / f"sensor_seed_{seed}.txt"
        flow_file = batch_dir / f"flow_seed_{seed}.txt"
        algo2_file = batch_dir / f"algo2_seed_{seed}.txt"
        ilp_file = batch_dir / f"ilp_seed_{seed}.txt"

        generate_sensor(workdir, args, seed, sensor_file)
        run_converter(workdir, sensor_file, flow_file, args.inf_cap)
        run_algo2(workdir, flow_file, algo2_file)
        run_ilp(workdir, flow_file, ilp_file)

        algo2_total, algo2_per = parse_result(algo2_file)
        ilp_total, ilp_per = parse_result(ilp_file)

        rows.append({
            "seed": seed,
            "algo2_total": algo2_total,
            "ilp_total": ilp_total,
            "match_total": algo2_total == ilp_total,
            "match_per_source": algo2_per == ilp_per,
            "algo2_per_source": str(algo2_per),
            "ilp_per_source": str(ilp_per),
            "sensor_file": sensor_file.name,
            "flow_file": flow_file.name,
            "algo2_file": algo2_file.name,
            "ilp_file": ilp_file.name,
        })

    summary_csv = batch_dir / "summary.csv"
    with open(summary_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    summary_txt = batch_dir / "summary.txt"
    with open(summary_txt, "w", encoding="utf-8") as f:
        f.write(f"BATCH_BASE_SEED {batch_seed}\n")
        f.write(f"RUNS {args.runs}\n\n")
        for row in rows:
            for k, v in row.items():
                f.write(f"{k.upper()} {v}\n")
            f.write("\n")

    print("WROTE", summary_csv)
    print("WROTE", summary_txt)


if __name__ == "__main__":
    main()
