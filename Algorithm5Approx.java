
import java.io.*;
import java.util.*;

public class Algorithm5Approx {

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

    static class ResEdge {

        int to, rev, cap, inputIndex;

        ResEdge(int to, int rev, int cap, int inputIndex) {
            this.to = to;
            this.rev = rev;
            this.cap = cap;
            this.inputIndex = inputIndex;
        }
    }

    static class Result {

        int totalFlow;
        long totalWeight;
        int[] sourceFlow;
        int[] edgeUse;
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
            in.edges.add(new InputEdge(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
        }
        br.close();
        return in;
    }

    static void addEdge(ArrayList<ResEdge>[] g, int u, int v, int cap, int inputIndex) {
        ResEdge a = new ResEdge(v, g[v].size(), cap, inputIndex);
        ResEdge b = new ResEdge(u, g[u].size(), 0, -1);
        g[u].add(a);
        g[v].add(b);
    }

    static int[] originalCaps(FlowInput in) {
        int[] c = new int[in.edges.size()];
        for (int i = 0; i < c.length; i++) {
            c[i] = in.edges.get(i).cap;

        }
        return c;
    }

    static int[] scaledCaps(FlowInput in, int[] current, OriginalNode source) {
        int[] out = new int[current.length];
        for (int i = 0; i < current.length; i++) {
            out[i] = in.edges.get(i).scalable == 1 ? current[i] / source.cost : current[i];

        }
        return out;
    }

    static void applyRound(FlowInput in, int[] current, int[] used, int cost) {
        for (int i = 0; i < current.length; i++) {
            if (used[i] == 0) {
                continue;

            }
            current[i] -= in.edges.get(i).scalable == 1 ? used[i] * cost : used[i];
            if (current[i] < 0) {
                current[i] = 0;

            }
        }
    }

    static Result maxFlowSingleSource(FlowInput in, int[] caps, int sourcePrime) {
        @SuppressWarnings("unchecked")
        ArrayList<ResEdge>[] g = new ArrayList[in.flowNodeCount];
        for (int i = 0; i < in.flowNodeCount; i++) {
            g[i] = new ArrayList<>();

        }
        for (int i = 0; i < in.edges.size(); i++) {
            InputEdge e = in.edges.get(i);
            int cap = caps[i];
            if (e.u == in.superSource && e.v != sourcePrime) {
                cap = 0;

            }
            addEdge(g, e.u, e.v, cap, i);
        }
        int total = 0;
        int[] edgeUse = new int[in.edges.size()];
        while (true) {
            int[] pv = new int[in.flowNodeCount], pe = new int[in.flowNodeCount];
            Arrays.fill(pv, -1);
            Queue<Integer> q = new ArrayDeque<>();
            q.add(in.superSource);
            pv[in.superSource] = in.superSource;
            while (!q.isEmpty() && pv[in.superSink] == -1) {
                int u = q.poll();
                for (int i = 0; i < g[u].size(); i++) {
                    ResEdge e = g[u].get(i);
                    if (e.cap <= 0 || pv[e.to] != -1) {
                        continue;

                    }
                    pv[e.to] = u;
                    pe[e.to] = i;
                    q.add(e.to);
                    if (e.to == in.superSink) {
                        break;

                    }
                }
            }
            if (pv[in.superSink] == -1) {
                break;

            }
            int add = Integer.MAX_VALUE;
            for (int v = in.superSink; v != in.superSource; v = pv[v]) {
                add = Math.min(add, g[pv[v]].get(pe[v]).cap);

            }
            for (int v = in.superSink; v != in.superSource; v = pv[v]) {
                ResEdge e = g[pv[v]].get(pe[v]);
                e.cap -= add;
                g[e.to].get(e.rev).cap += add;
                if (e.inputIndex >= 0) {
                    edgeUse[e.inputIndex] += add;

                }
            }
            total += add;
        }
        Result r = new Result();
        r.totalFlow = total;
        r.edgeUse = edgeUse;
        r.sourceFlow = new int[in.flowNodeCount];
        r.sourceFlow[sourcePrime] = total;
        return r;
    }

    static Result runOrder(FlowInput in, ArrayList<OriginalNode> order) {
        int[] current = originalCaps(in);
        Result out = new Result();
        out.sourceFlow = new int[in.flowNodeCount];
        out.edgeUse = new int[in.edges.size()];
        for (OriginalNode s : order) {
            int[] scaled = scaledCaps(in, current, s);
            Result round = maxFlowSingleSource(in, scaled, s.prime);
            if (round.totalFlow < s.data) {
                continue; // all or nothing

            }
            out.totalFlow += round.totalFlow;
            out.totalWeight += (long) round.totalFlow * s.priority;
            out.sourceFlow[s.prime] = round.totalFlow;
            for (int i = 0; i < out.edgeUse.length; i++) {
                out.edgeUse[i] += round.edgeUse[i];
            }
            applyRound(in, current, round.edgeUse, s.cost);
        }
        return out;
    }

    static void writeResult(FlowInput in, Result r, String outFile) throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            out.println("ALGORITHM 5");
            out.println("TOTAL_FLOW " + r.totalFlow);
            out.println("TOTAL_WEIGHT " + r.totalWeight);
            out.print("PER_SOURCE");
            for (int prime : in.sourcePrimes) {
                out.print(" " + prime + ":" + r.sourceFlow[prime]);
            }
            out.println();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Algorithm5Approx flow.txt output.txt");
            return;
        }
        FlowInput in = readFlow(args[0]);
        ArrayList<OriginalNode> sources = new ArrayList<>();
        for (int p : in.sourcePrimes) {
            sources.add(in.byPrime.get(p));
        }
        ArrayList<OriginalNode> byPriority = new ArrayList<>(sources);
        byPriority.sort((a, b) -> Integer.compare(b.priority, a.priority));
        ArrayList<OriginalNode> byRatio = new ArrayList<>(sources);
        byRatio.sort((a, b) -> Double.compare((double) b.priority / b.cost, (double) a.priority / a.cost));
        Result r1 = runOrder(in, byPriority), r2 = runOrder(in, byRatio);
        Result best = r1.totalWeight >= r2.totalWeight ? r1 : r2;
        writeResult(in, best, args[1]);
        System.out.println("Algorithm 5 weight = " + best.totalWeight + ", flow = " + best.totalFlow);
    }
}
