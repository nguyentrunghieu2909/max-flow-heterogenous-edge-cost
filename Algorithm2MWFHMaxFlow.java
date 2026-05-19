
import java.io.*;
import java.util.*;

public class Algorithm2MWFHMaxFlow {

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

    static class Result {

        int totalFlow;
        int[] sourceFlow;     // indexed by prime node id
        int[] edgeUse;        // indexed by input edge index
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

    static int[] originalCaps(FlowInput in) {
        int[] c = new int[in.edges.size()];
        for (int i = 0; i < in.edges.size(); i++) {
            c[i] = in.edges.get(i).cap;
        }
        return c;
    }

    static int[] scaledCaps(FlowInput in, int[] current, OriginalNode source) {
        int[] out = new int[current.length];
        for (int i = 0; i < current.length; i++) {
            if (in.edges.get(i).scalable == 1) {
                out[i] = current[i] / source.cost;
            } else {
                out[i] = current[i];
            }
        }
        return out;
    }

    static void applyRound(FlowInput in, int[] current, int[] used, int cost) {
        for (int i = 0; i < current.length; i++) {
            if (used[i] == 0) {
                continue;
            }

            if (in.edges.get(i).scalable == 1) {
                current[i] -= used[i] * cost;
            } else {
                current[i] -= used[i];
            }

            if (current[i] < 0) {
                current[i] = 0;
            }
        }
    }

    static Result maxFlowSingleSource(FlowInput in, int[] caps, int sourcePrime) {
        int n = in.flowNodeCount;
        int s = in.superSource;
        int t = in.superSink;

        int[][] cap = new int[n][n];
        int[][] flow = new int[n][n];
        @SuppressWarnings("unchecked")
        ArrayList<Integer>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
        }

        // build graph
        for (int i = 0; i < in.edges.size(); i++) {
            InputEdge e = in.edges.get(i);
            int c = caps[i];

            // only chosen source can receive flow from super source
            if (e.u == s && e.v != sourcePrime) {
                c = 0;
            }

            cap[e.u][e.v] += c;

            // add both directions in adjacency for residual traversal
            adj[e.u].add(e.v);
            adj[e.v].add(e.u);
        }

        int total = 0;
        int[] parent = new int[n];

        while (true) {
            Arrays.fill(parent, -2);
            Queue<Integer> q = new ArrayDeque<>();
            q.add(s);
            parent[s] = -1;

            while (!q.isEmpty() && parent[t] == -2) {
                int u = q.poll();
                for (int v : adj[u]) {
                    if (parent[v] != -2) {
                        continue;
                    }

                    // residual capacity > 0
                    if (cap[u][v] - flow[u][v] <= 0) {
                        continue;
                    }

                    parent[v] = u;
                    q.add(v);
                    if (v == t) {
                        break;
                    }
                }
            }

            if (parent[t] == -2) {
                break;
            }

            int delta = Integer.MAX_VALUE;
            int v = t;
            while (v != s) {
                int u = parent[v];
                delta = Math.min(delta, cap[u][v] - flow[u][v]);
                v = u;
            }

            v = t;
            while (v != s) {
                int u = parent[v];
                flow[u][v] += delta;
                flow[v][u] -= delta;
                v = u;
            }

            total += delta;
        }

        // extract final usage on original directed edges
        int[] edgeUse = new int[in.edges.size()];
        for (int i = 0; i < in.edges.size(); i++) {
            InputEdge e = in.edges.get(i);
            if (flow[e.u][e.v] > 0) {
                edgeUse[i] = flow[e.u][e.v];
            } else {
                edgeUse[i] = 0;
            }
        }

        Result r = new Result();
        r.totalFlow = total;
        r.edgeUse = edgeUse;
        r.sourceFlow = new int[in.flowNodeCount];
        r.sourceFlow[sourcePrime] = total;
        return r;
    }

    static Result runAlgorithm2(FlowInput in) {
        ArrayList<OriginalNode> sources = new ArrayList<>();
        for (int prime : in.sourcePrimes) {
            sources.add(in.byPrime.get(prime));
        }

        // non-decreasing order of cost
        sources.sort(Comparator.comparingInt(a -> a.cost));

        int[] current = originalCaps(in);

        Result out = new Result();
        out.totalFlow = 0;
        out.sourceFlow = new int[in.flowNodeCount];
        out.edgeUse = new int[in.edges.size()];

        for (OriginalNode src : sources) {
            int[] scaled = scaledCaps(in, current, src);
            Result round = maxFlowSingleSource(in, scaled, src.prime);

            out.totalFlow += round.totalFlow;
            out.sourceFlow[src.prime] = round.totalFlow;

            for (int i = 0; i < out.edgeUse.length; i++) {
                out.edgeUse[i] += round.edgeUse[i];
            }

            applyRound(in, current, round.edgeUse, src.cost);
        }

        return out;
    }

    static void writeResult(FlowInput in, Result r, String outFile) throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            out.println("ALGORITHM 2");
            out.println("TOTAL_FLOW " + r.totalFlow);
            out.println();

            out.println("PER_SOURCE");
            for (int prime : in.sourcePrimes) {
                out.println(prime + " " + r.sourceFlow[prime]);
            }
            out.println();

            out.println("EDGE_FLOW_BY_SOURCE");
            out.println();

            out.println("TOTAL_EDGE_FLOW");
            for (int i = 0; i < in.edges.size(); i++) {
                if (r.edgeUse[i] > 0) {
                    InputEdge e = in.edges.get(i);
                    out.println(e.u + " " + e.v + " " + r.edgeUse[i]);
                }
            }
            out.println();

            out.println("WEIGHTED_EDGE_USAGE");
            for (int i = 0; i < in.edges.size(); i++) {
                if (r.edgeUse[i] > 0) {
                    InputEdge e = in.edges.get(i);
                    out.println(e.u + " " + e.v + " " + r.edgeUse[i] + " " + e.cap);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Algorithm2MWFHMaxFlow flow.txt output.txt");
            return;
        }

        FlowInput in = readFlow(args[0]);
        Result r = runAlgorithm2(in);
        writeResult(in, r, args[1]);

        System.out.println("Algorithm 2 total flow = " + r.totalFlow);
    }
}
