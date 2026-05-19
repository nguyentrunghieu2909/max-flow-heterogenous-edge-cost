
import java.io.*;
import java.util.*;

public class SensorToFlowNetworkConverter {

    static class SensorNode {

        int id, type, data, storage, energy, cost, priority;
        double x, y;
        ArrayList<Integer> neighbors = new ArrayList<>();
    }

    static class Edge {

        int u, v, cap, scalable;

        Edge(int u, int v, int cap, int scalable) {
            this.u = u;
            this.v = v;
            this.cap = cap;
            this.scalable = scalable;
        }
    }

    static class SensorInput {

        int n, dgCount, storageCount, transCount, edgeCount;
        ArrayList<Integer> dgs = new ArrayList<>();
        ArrayList<Integer> storages = new ArrayList<>();
        ArrayList<SensorNode> nodes = new ArrayList<>();
    }

    static int[] readList(String line) {
        String[] p = line.trim().split("\\s+");
        int c = Integer.parseInt(p[0]);
        int[] out = new int[c];
        for (int i = 0; i < c; i++) {
            out[i] = Integer.parseInt(p[i + 1]);
        }
        return out;
    }

    static SensorInput readSensor(String file) throws Exception {
        SensorInput in = new SensorInput();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String[] a = br.readLine().trim().split("\\s+");
        in.n = Integer.parseInt(a[0]);
        in.dgCount = Integer.parseInt(a[1]);
        in.storageCount = Integer.parseInt(a[2]);
        in.transCount = Integer.parseInt(a[3]);
        in.edgeCount = Integer.parseInt(a[4]);
        for (int x : readList(br.readLine())) {
            in.dgs.add(x);
        }
        for (int x : readList(br.readLine())) {
            in.storages.add(x);
        }
        br.readLine(); // trans list not needed
        for (int i = 0; i < in.n; i++) {
            String[] p = br.readLine().trim().split("\\s+");
            int z = 0;
            SensorNode u = new SensorNode();
            u.id = Integer.parseInt(p[z++]);
            u.type = Integer.parseInt(p[z++]);
            u.x = Double.parseDouble(p[z++]);
            u.y = Double.parseDouble(p[z++]);
            u.data = Integer.parseInt(p[z++]);
            u.storage = Integer.parseInt(p[z++]);
            u.energy = Integer.parseInt(p[z++]);
            u.cost = Integer.parseInt(p[z++]);
            u.priority = Integer.parseInt(p[z++]);
            int deg = Integer.parseInt(p[z++]);
            for (int j = 0; j < deg; j++) {
                u.neighbors.add(Integer.parseInt(p[z++]));
            }
            in.nodes.add(u);
        }
        br.close();
        return in;
    }

    static void writeFlow(SensorInput in, String outFile, int INF) throws Exception {
        int flowNodeCount = 2 * in.n + 2;
        int superSource = 2 * in.n;
        int superSink = 2 * in.n + 1;
        ArrayList<Integer> sourcePrimes = new ArrayList<>();
        ArrayList<Integer> sinkDoubles = new ArrayList<>();
        ArrayList<Edge> edges = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        for (SensorNode u : in.nodes) {
            int prime = 2 * u.id;
            int dbl = 2 * u.id + 1;
            edges.add(new Edge(prime, dbl, u.energy, 1));
            if (u.type == 1) {
                sourcePrimes.add(prime);
                edges.add(new Edge(superSource, prime, u.data, 0));
            }
            if (u.type == 2) {
                sinkDoubles.add(dbl);
                edges.add(new Edge(dbl, superSink, u.storage, 0));
            }
        }
        for (SensorNode u : in.nodes) {
            for (int v : u.neighbors) {
                String k = u.id + ":" + v;
                if (seen.contains(k)) {
                    continue;
                }
                seen.add(k);
                edges.add(new Edge(2 * u.id + 1, 2 * v, INF, 1));
            }
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            out.println(flowNodeCount + " " + edges.size() + " " + in.n + " " + sourcePrimes.size() + " " + sinkDoubles.size() + " " + superSource + " " + superSink + " " + INF);
            out.print(sourcePrimes.size());
            for (int x : sourcePrimes) {
                out.print(" " + x);

            }
            out.println();
            out.print(sinkDoubles.size());
            for (int x : sinkDoubles) {
                out.print(" " + x);

            }
            out.println();
            for (SensorNode u : in.nodes) {
                int prime = 2 * u.id;
                int dbl = 2 * u.id + 1;
                out.println(u.id + " " + prime + " " + dbl + " " + u.type + " " + u.data + " " + u.storage + " " + u.energy + " " + u.cost + " " + u.priority + " " + String.format(Locale.US, "%.4f", u.x) + " " + String.format(Locale.US, "%.4f", u.y));
            }
            for (Edge e : edges) {
                out.println(e.u + " " + e.v + " " + e.cap + " " + e.scalable);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String inFile, outFile;
        int INF;
        if (args.length >= 3) {
            inFile = args[0];
            outFile = args[1];
            INF = Integer.parseInt(args[2]);
        } else {
            Scanner sc = new Scanner(System.in);
            System.out.println("Sensor Network -> Flow Network Converter\n");
            System.out.print("Input sensor file: ");
            inFile = sc.nextLine().trim();
            System.out.print("Output flow file [flow.txt]: ");
            String s = sc.nextLine().trim();
            outFile = s.isEmpty() ? "flow.txt" : s;
            System.out.print("Large capacity for original links [1000000000]: ");
            s = sc.nextLine().trim();
            INF = s.isEmpty() ? 1000000000 : Integer.parseInt(s);
        }
        SensorInput in = readSensor(inFile);
        writeFlow(in, outFile, INF);
        System.out.println("Wrote flow file: " + outFile);
    }
}
