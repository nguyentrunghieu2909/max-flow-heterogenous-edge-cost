
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.swing.*;

public class SensorNetworkGenerator {

    static class Node {

        int id;
        int type; // 0 trans, 1 DG, 2 storage
        double x, y;
        int data;
        int storage;
        int energy;
        int cost;
        int priority;
        ArrayList<Integer> neighbors = new ArrayList<>();
    }

    static class Params {

        int width, length, nodes, dgs, storage;
        int minData, maxData, minStorage, maxStorage;
        int minEnergy, maxEnergy, minCost, maxCost, minPriority, maxPriority;
        double range;
        long seed;
        String outputFile;
    }

    static class Panel extends JPanel {

        java.util.List<Node> nodes;
        int width, length;

        Panel(java.util.List<Node> nodes, int width, int length) {
            this.nodes = nodes;
            this.width = width;
            this.length = length;
            setPreferredSize(new Dimension(900, 650));
            setBackground(Color.WHITE);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int margin = 50;
            int w = getWidth() - 2 * margin;
            int h = getHeight() - 2 * margin;
            g2.setColor(Color.LIGHT_GRAY);
            for (Node u : nodes) {
                int x1 = margin + (int) Math.round(u.x / width * w);
                int y1 = margin + h - (int) Math.round(u.y / length * h);
                for (int vId : u.neighbors) {
                    if (u.id < vId) {
                        Node v = nodes.get(vId);
                        int x2 = margin + (int) Math.round(v.x / width * w);
                        int y2 = margin + h - (int) Math.round(v.y / length * h);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
            }
            for (Node u : nodes) {
                int x = margin + (int) Math.round(u.x / width * w);
                int y = margin + h - (int) Math.round(u.y / length * h);
                g2.setColor(u.type == 1 ? new Color(200, 70, 70) : u.type == 2 ? new Color(70, 110, 220) : new Color(70, 170, 90));
                if (u.type == 1) {
                    g2.fillOval(x - 8, y - 8, 16, 16);
                } else if (u.type == 2) {
                    g2.fillRect(x - 8, y - 8, 16, 16);
                } else {
                    int[] xs = {x, x - 8, x + 8};
                    int[] ys = {y - 8, y + 8, y + 8};
                    g2.fillPolygon(xs, ys, 3);
                }
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(u.id), x + 8, y - 8);
            }
        }
    }

    static int readInt(Scanner sc, String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v < min) {
                    throw new NumberFormatException();
                }
                return v;
            } catch (Exception e) {
                System.out.println("Enter an integer >= " + min);
            }
        }
    }

    static double readDouble(Scanner sc, String prompt, double min) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(sc.nextLine().trim());
                if (v < min) {
                    throw new NumberFormatException();
                }
                return v;
            } catch (Exception e) {
                System.out.println("Enter a number >= " + min);
            }
        }
    }

    static Params readInteractive() {
        Scanner sc = new Scanner(System.in);
        Params p = new Params();
        System.out.println("Sensor Network Generator\n");
        p.width = readInt(sc, "Width: ", 1);
        p.length = readInt(sc, "Length: ", 1);
        p.nodes = readInt(sc, "Number of nodes: ", 1);
        while (true) {
            p.dgs = readInt(sc, "Number of DGs: ", 0);
            p.storage = readInt(sc, "Number of storage nodes: ", 0);
            if (p.dgs + p.storage <= p.nodes) {
                break;
            }
            System.out.println("DG + storage cannot exceed total nodes.");
        }
        p.minData = readInt(sc, "Min data amount: ", 0);
        p.maxData = readInt(sc, "Max data amount: ", p.minData);
        p.minStorage = readInt(sc, "Min storage capacity: ", 0);
        p.maxStorage = readInt(sc, "Max storage capacity: ", p.minStorage);
        p.minEnergy = readInt(sc, "Min node energy: ", 0);
        p.maxEnergy = readInt(sc, "Max node energy: ", p.minEnergy);
        p.minCost = readInt(sc, "Min cost: ", 0);
        p.maxCost = readInt(sc, "Max cost: ", p.minCost);
        p.minPriority = readInt(sc, "Min priority: ", 0);
        p.maxPriority = readInt(sc, "Max priority: ", p.minPriority);
        p.range = readDouble(sc, "Transmission range: ", 0);
        p.seed = readInt(sc, "Random seed: ", 0);
        System.out.print("Output file [sensor.txt]: ");
        String s = sc.nextLine().trim();
        p.outputFile = s.isEmpty() ? "sensor.txt" : s;
        return p;
    }

    static Params readArgs(String[] args) {
        if (args.length != 18) {
            return null;
        }
        Params p = new Params();
        int i = 0;
        p.width = Integer.parseInt(args[i++]);
        p.length = Integer.parseInt(args[i++]);
        p.nodes = Integer.parseInt(args[i++]);
        p.dgs = Integer.parseInt(args[i++]);
        p.storage = Integer.parseInt(args[i++]);
        p.minData = Integer.parseInt(args[i++]);
        p.maxData = Integer.parseInt(args[i++]);
        p.minStorage = Integer.parseInt(args[i++]);
        p.maxStorage = Integer.parseInt(args[i++]);
        p.minEnergy = Integer.parseInt(args[i++]);
        p.maxEnergy = Integer.parseInt(args[i++]);
        p.minCost = Integer.parseInt(args[i++]);
        p.maxCost = Integer.parseInt(args[i++]);
        p.minPriority = Integer.parseInt(args[i++]);
        p.maxPriority = Integer.parseInt(args[i++]);
        p.range = Double.parseDouble(args[i++]);
        p.seed = Long.parseLong(args[i++]);
        p.outputFile = args[i];
        return p;
    }

    static int rnd(Random r, int a, int b) {
        return a + r.nextInt(b - a + 1);
    }

    static ArrayList<Node> generate(Params p) {
        Random r = new Random(p.seed);
        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < p.nodes; i++) {
            ids.add(i);
        }
        Collections.shuffle(ids, r);
        HashSet<Integer> dgSet = new HashSet<>(ids.subList(0, p.dgs));
        HashSet<Integer> stSet = new HashSet<>(ids.subList(p.dgs, p.dgs + p.storage));
        for (int i = 0; i < p.nodes; i++) {
            Node u = new Node();
            u.id = i;
            u.x = r.nextDouble() * p.width;
            u.y = r.nextDouble() * p.length;
            u.energy = rnd(r, p.minEnergy, p.maxEnergy);
            if (dgSet.contains(i)) {
                u.type = 1;
                u.data = rnd(r, p.minData, p.maxData);
                u.cost = rnd(r, p.minCost, p.maxCost);
                u.priority = rnd(r, p.minPriority, p.maxPriority);
            } else if (stSet.contains(i)) {
                u.type = 2;
                u.storage = rnd(r, p.minStorage, p.maxStorage);
            }
            nodes.add(u);
        }
        for (int i = 0; i < p.nodes; i++) {
            for (int j = i + 1; j < p.nodes; j++) {
                Node a = nodes.get(i), b = nodes.get(j);
                double dx = a.x - b.x, dy = a.y - b.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist <= p.range) {
                    a.neighbors.add(j);
                    b.neighbors.add(i);
                }
            }
        }
        return nodes;
    }

    static void save(ArrayList<Node> nodes, Params p) throws IOException {
        ArrayList<Integer> dgs = new ArrayList<>(), sts = new ArrayList<>(), trs = new ArrayList<>();
        int edges = 0;
        for (Node u : nodes) {
            if (u.type == 1) {
                dgs.add(u.id);
            } else if (u.type == 2) {
                sts.add(u.id);
            } else {
                trs.add(u.id);
            }
            edges += u.neighbors.size();
        }
        edges /= 2;
        try (PrintWriter out = new PrintWriter(new FileWriter(p.outputFile))) {
            out.println(p.nodes + " " + dgs.size() + " " + sts.size() + " " + trs.size() + " " + edges);
            out.print(dgs.size());
            for (int x : dgs) {
                out.print(" " + x);

            }
            out.println();
            out.print(sts.size());
            for (int x : sts) {
                out.print(" " + x);

            }
            out.println();
            out.print(trs.size());
            for (int x : trs) {
                out.print(" " + x);

            }
            out.println();
            for (Node u : nodes) {
                Collections.sort(u.neighbors);
                out.print(u.id + " " + u.type + " ");
                out.printf(Locale.US, "%.4f %.4f ", u.x, u.y);
                out.print(u.data + " " + u.storage + " " + u.energy + " " + u.cost + " " + u.priority + " " + u.neighbors.size());
                for (int v : u.neighbors) {
                    out.print(" " + v);
                }
                out.println();
            }
        }
    }

    static void printSummary(ArrayList<Node> nodes) {
        int e = 0;
        for (Node u : nodes) {
            e += u.neighbors.size();
        }
        System.out.println("Generated nodes: " + nodes.size());
        System.out.println("Edges: " + (e / 2));
        for (Node u : nodes) {
            System.out.println("node=" + u.id + " type=" + u.type + " x=" + String.format(Locale.US, "%.2f", u.x)
                    + " y=" + String.format(Locale.US, "%.2f", u.y) + " data=" + u.data + " storage=" + u.storage
                    + " energy=" + u.energy + " cost=" + u.cost + " priority=" + u.priority + " deg=" + u.neighbors.size());
        }
    }

    public static void main(String[] args) throws Exception {
        Params p = readArgs(args);
        if (p == null) {
            p = readInteractive();
        }
        ArrayList<Node> nodes = generate(p);
        save(nodes, p);
        printSummary(nodes);
        if (args.length == 0) {
            JFrame f = new JFrame("Sensor Network");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new Panel(nodes, p.width, p.length));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        }
    }
}
