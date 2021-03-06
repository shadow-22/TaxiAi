package gr.ntua.ece;

import java.util.Comparator;

public class NodeWithPriority implements Comparator<NodeWithPriority>
{
    public Node x;
    public double cost;

    NodeWithPriority() {
    }

    NodeWithPriority(Node x, double cost) {
        this.x = x;
        this.cost = cost;
    }

    public int compare(NodeWithPriority node1, NodeWithPriority node2) {
        return Double.compare(node1.cost, node2.cost);
    }

    public Node getNode() {
        return x;
    }
}
