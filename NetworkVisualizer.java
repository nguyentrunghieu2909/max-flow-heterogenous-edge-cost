
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class NetworkVisualizer {

    static class NodeInfo {

        int id, type, data, storage, energy, cost, priority, originalId = -1;
        double x, y;
        boolean isPrime, isDouble, isSuperSource, isSuperSink;
        ArrayList<Integer> neighbors = new ArrayList<>();
    }

    static class EdgeInfo {

        int u, v, cap, scalable;
        boolean directed = true;
    }

    static class Model {

        String kind;
        ArrayList<NodeInfo> nodes = new ArrayList<>();
        ArrayList<EdgeInfo> edges = new ArrayList<>();
        HashMap<Integer, NodeInfo> map = new HashMap<>();
        int superSource = -1, superSink = -1;
    }

    static class ViewerPanel extends JPanel {

        Model model;
        JTextArea detail;
        NodeInfo selectedNode;
        EdgeInfo selectedEdge;

        ViewerPanel(Model m, JTextArea d) {
            model = m;
            detail = d;
            setPreferredSize(new Dimension(950, 700));
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    click(e.getX(), e.getY());
                }
            });
        }

        void click(int mx, int my) {
            selectedNode = findNode(mx, my);
            if (selectedNode != null) {
                selectedEdge = null;
                detail.setText(nodeText(selectedNode));
                repaint();
                return;
            }
            selectedEdge = findEdge(mx, my);
            if (selectedEdge != null) {
                selectedNode = null;
                detail.setText(edgeText(selectedEdge));
                repaint();
                return;
            }
        }

        NodeInfo findNode(int mx, int my) {
            for (NodeInfo n : model.nodes) {
                Point p = screen(n);
                int r = 9;
                int dx = mx - p.x, dy = my - p.y;
                if (dx * dx + dy * dy <= r * r) {
                    return n;

                }
            }
            return null;
        }

        EdgeInfo findEdge(int mx, int my) {
            double best = 8;
            EdgeInfo ans = null;
            for (EdgeInfo e : model.edges) {
                Point a = screen(model.map.get(e.u));
                Point b = screen(model.map.get(e.v));
                double d = dist(mx, my, a.x, a.y, b.x, b.y);
                if (d < best) {
                    best = d;
                    ans = e;
                }
            }
            return ans;
        }

        Point screen(NodeInfo n) {
            int margin = 40, w = getWidth() - 2 * margin, h = getHeight() - 2 * margin;
            return new Point(margin + (int) Math.round(n.x * w), margin + (int) Math.round(n.y * h));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (EdgeInfo e : model.edges) {
                Point a = screen(model.map.get(e.u)), b = screen(model.map.get(e.v));
                g2.setColor(e == selectedEdge ? Color.RED : Color.LIGHT_GRAY);
                drawArrow(g2, a.x, a.y, b.x, b.y);
            }
            for (NodeInfo n : model.nodes) {
                Point p = screen(n);
                drawNode(g2, n, p.x, p.y);
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(n.id), p.x + 8, p.y - 8);
                if (n == selectedNode) {
                    g2.setColor(Color.RED);
                    g2.drawOval(p.x - 11, p.y - 11, 22, 22);
                }
            }
        }

        void drawNode(Graphics2D g2, NodeInfo n, int x, int y) {
            if (n.isSuperSource) {
                g2.setColor(Color.ORANGE);
                g2.fillRect(x - 8, y - 8, 16, 16);
                return;
            }
            if (n.isSuperSink) {
                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(x - 8, y - 8, 16, 16);
                return;
            }
            if (n.type == 1) {
                g2.setColor(new Color(210, 70, 70));
            } else if (n.type == 2) {
                g2.setColor(new Color(70, 110, 220));
            } else {
                g2.setColor(new Color(70, 170, 90));
            }
            if (model.kind.equals("sensor")) {
                if (n.type == 1) {
                    g2.fillOval(x - 8, y - 8, 16, 16);
                } else if (n.type == 2) {
                    g2.fillRect(x - 8, y - 8, 16, 16);
                } else {
                    int[] xs = {x, x - 8, x + 8};
                    int[] ys = {y - 8, y + 8, y + 8};
                    g2.fillPolygon(xs, ys, 3);
                }
            } else {
                if (n.isPrime) {
                    g2.fillOval(x - 8, y - 8, 16, 16);
                } else {
                    g2.fillRect(x - 8, y - 8, 16, 16);

                }
            }
        }

        String nodeText(NodeInfo n) {
            return "id=" + n.id + "\ntype=" + n.type + "\noriginal=" + n.originalId + "\ndata=" + n.data + "\nstorage=" + n.storage + "\nenergy=" + n.energy + "\ncost=" + n.cost + "\npriority=" + n.priority;
        }

        String edgeText(EdgeInfo e) {
            return "u=" + e.u + "\nv=" + e.v + "\ncap=" + e.cap + "\nscalable=" + e.scalable;
        }

        double dist(double px, double py, double x1, double y1, double x2, double y2) {
            double dx = x2 - x1, dy = y2 - y1;
            if (dx == 0 && dy == 0) {
                return Math.hypot(px - x1, py - y1);

            }
            double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
            t = Math.max(0, Math.min(1, t));
            double x = x1 + t * dx, y = y1 + t * dy;
            return Math.hypot(px - x, py - y);
        }

        void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            g2.drawLine(x1, y1, x2, y2);
            double a = Math.atan2(y2 - y1, x2 - x1);
            int s = 7;
            int ax1 = (int) (x2 - s * Math.cos(a - Math.PI / 6)), ay1 = (int) (y2 - s * Math.sin(a - Math.PI / 6));
            int ax2 = (int) (x2 - s * Math.cos(a + Math.PI / 6)), ay2 = (int) (y2 - s * Math.sin(a + Math.PI / 6));
            g2.drawLine(x2, y2, ax1, ay1);
            g2.drawLine(x2, y2, ax2, ay2);
        }
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

    static Model readSensor(String file) throws Exception {
        Model m = new Model();
        m.kind = "sensor";
        BufferedReader br = new BufferedReader(new FileReader(file));
        String[] h = br.readLine().trim().split("\\s+");
        int n = Integer.parseInt(h[0]);
        int edgeCount = Integer.parseInt(h[4]);
        br.readLine();
        br.readLine();
        br.readLine();
        for (int i = 0; i < n; i++) {
            String[] p = br.readLine().trim().split("\\s+");
            int z = 0;
            NodeInfo u = new NodeInfo();
            u.id = Integer.parseInt(p[z++]);
            u.type = Integer.parseInt(p[z++]);
            u.x = Double.parseDouble(p[z++]) / 100.0;
            u.y = 1.0 - Double.parseDouble(p[z++]) / 100.0;
            u.data = Integer.parseInt(p[z++]);
            u.storage = Integer.parseInt(p[z++]);
            u.energy = Integer.parseInt(p[z++]);
            u.cost = Integer.parseInt(p[z++]);
            u.priority = Integer.parseInt(p[z++]);
            int deg = Integer.parseInt(p[z++]);
            for (int j = 0; j < deg; j++) {
                u.neighbors.add(Integer.parseInt(p[z++]));

            }
            m.nodes.add(u);
            m.map.put(u.id, u);
        }
        HashSet<String> seen = new HashSet<>();
        for (NodeInfo u : m.nodes) {
            for (int v : u.neighbors) {
                String k = Math.min(u.id, v) + ":" + Math.max(u.id, v);
                if (seen.contains(k)) {
                    continue;

                }
                seen.add(k);
                EdgeInfo e = new EdgeInfo();
                e.u = u.id;
                e.v = v;
                e.cap = 1;
                e.scalable = 1;
                e.directed = false;
                m.edges.add(e);
            }
        }
        br.close();
        if (!m.nodes.isEmpty()) {
            double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
            for (NodeInfo u : m.nodes) {
                minX = Math.min(minX, u.x);
                maxX = Math.max(maxX, u.x);
                minY = Math.min(minY, u.y);
                maxY = Math.max(maxY, u.y);
            }
            double dx = Math.max(1e-6, maxX - minX), dy = Math.max(1e-6, maxY - minY);
            for (NodeInfo u : m.nodes) {
                u.x = (u.x - minX) / dx;
                u.y = (u.y - minY) / dy;
            }
        }
        return m;
    }

    static Model readFlow(String file) throws Exception {
        Model m = new Model();
        m.kind = "flow";
        BufferedReader br = new BufferedReader(new FileReader(file));
        String[] h = br.readLine().trim().split("\\s+");
        int flowNodes = Integer.parseInt(h[0]);
        int originalCount = Integer.parseInt(h[2]);
        int superSource = Integer.parseInt(h[5]);
        int superSink = Integer.parseInt(h[6]);
        m.superSource = superSource;
        m.superSink = superSink;
        int[] sources = parseList(br.readLine());
        int[] sinks = parseList(br.readLine());
        HashSet<Integer> sourceSet = new HashSet<>(), sinkSet = new HashSet<>();
        for (int x : sources) {
            sourceSet.add(x);

        }
        for (int x : sinks) {
            sinkSet.add(x);
        }
        for (int i = 0; i < originalCount; i++) {
            String[] p = br.readLine().trim().split("\\s+");
            int z = 0;
            int original = Integer.parseInt(p[z++]);
            int prime = Integer.parseInt(p[z++]);
            int dbl = Integer.parseInt(p[z++]);
            int type = Integer.parseInt(p[z++]);
            int data = Integer.parseInt(p[z++]);
            int storage = Integer.parseInt(p[z++]);
            int energy = Integer.parseInt(p[z++]);
            int cost = Integer.parseInt(p[z++]);
            int priority = Integer.parseInt(p[z++]);
            double ox = Double.parseDouble(p[z++]);
            double oy = Double.parseDouble(p[z++]);
            NodeInfo a = new NodeInfo();
            a.id = prime;
            a.type = type;
            a.data = data;
            a.storage = storage;
            a.energy = energy;
            a.cost = cost;
            a.priority = priority;
            a.originalId = original;
            a.isPrime = true;
            a.x = 0.35;
            a.y = (original + 1.0) / (originalCount + 1.0);
            NodeInfo b = new NodeInfo();
            b.id = dbl;
            b.type = type;
            b.data = data;
            b.storage = storage;
            b.energy = energy;
            b.cost = cost;
            b.priority = priority;
            b.originalId = original;
            b.isDouble = true;
            b.x = 0.65;
            b.y = (original + 1.0) / (originalCount + 1.0);
            m.nodes.add(a);
            m.nodes.add(b);
            m.map.put(a.id, a);
            m.map.put(b.id, b);
        }
        NodeInfo s = new NodeInfo();
        s.id = superSource;
        s.isSuperSource = true;
        s.x = 0.1;
        s.y = 0.5;
        m.nodes.add(s);
        m.map.put(s.id, s);
        NodeInfo t = new NodeInfo();
        t.id = superSink;
        t.isSuperSink = true;
        t.x = 0.9;
        t.y = 0.5;
        m.nodes.add(t);
        m.map.put(t.id, t);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;

            }
            String[] p = line.trim().split("\\s+");
            EdgeInfo e = new EdgeInfo();
            e.u = Integer.parseInt(p[0]);
            e.v = Integer.parseInt(p[1]);
            e.cap = Integer.parseInt(p[2]);
            e.scalable = Integer.parseInt(p[3]);
            m.edges.add(e);
        }
        br.close();
        return m;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java NetworkVisualizer sensor|flow file.txt");
            return;
        }
        Model m = args[0].equalsIgnoreCase("sensor") ? readSensor(args[1]) : readFlow(args[1]);
        JTextArea detail = new JTextArea("Click a node or edge.");
        detail.setEditable(false);
        JFrame f = new JFrame("Network Visualizer - " + args[0]);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout());
        f.add(new ViewerPanel(m, detail), BorderLayout.CENTER);
        f.add(new JScrollPane(detail), BorderLayout.EAST);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
