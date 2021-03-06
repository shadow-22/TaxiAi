package gr.ntua.ece;

import com.ugos.jiprolog.engine.JIPQuery;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Input {
    private SimpleWeightedGraph<Node, DefaultWeightedEdge> graph;
    private Client client;
    private ArrayList<Taxi> taxis;
    private PrologSystem prolog;

    // Input as a singleton instance
    private static final Input instance = new Input();

    // Files
    private static final String NODES_FILE = "nodes.csv";
    private static final String CLIENT_FILE = "client.csv";
    private static final String TAXIS_FILE = "taxis.csv";

    private Input() {
        prolog = PrologSystem.getInstance();
        readInput();
    }

    public static Input getInstance() {
        return instance;
    }

    public void readInput() {
        readNodes();
        readClient();
        readTaxis();
    }

    private void readNodes() {
        /* Create a Graph representation of the data as the return value & a Map to check for points
           belonging to multiply roads. */
        this.graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        BufferedReader reader = null;

        double x;
        double y;
        long lineId;
        long nodeId;
        String streetName;

        /* Read the places from the file, create nodes and add edges to them */
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(NODES_FILE))));
            String readLine;
            String[] split;
            int counter = 0;
            Long prevLineId = null;
            Node prevNode = null;
            HashMap<Long, Node> checkForCross = new HashMap<>();

            /* Read from the second line of the file onwards, first one contains headers, not data.
               Then, for each point, read its road Id. */
            reader.readLine(); // skip the header readLine
            while ((readLine = reader.readLine()) != null) {
                split = readLine.split(",");
                x = Double.parseDouble(split[0].trim());
                y = Double.parseDouble(split[1].trim());
                lineId = Long.parseLong(split[2].trim());
                nodeId = Long.parseLong(split[3].trim());

                streetName = "";
                if (split.length == 5) {
                    streetName = String.valueOf(split[4].trim());
                }

                /* For each point, create a Node representation of it, given its coordinates as parameters. */
                Node currNode = new Node(x, y, streetName, nodeId, lineId);

                // ************************************
                String query_nodes = "node(" + x + ", " + y + ", " + nodeId+ ").";
                prolog.asserta(query_nodes);

                System.out.println("test1: ");
                // ********
                Boolean result = prolog.query("node(" + x + ", " + y + ", " + nodeId+ ").");
                if (result) {
                    System.out.println("yes");
                }

                // *********

                String belongsTo = "belongsTo(" + nodeId + ", " + lineId + ").";
                prolog.asserta(belongsTo);

                // *******************************************************


                /* If current point belongs to the same road as the previous one: */
                if (prevLineId != null && lineId == prevLineId) {
                    if (checkForCross.containsKey(nodeId)) {
                        /* Add back edge to already found node on the same road. Creates a bidirectional circle.
                       Potentially a road shortcut. */

                        /* Basically, given a road R with already read vertex-intersection V and a different
                         * road K that leads to or cuts through R in vertex V, create the relevant edge
                         * and set V as current node. Make sure the vertex of K leading to the already read V is
                         * not V itself. */
                        currNode = checkForCross.get(currNode.getNodeId());
                        if (!graph.containsEdge(prevNode, currNode)) {
                            if (!currNode.equals(prevNode)) {
                                DefaultWeightedEdge edge = graph.addEdge(prevNode, currNode);
                                graph.setEdgeWeight(edge, currNode.euclid(prevNode));
                            }
                        }
                    } else {
                        /* Add edge between newly found node and previous node on the same road. Linear connection. */
                        checkForCross.put(nodeId, currNode);
                        graph.addVertex(currNode);
                        DefaultWeightedEdge edge = graph.addEdge(prevNode, currNode);
                        graph.setEdgeWeight(edge, currNode.euclid(prevNode));
                    }
                } else {
                    /* If node has already been found and it's an intersection* between two roads,
                       then set this node as current node(don't change anything).
                   Else, if node is new and not connected with previous road,
                       then create the new node and put it in the graph.
                   *Only one node can act as intersection between two roads, thus no new edges need
                   to be constructed. */
                    if (checkForCross.containsKey(nodeId)) {
                        currNode = checkForCross.get(currNode.getNodeId());
                    } else {
                        checkForCross.put(currNode.getNodeId(), currNode);
                        graph.addVertex(currNode);
                    }
                }
                /* In any case, set the current Node and Road as previous for the loop to continue */
                prevNode = currNode;
                prevLineId = lineId;
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    /* Read the Client from the file */
    /* Search for the closest point in the graph relative to each client
     * and set the initial state of the client to that point.
     * We'll assume all clients are reasonably close to the road network
     * as given in nodes.scv.*/
    private void readClient() {
        BufferedReader reader = null;

        double x;
        double y;
        double destX;
        double destY;
        Date time = new Date(); // if date is not provided, set it as the current time.
        int persons;
        String lang;
        int luggage;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(CLIENT_FILE))));
            String readLine;
            String[] split;

            reader.readLine(); // skip the header readLine
            readLine = reader.readLine();
            split = readLine.split(",");
            x = Double.parseDouble(split[0].trim());
            y = Double.parseDouble(split[1].trim());
            destX = Double.parseDouble(split[2].trim());
            destY = Double.parseDouble(split[3].trim());
            try {
                time = Client.TIME_FORMAT.parse(split[4].trim());
            } catch (ParseException e) {
                System.err.println(e.getMessage());
            }
            persons = Integer.parseInt(split[5].trim());
            lang = String.valueOf(split[6].trim());
            luggage = Integer.parseInt(split[7].trim());

            this.client = new Client(x, y, destX, destY, time, persons, lang, luggage);

            String query_client = "client(" + x + ", " + y + ", " + destX +
                    ", " + destY + ", " + this.client.timeString() + ", " + persons +
                    ", " + lang + ", " + luggage + ").";
            prolog.asserta(query_client);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        /* Set the Client.closestNode to the closest node to the client from the graph */
        this.client.setClosestNode(closestNodeAt(this.client));
    }

    /* Read the Taxi Drivers from the file */
    /* For each taxi driver search for the closest point given in the graph
     * and use that as the initial taxi driver's location. If the taxi driver's
     * coordinates are included in the nodes.csv then his initial state will be
     * the location as given in taxis.csv. Else, it'll be the closest point in nodes.csv
     * We'll assume all given taxi driver's coordinates are reasonably close to the graph.*/
    private void readTaxis() {
        this.taxis = new ArrayList<>();
        BufferedReader reader = null;

        double x;
        double y;
        long id;
        Boolean isAvailable;
        int capacity;
        String[] languages;
        double rating;
        Boolean longDistances;
        String description;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(TAXIS_FILE))));
            String readLine;
            String[] split;

            reader.readLine(); // skip the header readLine
            while ((readLine = reader.readLine()) != null) {
                split = readLine.split(",");
                x = Double.parseDouble(split[0].trim());
                y = Double.parseDouble(split[1].trim());
                id = Long.parseLong(split[2].trim());
                isAvailable = (split[3].trim().compareTo("yes") == 0);

                capacity = 1;
                {
                    String[] split2 = split[4].trim().split("-");
                    if (split2.length == 2) {
                        capacity = Integer.parseInt(split2[1].trim());
                    }
                }

                languages = split[5].trim().split("\\|");
                rating = Double.parseDouble(split[6].trim());
                longDistances = (split[7].trim().compareTo("yes") == 0);
                description = String.valueOf(split[8].trim());
                Taxi taxi = new Taxi(x,y, id, isAvailable, capacity, languages, rating, longDistances, description);

                /* Set the Taxi.closestNode to the closest node to the taxi from the graph */
                taxi.setClosestNode(closestNodeAt(taxi));
                this.taxis.add(taxi);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public Node closestNodeAt (Point p) {
        double min = Double.MAX_VALUE;
        Node tmp = null;
        for (Node v : this.graph.vertexSet()) {
            double dist = p.euclid(v);
            if (dist < min) {
                tmp = v;
                min = dist;
            }
        }
        return tmp;
    }

    public SimpleWeightedGraph<Node, DefaultWeightedEdge> getGraph() {
        return graph;
    }

    public void setGraph(SimpleWeightedGraph<Node, DefaultWeightedEdge> graph) {
        this.graph = graph;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ArrayList<Taxi> getTaxis() {
        return taxis;
    }

    public void setTaxis(ArrayList<Taxi> taxis) {
        this.taxis = taxis;
    }
}
