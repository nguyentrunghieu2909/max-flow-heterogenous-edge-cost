import sys
from collections import defaultdict
import gurobipy as gp
from gurobipy import GRB


class FlowInstance:
    def __init__(self):
        self.flow_node_count = 0
        self.flow_edge_count = 0
        self.original_node_count = 0
        self.source_prime_count = 0
        self.sink_double_prime_count = 0
        self.super_source = None
        self.super_sink = None
        self.inf_value = None

        self.source_primes = []
        self.sink_double_primes = []

        self.original_nodes = {}
        self.prime_to_original = {}

        self.edges = []
        self.edge_cap = {}
        self.out_edges = defaultdict(list)
        self.in_edges = defaultdict(list)


def _parse_list(line):
    parts = line.strip().split()
    count = int(parts[0])
    return list(map(int, parts[1:1 + count]))


def read_flow_file(path):
    inst = FlowInstance()

    with open(path, "r", encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]

    ptr = 0

    parts = lines[ptr].split()
    ptr += 1
    inst.flow_node_count = int(parts[0])
    inst.flow_edge_count = int(parts[1])
    inst.original_node_count = int(parts[2])
    inst.source_prime_count = int(parts[3])
    inst.sink_double_prime_count = int(parts[4])
    inst.super_source = int(parts[5])
    inst.super_sink = int(parts[6])
    inst.inf_value = int(parts[7])

    inst.source_primes = _parse_list(lines[ptr])
    ptr += 1

    inst.sink_double_primes = _parse_list(lines[ptr])
    ptr += 1

    for _ in range(inst.original_node_count):
        parts = lines[ptr].split()
        ptr += 1
        original_id = int(parts[0])
        prime_id = int(parts[1])
        double_prime_id = int(parts[2])
        node_type = int(parts[3])
        data = int(parts[4])
        storage = int(parts[5])
        energy = int(parts[6])
        cost = int(parts[7])
        priority = int(parts[8])

        inst.original_nodes[original_id] = {
            "prime": prime_id,
            "double_prime": double_prime_id,
            "type": node_type,
            "data": data,
            "storage": storage,
            "energy": energy,
            "cost": cost,
            "priority": priority,
        }
        inst.prime_to_original[prime_id] = original_id

    while ptr < len(lines):
        parts = lines[ptr].split()
        ptr += 1
        u = int(parts[0])
        v = int(parts[1])
        cap = int(parts[2])

        inst.edges.append((u, v))
        inst.edge_cap[(u, v)] = cap
        inst.out_edges[u].append((u, v))
        inst.in_edges[v].append((u, v))

    return inst


def is_super_edge(inst, u, v):
    if u == inst.super_source and v in inst.source_primes:
        return True
    if v == inst.super_sink and u in inst.sink_double_primes:
        return True
    return False


def build_model(inst):
    model = gp.Model("mwfh_maxflow_ilp")

    K = list(inst.source_primes)
    V = list(range(inst.flow_node_count))
    E = list(inst.edges)
    s = inst.super_source
    t = inst.super_sink

    d_k = {}
    c_k = {}

    for k in K:
        original_id = inst.prime_to_original[k]
        info = inst.original_nodes[original_id]
        d_k[k] = info["data"]
        c_k[k] = info["cost"]

    x = {}
    for k in K:
        for (u, v) in E:
            x[(k, u, v)] = model.addVar(vtype=GRB.INTEGER, lb=0, name=f"x_{k}_{u}_{v}")

    model.update()

    model.setObjective(
        gp.quicksum(x[(k, s, k)] for k in K if (s, k) in inst.edge_cap),
        GRB.MAXIMIZE
    )

    for k in K:
        for v in V:
            if v == s or v == t:
                continue
            inflow = gp.quicksum(x[(k, u, v)] for (u, _) in inst.in_edges[v])
            outflow = gp.quicksum(x[(k, v, w)] for (_, w) in inst.out_edges[v])
            model.addConstr(inflow == outflow, name=f"flow_cons_k{k}_v{v}")

    for k in K:
        for r in K:
            if (s, r) not in inst.edge_cap:
                continue
            if r == k:
                model.addConstr(x[(k, s, r)] <= d_k[k], name=f"src_cap_k{k}")
            else:
                model.addConstr(x[(k, s, r)] == 0, name=f"wrong_src_k{k}_r{r}")

    for (u, v) in E:
        if is_super_edge(inst, u, v):
            model.addConstr(
                gp.quicksum(x[(k, u, v)] for k in K) <= inst.edge_cap[(u, v)],
                name=f"cap_super_{u}_{v}"
            )
        else:
            model.addConstr(
                gp.quicksum(c_k[k] * x[(k, u, v)] for k in K) <= inst.edge_cap[(u, v)],
                name=f"cap_internal_{u}_{v}"
            )

    return model, x, c_k


def write_solution(inst, model, x, c_k, out_file):
    with open(out_file, "w", encoding="utf-8") as f:
        if model.Status != GRB.OPTIMAL:
            f.write(f"STATUS {model.Status}\n")
            return

        f.write("OPTIMAL\n")
        f.write(f"TOTAL_FLOW {int(round(model.ObjVal))}\n\n")

        f.write("PER_SOURCE\n")
        for k in inst.source_primes:
            val = 0
            if (k, inst.super_source, k) in x:
                val = int(round(x[(k, inst.super_source, k)].X))
            f.write(f"{k} {val}\n")

        f.write("\nEDGE_FLOW_BY_SOURCE\n")
        for (k, u, v), var in x.items():
            val = int(round(var.X))
            if val > 0:
                f.write(f"{k} {u} {v} {val}\n")

        f.write("\nTOTAL_EDGE_FLOW\n")
        for (u, v) in inst.edges:
            total = sum(int(round(x[(k, u, v)].X)) for k in inst.source_primes)
            if total > 0:
                f.write(f"{u} {v} {total}\n")

        f.write("\nWEIGHTED_EDGE_USAGE\n")
        for (u, v) in inst.edges:
            used = sum(c_k[k] * int(round(x[(k, u, v)].X)) for k in inst.source_primes)
            if used > 0:
                f.write(f"{u} {v} {used} {inst.edge_cap[(u, v)]}\n")


def main():
    if len(sys.argv) < 2:
        print("Usage: python ilp_mwfh_maxflow_v2.py flow.txt [output.txt]")
        return

    flow_file = sys.argv[1]
    out_file = sys.argv[2] if len(sys.argv) >= 3 else "ilp_result.txt"

    inst = read_flow_file(flow_file)
    model, x, c_k = build_model(inst)
    model.optimize()
    write_solution(inst, model, x, c_k, out_file)

    if model.Status == GRB.OPTIMAL:
        print("OPTIMAL")
        print("TOTAL_FLOW", int(round(model.ObjVal)))
        print("WROTE", out_file)
    else:
        print("STATUS", model.Status)


if __name__ == "__main__":
    main()
