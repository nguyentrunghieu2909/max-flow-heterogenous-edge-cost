# PDP Mini Repo

This repo contains a simple pipeline for the project:

1. Generate a sensor network
2. Convert it to the split-node flow network
3. Visualize saved sensor / flow txt files
4. Run Algorithm 1, 2, and 5
5. Solve the exact ILP max-flow benchmark for MWF-H with Gurobi
6. Batch-compare Java Algorithm 2 against the ILP result

## Files

- `SensorNetworkGenerator.java`
- `SensorToFlowNetworkConverter.java`
- `NetworkVisualizer.java`
- `Algorithm1MaxFlow.java`
- `Algorithm2MWFHMaxFlow.java`
- `Algorithm5Approx.java`
- `ilp_mwfh_maxflow.py`
- `compare_algo2_ilp.py`

## Compile Java

```bash
javac SensorNetworkGenerator.java SensorToFlowNetworkConverter.java NetworkVisualizer.java Algorithm1MaxFlow.java Algorithm2MWFHMaxFlow.java Algorithm5Approx.java
```

## 1) Generate a sensor network

Interactive:

```bash
java SensorNetworkGenerator
```

CLI example:

```bash
java SensorNetworkGenerator 100 100 20 5 5 3 8 3 8 10 50 1 10 1 10 35 1 sensor.txt
```

Arguments:

```text
width length nodes dgs storage minData maxData minStorage maxStorage minEnergy maxEnergy minCost maxCost minPriority maxPriority range seed outputFile
```

## 2) Convert sensor to flow

Interactive:

```bash
java SensorToFlowNetworkConverter
```

CLI:

```bash
java SensorToFlowNetworkConverter sensor.txt flow.txt 1000000000
```

## 3) Visualize saved txt files

```bash
java NetworkVisualizer sensor sensor.txt
java NetworkVisualizer flow flow.txt
```

## 4) Run algorithms

Algorithm 1:

```bash
java Algorithm1MaxFlow flow.txt algo1.txt
```

Algorithm 2:

```bash
java Algorithm2MWFHMaxFlow flow.txt algo2.txt
```

Algorithm 5:

```bash
java Algorithm5Approx flow.txt algo5.txt
```

## 5) Run ILP benchmark (Gurobi)

```bash
python ilp_mwfh_maxflow_v2.py flow.txt ilp.txt
```

## 6) Batch compare Algorithm 2 vs ILP

```bash
python compare_algo2_ilp_v2.py --runs 3 --workdir . --width 100 --length 100 --nodes 20 --dgs 5 --storage 5 --min-data 3 --max-data 8 --min-storage 3 --max-storage 8 --min-energy 10 --max-energy 50 --min-cost 1 --max-cost 10 --min-priority 1 --max-priority 10 --range 35 --seed 1
```

The script writes files into `batch_out/` and creates `summary.csv` and `summary.txt`.
