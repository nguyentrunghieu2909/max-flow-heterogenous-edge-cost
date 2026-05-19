import sys
from collections import defaultdict
import gurobipy as gp
from gurobipy import GRB

class Inst:
    pass

def parse_list(line):
    p = line.strip().split()
    c = int(p[0])
    return list(map(int, p[1:1+c]))

def read_flow(path):
    inst = Inst()
    with open(path, 'r') as f:
        lines = [x.strip() for x in f if x.strip()]
    i = 0
    h = lines[i].split(); i += 1
    inst.flow_nodes = int(h[0]); inst.edge_count = int(h[1]); inst.original_count = int(h[2])
    inst.source_count = int(h[3]); inst.sink_count = int(h[4]); inst.s = int(h[5]); inst.t = int(h[6]); inst.inf = int(h[7])
    inst.source_primes = parse_list(lines[i]); i += 1
    inst.sink_doubles = parse_list(lines[i]); i += 1
    inst.originals = {}
    inst.prime_to_original = {}
    for _ in range(inst.original_count):
        p = lines[i].split(); i += 1
        oid, prime, dbl, typ, data, storage, energy, cost, priority = map(int, p[:9])
        inst.originals[oid] = {
            'prime': prime, 'double': dbl, 'type': typ, 'data': data,
            'storage': storage, 'energy': energy, 'cost': cost, 'priority': priority,
        }
        inst.prime_to_original[prime] = oid
    inst.edges = []
    inst.cap = {}
    inst.scalable = {}
    inst.in_edges = defaultdict(list)
    inst.out_edges = defaultdict(list)
    for j in range(i, len(lines)):
        u, v, c, scalable = map(int, lines[j].split()[:4])
        inst.edges.append((u, v))
        inst.cap[(u, v)] = c
        inst.scalable[(u, v)] = scalable
        inst.out_edges[u].append((u, v))
        inst.in_edges[v].append((u, v))
    return inst

def is_super_edge(inst, u, v):
    return (u == inst.s and v in inst.source_primes) or (v == inst.t and u in inst.sink_doubles)

def solve_maxflow(inst):
    m = gp.Model('mwfh_maxflow')
    K = list(inst.source_primes)
    x = {}
    d = {}
    cost = {}
    for k in K:
        o = inst.originals[inst.prime_to_original[k]]
        d[k] = o['data']
        cost[k] = o['cost']
    for k in K:
        for (u, v) in inst.edges:
            x[k, u, v] = m.addVar(vtype=GRB.INTEGER, lb=0, name=f'x_{k}_{u}_{v}')
    m.update()
    m.setObjective(gp.quicksum(x[k, inst.s, k] for k in K if (inst.s, k) in inst.cap), GRB.MAXIMIZE)
    for k in K:
        for v in range(inst.flow_nodes):
            if v == inst.s or v == inst.t:
                continue
            inflow = gp.quicksum(x[k, u, v] for (u, vv) in inst.in_edges[v])
            outflow = gp.quicksum(x[k, v, w] for (vv, w) in inst.out_edges[v])
            m.addConstr(inflow == outflow)
    for k in K:
        for j in K:
            if (inst.s, j) not in inst.cap:
                continue
            if j == k:
                m.addConstr(x[k, inst.s, j] <= d[k])
            else:
                m.addConstr(x[k, inst.s, j] == 0)
    for (u, v) in inst.edges:
        if is_super_edge(inst, u, v):
            m.addConstr(gp.quicksum(x[k, u, v] for k in K) <= inst.cap[(u, v)])
        else:
            m.addConstr(gp.quicksum(cost[k] * x[k, u, v] for k in K) <= inst.cap[(u, v)])
    m.optimize()
    return m, x

def write_result(inst, model, x, out_file):
    with open(out_file, 'w') as out:
        out.write('ILP MAXFLOW\n')
        if model.Status != GRB.OPTIMAL:
            out.write(f'STATUS {model.Status}\n')
            return
        out.write(f'TOTAL_FLOW {int(round(model.ObjVal))}\n')
        out.write('PER_SOURCE')
        for k in inst.source_primes:
            val = int(round(x[k, inst.s, k].X)) if (k, inst.s, k) in x else 0
            out.write(f' {k}:{val}')
        out.write('\n')

def main():
    if len(sys.argv) < 3:
        print('Usage: python ilp_mwfh_maxflow.py flow.txt output.txt')
        return
    inst = read_flow(sys.argv[1])
    model, x = solve_maxflow(inst)
    write_result(inst, model, x, sys.argv[2])
    if model.Status == GRB.OPTIMAL:
        print('ILP total flow =', int(round(model.ObjVal)))
    else:
        print('ILP status =', model.Status)

if __name__ == '__main__':
    main()
