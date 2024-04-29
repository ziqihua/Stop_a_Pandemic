import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class InformationSpread implements IInformationSpread {
    private double tau;
    private GraphL graph;
    public InformationSpread() {
        this.graph = new GraphL();
    }
    @Override
    public int loadGraphFromDataSet(String filePath, double tau) {
        this.tau = tau * 100; // scale tau up for integer comparison
        HashSet<Integer> connectedNodes = new HashSet<>();
        List<String> lines = readFile(filePath);

        if (!lines.isEmpty()) {
            initializeGraph(lines.get(0));
            for (int i = 1; i < lines.size(); i++) {
                processEdge(lines.get(i), connectedNodes);
            }
        }
        return connectedNodes.size();
    }
    private List<String> readFile(String filePath) {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
        return lines;
    }

    private void initializeGraph(String firstLine) {
        String[] parts = firstLine.split("\\s+");
        int numVertices = Integer.parseInt(parts[0]);
        graph.init(numVertices + 1); // Initialize the graph with one extra vertex
    }

    private void processEdge(String line, Set<Integer> connectedNodes) {
        String[] parts = line.split("\\s+");
        int from = Integer.parseInt(parts[0]);
        int to = Integer.parseInt(parts[1]);
        int weight = (int) (Double.parseDouble(parts[2]) * 100);

        if (from != 0 && to != 0 && weight > this.tau - 0.1) {
            graph.addEdge(from, to, weight);
            graph.addEdge(to, from, weight); // Add reverse edge for undirected graph
            connectedNodes.add(from);
            connectedNodes.add(to);
        }
    }

    @Override
    public int[] getNeighbors(int id) {
        return graph.neighbors(id);
    }

    @Override
    public List<Integer> path(int source, int destination) {
        if (source == destination) {
            return Collections.singletonList(source);
        }

        int nodeCount = graph.nodeCount();
        double[] distance = new double[nodeCount];
        boolean[] visited = new boolean[nodeCount];
        int[] pred = new int[nodeCount];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(pred, -10);
        Arrays.fill(visited, false);
        distance[source] = 0;

        PriorityQueue<Integer> priorityQueue =
                new PriorityQueue<>(Comparator.comparing(i -> distance[i]));
        priorityQueue.add(source);

        while (!priorityQueue.isEmpty()) {
            int vertexU = priorityQueue.poll();
            if (vertexU == destination) {
                break;
            }

            if (visited[vertexU]) {
                continue;
            }
            visited[vertexU] = true;
            relaxEdges(vertexU, distance, pred, visited, priorityQueue);
        }

        return buildPath(pred, source, destination);
    }

    private void relaxEdges(int vertexU, double[] distance, int[] pred,
                            boolean[] visited, PriorityQueue priorityQueue) {
        int[] neighbors = graph.neighbors(vertexU);
        for (int i = 0; i < neighbors.length; i++) {
            if (!visited[neighbors[i]]) {
                double weight = graph.weight(vertexU, neighbors[i]);
                double prob = weight / 100.0;
                double cost = -Math.log(prob);
                if (distance[vertexU] + cost < distance[neighbors[i]]) {
                    distance[neighbors[i]] = distance[vertexU] + cost;
                    pred[neighbors[i]] = vertexU;
                    priorityQueue.add(neighbors[i]);
                }
            }
        }
    }

    private List<Integer> buildPath(int[] predecessors, int source, int destination) {
        List<Integer> path = new ArrayList<>();
        for (int at = destination; at != -10; at = predecessors[at]) {
            path.add(at);
        }
        if (path.get(path.size() - 1) != source) {
            return new ArrayList<>(); // Path does not exist
        }
        Collections.reverse(path);
        return path;
    }


    @Override
    public double avgDegree() {
        if ((graph.nodeCount() - 1) == 0) {
            return 0.0;
        }
        int edgeNum = graph.edgeCount();
        int nodeNum = graph.nodeCount() - 1;
        double avgDegree = (double) edgeNum / nodeNum;
        return avgDegree;
    }

    @Override
    public double rNumber() {
        double avgDegree = this.avgDegree();
        double d = 1.0;
        double rNumber = tau * avgDegree * d / 100;
        return rNumber;
    }

    @Override
    public int generations(int seed, double threshold) {
        if (seed <= 0 || seed > graph.nodeCount() - 1 || threshold < 0 || threshold > 1) {
            return -1;
        }

        if (threshold == 0) {
            return 0;
        }

        int nodeNum = graph.nodeCount() - 1;
        int targetCount = (int) Math.ceil(threshold * nodeNum);

        boolean[] checked = new boolean[nodeNum + 1];
        Arrays.fill(checked, false);
        checked[seed] = true;
        int genCount = 0;

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(seed);
        int visitedCount = 1;
        while (!queue.isEmpty() && visitedCount < targetCount) {
            genCount++;
            int breadthNodeNum = queue.size();
            for (int i = 0; i < breadthNodeNum; i++) {
                int curNode = queue.poll();
                for (int neighbor : graph.neighbors(curNode)) {
                    if (!checked[neighbor]) {
                        checked[neighbor] = true;
                        visitedCount++;
                        queue.offer(neighbor);
                    }
                }
            }
            if (visitedCount >= targetCount) {
                return genCount;
            }
        }

        if (visitedCount >= targetCount) {
            return genCount;
        } else {
            return -1;
        }
    }

    @Override
    public int degree(int n) {
        if (n <= 0 || n >= graph.nodeCount()) {
            return -1;
        }
        int[] neighbors = graph.neighbors(n);
        return neighbors.length;
    }

    @Override
    public Collection<Integer> degreeNodes(int d) {
        Set<Integer> nodes = new HashSet<>();
        for (int i = 1; i < graph.nodeCount(); i++) {
            int[] neighbors = graph.neighbors(i);
            if (neighbors.length == d) {
                nodes.add(i);
            }
        }
        return nodes;
    }

    @Override
    public int generationsDegree(int seed, double threshold, int d) {
        if (seed <= 0 || seed >= graph.nodeCount() || threshold <= 0 || threshold > 1) {
            return -1;
        }
        GraphL copiedGraph = copyGraph();

        Collection<Integer> removeNodes = degreeNodes(d);
        if (removeNodes.isEmpty()) {
            return -1;
        } else if (removeNodes.contains(seed)) {
            return 0;
        }
        copiedGraph = toRemoveNodes(copiedGraph, removeNodes);

        int nodeNum = copiedGraph.nodeCount() - 1;
        int targetCount = (int) Math.ceil(threshold * nodeNum);

        boolean[] checked = new boolean[nodeNum + 1];
        Arrays.fill(checked, false);
        checked[seed] = true;
        int genCount = 0;

        Queue<Integer> q = new LinkedList<>();
        q.offer(seed);
        int checkedCount = 1;
        while (!q.isEmpty() && checkedCount < targetCount) {
            genCount++;
            int breadthNodeNum = q.size();
            for (int i = 0; i < breadthNodeNum; i++) {
                int curNode = q.poll();
                for (int neighbor : copiedGraph.neighbors(curNode)) {
                    if (!checked[neighbor]) {
                        checked[neighbor] = true;
                        checkedCount++;
                        q.offer(neighbor);
                    }
                }
            }
            if (checkedCount >= targetCount) {
                return genCount;
            }
        }

        if (checkedCount >= targetCount) {
            return genCount;
        } else {
            return -1;
        }
    }

    private GraphL copyGraph() {
        GraphL copiedGraph = new GraphL();
        copiedGraph.init(graph.nodeCount());

        for (int i = 1; i < graph.nodeCount(); i++) {
            int [] neighbors = graph.neighbors(i);
            for (int j = 0; j < neighbors.length; j++) {
                int weight = graph.weight(i, neighbors[j]);
                copiedGraph.addEdge(i, neighbors[j], weight);
            }
        }
        return copiedGraph;
    }

    private GraphL toRemoveNodes(GraphL graph, Collection<Integer> removeNodes) {
        for (int i : removeNodes) {
            int[] neighbors = graph.neighbors(i);
            for (int j = 0; j < neighbors.length; j++) {
                int neighbor = neighbors[j];
                graph.removeEdge(i, neighbor);
                graph.removeEdge(neighbor, i);
            }
        }
        return graph;
    }

    private double avgDegreeWithRemovedNodes(GraphL graph) {
        int edgeNum = 0;
        int nodeNum = graph.nodeCount() - 1;
        for (int i = 1; i <= nodeNum; i++) {
            edgeNum += graph.neighbors(i).length;
        }

        double avgDegreeRemovedNodes = (double) edgeNum / nodeNum;
        return avgDegreeRemovedNodes;
    }

    @Override
    public double rNumberDegree(int d) {
        GraphL copiedGraph = copyGraph();
        Collection<Integer> removeNodes = degreeNodes(d);
        if (removeNodes.isEmpty()) {
            return rNumber();
        }
        copiedGraph = toRemoveNodes(copiedGraph, removeNodes);
        double avgDegreeRemovedNodes = avgDegreeWithRemovedNodes(copiedGraph);
        double theD = 1.0;
        double rNumberDegree = tau * avgDegreeRemovedNodes * theD / 100;
        return rNumberDegree;
    }

    @Override
    public double clustCoeff(int n) {
        if (n <= 0 || n >= graph.nodeCount()) {
            return -1;
        }
        int[] neighbors = graph.neighbors(n);
        int nodeDegree = neighbors.length;
        if (nodeDegree <= 1) {
            return 0;
        }

        int t = 0;
        for (int i = 0; i < nodeDegree; i++) {
            for (int j = i + 1; j < nodeDegree; j++) {
                if (graph.hasEdge(neighbors[i], neighbors[j])) {
                    t++;
                }
            }
        }
        double coeff = (double) (2 * t) / (nodeDegree * (nodeDegree - 1));
        return coeff;
    }

    @Override
    public Collection<Integer> clustCoeffNodes(double low, double high) {
        Set<Integer> clustCoeffNodes = new HashSet<>();
        for (int i = 1; i < graph.nodeCount(); i++) {
            double coeff = clustCoeff(i);
            if (inRange(coeff, high, low)) {
                clustCoeffNodes.add(i);
            }
        }
        return clustCoeffNodes;
    }

    private static boolean inRange(double coeff, double upper, double lower) {
        double e = 0.01;
        return coeff >= lower - e && coeff <= upper + e;
    }

    @Override
    public int generationsCC(int seed, double threshold, double low, double high) {
        if (seed <= 0 || seed >= graph.nodeCount() || threshold <= 0 || threshold > 1) {
            return -1;
        }
        GraphL copiedGraph = copyGraph();

        Collection<Integer> removeNodes = clustCoeffNodes(low, high);
        if (removeNodes.isEmpty()) {
            return -1;
        } else if (removeNodes.contains(seed)) {
            return 0;
        }
        copiedGraph = toRemoveNodes(copiedGraph, removeNodes);

        int nodeNum = copiedGraph.nodeCount() - 1;
        int targetCount = (int) Math.ceil(threshold * nodeNum);

        boolean[] checked = new boolean[nodeNum + 1];
        Arrays.fill(checked, false);
        checked[seed] = true;
        int genCount = 0;

        Queue<Integer> q = new LinkedList<>();
        q.offer(seed);
        int checkedCount = 1;
        while (!q.isEmpty() && checkedCount < targetCount) {
            genCount++;
            int breadthNodeNum = q.size();
            for (int i = 0; i < breadthNodeNum; i++) {
                int curNode = q.poll();
                for (int neighbor : copiedGraph.neighbors(curNode)) {
                    if (!checked[neighbor]) {
                        checked[neighbor] = true;
                        checkedCount++;
                        q.offer(neighbor);
                    }
                }
            }
            if (checkedCount >= targetCount) {
                return genCount;
            }
        }

        if (checkedCount >= targetCount) {
            return genCount;
        } else {
            return -1;
        }
    }

    @Override
    public double rNumberCC(double low, double high) {
        GraphL copiedGraph = copyGraph();
        Collection<Integer> removeNodes = clustCoeffNodes(low, high);
        if (removeNodes.isEmpty()) {
            return rNumber();
        }
        copiedGraph = toRemoveNodes(copiedGraph, removeNodes);
        double avgDegreeRemovedNodes = avgDegreeWithRemovedNodes(copiedGraph);
        double theD = 1.0;
        double rNumberCC = tau * avgDegreeRemovedNodes * theD / 100;
        return rNumberCC;
    }

    @Override
    public Collection<Integer> highDegLowCCNodes(int lowBoundDeg, double upBoundCC) {
        Set<Integer> highDegLowCCNodes = new HashSet<>();
        for (int i = 1; i < graph.nodeCount(); i++) {
            int nodeDegree = degree(i);
            double nodeCoeff = clustCoeff(i);
            if (nodeDegree >= lowBoundDeg && inRange(nodeCoeff, upBoundCC, 0)) {
                highDegLowCCNodes.add(i);
            }
        }
        return highDegLowCCNodes;
    }

    @Override
    public int generationsHighDegLowCC(int seed, double threshold,
                                       int lowBoundDegree, double upBoundCC) {
        if (seed <= 0 || seed >= graph.nodeCount() || threshold <= 0 || threshold > 1) {
            return -1;
        }
        GraphL copiedGraph = copyGraph();

        Collection<Integer> removeNodes = highDegLowCCNodes(lowBoundDegree, upBoundCC);
        if (removeNodes.isEmpty()) {
            return -1;
        }
        if (removeNodes.contains(seed)) {
            return 0;
        }
        copiedGraph = toRemoveNodes(copiedGraph, removeNodes);

        int nodeNum = copiedGraph.nodeCount() - 1;
        int targetCount = (int) Math.ceil(threshold * nodeNum);

        boolean[] checked = new boolean[nodeNum + 1];
        Arrays.fill(checked, false);
        checked[seed] = true;
        int genCount = 0;

        Queue<Integer> q = new LinkedList<>();
        q.offer(seed);
        int checkedCount = 1;
        while (!q.isEmpty() && checkedCount < targetCount) {
            genCount++;
            int breadthNodeNum = q.size();
            for (int i = 0; i < breadthNodeNum; i++) {
                int curNode = q.poll();
                for (int neighbor : copiedGraph.neighbors(curNode)) {
                    if (!checked[neighbor]) {
                        checked[neighbor] = true;
                        checkedCount++;
                        q.offer(neighbor);
                    }
                }
            }
            if (checkedCount >= targetCount) {
                return genCount;
            }
        }

        if (checkedCount >= targetCount) {
            return genCount;
        } else {
            return -1;
        }
    }

    @Override
    public double rNumberDegCC(int lowBoundDegree, double upBoundCC) {
        GraphL copiedGraph = copyGraph();
        Collection<Integer> removeNodes = highDegLowCCNodes(lowBoundDegree, upBoundCC);
        if (removeNodes.isEmpty()) {
            return rNumber();
        }
        copiedGraph = toRemoveNodes(copiedGraph, removeNodes);
        double avgDegreeRemovedNodes = avgDegreeWithRemovedNodes(copiedGraph);
        double theD = 1.0;
        double rNumberDegCC = tau * avgDegreeRemovedNodes * theD / 100;
        return rNumberDegCC;
    }
}
