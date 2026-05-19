import argparse
import csv
import subprocess
from pathlib import Path


def run(cmd, cwd=None):
    p = subprocess.run(cmd, cwd=cwd, text=True, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(f'Command failed: {cmd}\nSTDOUT:\n{p.stdout}\nSTDERR:\n{p.stderr}')
    return p.stdout


def parse_result(path):
    total = None
    per_source = {}
    with open(path, 'r') as f:
        for line in f:
            line = line.strip()
            if line.startswith('TOTAL_FLOW'):
                total = int(line.split()[1])
            elif line.startswith('PER_SOURCE'):
                parts = line.split()[1:]
                for p in parts:
                    a, b = p.split(':')
                    per_source[int(a)] = int(b)
    return total, per_source


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--runs', type=int, default=3)
    ap.add_argument('--seed', type=int, default=1)
    ap.add_argument('--workdir', type=str, default='.')
    ap.add_argument('--width', type=int, default=100)
    ap.add_argument('--length', type=int, default=100)
    ap.add_argument('--nodes', type=int, default=20)
    ap.add_argument('--dgs', type=int, default=5)
    ap.add_argument('--storage', type=int, default=5)
    ap.add_argument('--min-data', type=int, default=3)
    ap.add_argument('--max-data', type=int, default=8)
    ap.add_argument('--min-storage', type=int, default=3)
    ap.add_argument('--max-storage', type=int, default=8)
    ap.add_argument('--min-energy', type=int, default=10)
    ap.add_argument('--max-energy', type=int, default=50)
    ap.add_argument('--min-cost', type=int, default=1)
    ap.add_argument('--max-cost', type=int, default=10)
    ap.add_argument('--min-priority', type=int, default=1)
    ap.add_argument('--max-priority', type=int, default=10)
    ap.add_argument('--range', type=float, default=35)
    ap.add_argument('--inf', type=int, default=1000000000)
    args = ap.parse_args()

    workdir = Path(args.workdir)
    outdir = workdir / 'batch_out'
    outdir.mkdir(exist_ok=True)

    summary_rows = []
    print('Running batch comparison...')
    for i in range(args.runs):
        seed = args.seed + i
        sensor = outdir / f'sensor_{seed}.txt'
        flow = outdir / f'flow_{seed}.txt'
        algo2 = outdir / f'algo2_{seed}.txt'
        ilp = outdir / f'ilp_{seed}.txt'

        run([
            'java', 'SensorNetworkGenerator',
            str(args.width), str(args.length), str(args.nodes), str(args.dgs), str(args.storage),
            str(args.min_data), str(args.max_data), str(args.min_storage), str(args.max_storage),
            str(args.min_energy), str(args.max_energy), str(args.min_cost), str(args.max_cost),
            str(args.min_priority), str(args.max_priority), str(args.range), str(seed), str(sensor)
        ], cwd=workdir)
        run(['java', 'SensorToFlowNetworkConverter', str(sensor), str(flow), str(args.inf)], cwd=workdir)
        run(['java', 'Algorithm2MWFHMaxFlow', str(flow), str(algo2)], cwd=workdir)
        run(['python', 'ilp_mwfh_maxflow.py', str(flow), str(ilp)], cwd=workdir)

        algo2_total, algo2_src = parse_result(algo2)
        ilp_total, ilp_src = parse_result(ilp)
        match_total = algo2_total == ilp_total
        match_source = algo2_src == ilp_src
        summary_rows.append({
            'seed': seed,
            'algo2_total': algo2_total,
            'ilp_total': ilp_total,
            'match_total': match_total,
            'match_source': match_source,
        })
        print(f'seed={seed} algo2={algo2_total} ilp={ilp_total} match_total={match_total} match_source={match_source}')

    with open(outdir / 'summary.csv', 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['seed', 'algo2_total', 'ilp_total', 'match_total', 'match_source'])
        writer.writeheader()
        writer.writerows(summary_rows)

    with open(outdir / 'summary.txt', 'w') as f:
        for row in summary_rows:
            f.write(str(row) + '\n')

    print('Done. See batch_out/summary.csv and batch_out/summary.txt')


if __name__ == '__main__':
    main()
