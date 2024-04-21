package Infraestructure.VehicleTrafficZone.Strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import Models.Node;

public class AStar {

    List<Node> tree = new ArrayList<>();
    Node start = null;
    Node goal = null;

    public List<Node> findShortestPath(List<Node> tree) {
        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>();
        Map<Node, Double> fScore = new HashMap<>();
        PriorityQueue<Node> openSet = new PriorityQueue<>((n1, n2) -> Double.compare(fScore.getOrDefault(n1, Double.MAX_VALUE), fScore.getOrDefault(n2, Double.MAX_VALUE)));
        List<Node> closedSet = new ArrayList<>();
        this.tree = tree;
        start = tree.get(tree.size() - 1);
        goal = tree.get(0);

        gScore.put(start, 0.0);
        fScore.put(start, calculateHeuristicCostEstimate(start, goal));
        openSet.offer(start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }

            closedSet.add(current);

            for (Node neighbor : getNeighbors(current)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + distanceBetween(current, neighbor);

                if (!openSet.contains(neighbor) || tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + calculateHeuristicCostEstimate(neighbor, goal));

                    if (!openSet.contains(neighbor)) {
                        openSet.offer(neighbor);
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    private double calculateHeuristicCostEstimate(Node node, Node goal) {
        double dx = goal.getX() - node.getX();
        double dy = goal.getY() - node.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distanceBetween(Node node1, Node node2) {
        double dx = node2.getX() - node1.getX();
        double dy = node2.getY() - node1.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        Node parent = node.getParent();

        if (parent != null) {
            neighbors.add(parent);
        }

        for (Node n : tree) {
            if (n.getParent() == node) {
                neighbors.add(n);
            }
        }

        return neighbors;
    }

    private List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }

        Collections.reverse(path);
        return path;
    }
}
