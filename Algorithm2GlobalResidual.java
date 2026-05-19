
import java.io.*;
import java.util.*;

public class Algorithm2GlobalResidual {

    static class OriginalNode {

        int id, prime, dbl, type, data, storage, energy, cost, priority;
        double x, y;
    }

    static class InputEdge {

        int u, v, cap, scalable;

        InputEdge(int u, int v, int cap, int scalable) {
            this.u = u;
            this.v = v;
            this.cap = cap;
            this.scalable = scalable;
        }
    }

    static class FlowInput {

        int flowNodeCount, edgeCount, originalCount, sourceCount, sinkCount, superSource, superSink, INF;
        ArrayList<Integer> sourcePrimes = new ArrayList<>();
        ArrayList<Integer> sinkDoubles = new ArrayList<>();
        ArrayList<OriginalNode> originals = new ArrayList<>();
        ArrayList<InputEdge> edges = new ArrayList<>();
        HashMap<Integer, OriginalNode> byPrime = new HashMap<>();
    }

    static class ParentStep {

        int prevNode;
        int edgeIndex;
        int dir;
        int unitResidual;

        ParentStep(int prevNode, int edgeIndex, int dir, int unitResidual) {
            this.prevNode = prevNode;
            this.edgeIndex = edgeIndex;
            this.dir = dir;
            this.unitResidual = unitResidual;
        }
    }

    static class Result {

        int totalFlow;
        int[] sourceFlowByPrime;
        int[] weightedUsedByEdge;
    }

    static int[] parseList(String line) {
        String[] p = line.trim().split("\\s+");
        int c = Integer.parseInt(p[0]);
        int[] out = new int[c];
        for (int i = 0; i < c; i++) {
            out[i] = Integer.parseInt(p[i + 1]);
        }
        return out;
    }

    static FlowInput readFlow(String file) throws Exception {
        FlowInput in = new FlowInput();
        BufferedReader br = new BufferedReader(new FileReader(file));

        String[] h = br.readLine().trim().split("\\s+");
        in.flowNodeCount = Integer.parseInt(h[0]);
        in.edgeCount = Integer.parseInt(h[1]);
        in.originalCount = Integer.parseInt(h[2]);
        in.sourceCount = Integer.parseInt(h[3]);
        in.sinkCount = Integer.parseInt(h[4]);
        in.superSource = Integer.parseInt(h[5]);
        in.superSink = Integer.parseInt(h[6]);
        in.INF = Integer.parseInt(h[7]);

        for (int x : parseList(br.readLine())) {
            in.sourcePrimes.add(x);
        }
        for (int x : parseList(br.readLine())) {
            in.sinkDoubles.add(x);
        }

        for (int i = 0; i < in.originalCount; i++) {
            String[] p = br.readLine().trim().split("\\s+");
            OriginalNode o = new OriginalNode();
            int z = 0;
            o.id = Integer.parseInt(p[z++]);
            o.prime = Integer.parseInt(p[z++]);
            o.dbl = Integer.parseInt(p[z++]);
            o.type = Integer.parseInt(p[z++]);
            o.data = Integer.parseInt(p[z++]);
            o.storage = Integer.parseInt(p[z++]);
            o.energy = Integer.parseInt(p[z++]);
            o.cost = Integer.parseInt(p[z++]);
            o.priority = Integer.parseInt(p[z++]);
            o.x = Double.parseDouble(p[z++]);
            o.y = Double.parseDouble(p[z++]);
            in.originals.add(o);
            in.byPrime.put(o.prime, o);
        }

        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.trim().split("\\s+");
            in.edges.add(new InputEdge(
                    Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3])
            ));
        }

        br.close();
        return in;
    }

    static ArrayList<Integer>[] buildIncident(FlowInput in) {
        @SuppressWarnings("unchecked")
        ArrayList<Integer>[] incident = new ArrayList[in.flowNodeCount];
        for (int i = 0; i < in.flowNodeCount; i++) {
            incident[i] = new ArrayList<>();
        }
        for (int ei = 0; ei < in.edges.size(); ei++) {
            InputEdge e = in.edges.get(ei);
            incident[e.u].add(ei);
            incident[e.v].add(ei);
        }
        return incident;
    }

    static int unitsPerUse(InputEdge e, OriginalNode src) {
        return e.scalable == 1 ? src.cost : 1;
    }

    static int bfsAugment(
            FlowInput in,
            ArrayList<Integer>[] incident,
            int sourcePrime,
            int sourceRemaining,
            int[] weightedUsed
    ) {
        if (sourceRemaining <= 0) {
            return 0;
        }

        OriginalNode src = in.byPrime.get(sourcePrime);
        int n = in.flowNodeCount;
        int s = in.superSource;
        int t = in.superSink;

        boolean[] vis = new boolean[n];
        ParentStep[] parent = new ParentStep[n];
        Queue<Integer> q = new ArrayDeque<>();

        vis[s] = true;
        q.add(s);

        while (!q.isEmpty() && !vis[t]) {
            int u = q.poll();

            for (int ei : incident[u]) {
                InputEdge e = in.edges.get(ei);
                int need = unitsPerUse(e, src);

                if (u == e.u) {
                    int v = e.v;
                    if (e.u == s && e.v != sourcePrime) {
                        // only selected source may leave super source
                    } else {
                        int residual = e.cap - weightedUsed[ei];
                        int unitResidual = residual / need;
                        if (unitResidual > 0 && !vis[v]) {
                            vis[v] = true;
                            parent[v] = new ParentStep(u, ei, +1, unitResidual);
                            q.add(v);
                            if (v == t) {
                                break;
                            }
                        }
                    }
                }

                if (u == e.v) {
                    int v = e.u;
                    int unitResidual = weightedUsed[ei] / need;
                    if (unitResidual > 0 && !vis[v]) {
                        vis[v] = true;
                        parent[v] = new ParentStep(u, ei, -1, unitResidual);
                        q.add(v);
                        if (v == t) {
                            break;
                        }
                    }
                }
            }
        }

        if (!vis[t]) {
            return 0;
        }

        int deltaUnits = sourceRemaining;
        int cur = t;
        while (cur != s) {
            ParentStep st = parent[cur];
            deltaUnits = Math.min(deltaUnits, st.unitResidual);
            cur = st.prevNode;
        }

        cur = t;
        while (cur != s) {
            ParentStep st = parent[cur];
            InputEdge e = in.edges.get(st.edgeIndex);
            int need = unitsPerUse(e, src);
            int deltaWeighted = deltaUnits * need;

            if (st.dir == +1) {
                weightedUsed[st.edgeIndex] += deltaWeighted;
            } else {
                weightedUsed[st.edgeIndex] -= deltaWeighted;
            }

            cur = st.prevNode;
        }

        return deltaUnits;
    }

    static Result runGlobalResidual(FlowInput in) {
        ArrayList<OriginalNode> sources = new ArrayList<>();
        for (int prime : in.sourcePrimes) {
            sources.add(in.byPrime.get(prime));
        }
        sources.sort(Comparator.comparingInt(a -> a.cost));

        ArrayList<Integer>[] incident = buildIncident(in);

        int[] weightedUsed = new int[in.edges.size()];
        int[] sourceFlowByPrime = new int[in.flowNodeCount];
        int totalFlow = 0;

        for (OriginalNode src : sources) {
            int remaining = src.data - sourceFlowByPrime[src.prime];
            while (remaining > 0) {
                int delta = bfsAugment(in, incident, src.prime, remaining, weightedUsed);
                if (delta <= 0) {
                    break;
                }
                sourceFlowByPrime[src.prime] += delta;
                totalFlow += delta;
                remaining -= delta;
            }
        }

        Result r = new Result();
        r.totalFlow = totalFlow;
        r.sourceFlowByPrime = sourceFlowByPrime;
        r.weightedUsedByEdge = weightedUsed;
        return r;
    }

    static void writeResult(FlowInput in, Result r, String outFile) throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            out.println("ALGORITHM 2 GLOBAL RESIDUAL");
            out.println("TOTAL_FLOW " + r.totalFlow);
            out.println();

            out.println("PER_SOURCE");
            for (int prime : in.sourcePrimes) {
                out.println(prime + " " + r.sourceFlowByPrime[prime]);
            }
            out.println();

            out.println("TOTAL_EDGE_FLOW");
            for (int i = 0; i < in.edges.size(); i++) {
                if (r.weightedUsedByEdge[i] > 0) {
                    InputEdge e = in.edges.get(i);
                    out.println(e.u + " " + e.v + " " + r.weightedUsedByEdge[i]);
                }
            }
            out.println();

            out.println("WEIGHTED_EDGE_USAGE");
            for (int i = 0; i < in.edges.size(); i++) {
                if (r.weightedUsedByEdge[i] > 0) {
                    InputEdge e = in.edges.get(i);
                    out.println(e.u + " " + e.v + " " + r.weightedUsedByEdge[i] + " " + e.cap);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Algorithm2GlobalResidual flow.txt output.txt");
            return;
        }

        FlowInput in = readFlow(args[0]);
        Result r = runGlobalResidual(in);
        writeResult(in, r, args[1]);

        System.out.println("Global residual total flow = " + r.totalFlow);
    }
}
