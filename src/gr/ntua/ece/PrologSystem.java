package gr.ntua.ece;

import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

import java.io.IOException;

public class PrologSystem {
    private JIPEngine jip;
    private JIPTermParser parser;
    private JIPQuery jipQuery;
    private JIPTerm term;


    // PrologSystem as a singleton
    private static final PrologSystem instance = new PrologSystem();


    private PrologSystem() {
        try {
            jip = new JIPEngine();
            jip.consultFile("prolog.pl");
            parser = jip.getTermParser();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static PrologSystem getInstance() {
        return instance;
    }

    public void asserta(String predicate) {
         jip.asserta(parser.parseTerm(predicate));
    }

    public Boolean query(String predicate) {
        jipQuery = jip.openSynchronousQuery(parser.parseTerm(predicate));
        return (jipQuery.nextSolution() != null);
    }

    public boolean canMoveFromTo(Node A, Node B) {
        double Ax = A.getX();
        double Ay = A.getY();
        double Bx = B.getX();
        double By = B.getY();

        String queryString = "canMoveFromTo(" + Ax + "," + Ay + "," + Bx + ","  + By + ").";
        jipQuery = jip.openSynchronousQuery(parser.parseTerm(queryString));
        if (jipQuery.nextSolution() != null) {
            return true;
        } else {
            return false;
        }
    }

    public double calculateFactor(Node A, Node B) {
        double Ax = A.getX();
        double Ay = A.getY();
        double Bx = B.getX();
        double By = B.getY();

        String queryString = "weightFactor(" + Ax + "," + Ay + "," + Bx + ","  + By + ", Value).";
        jipQuery = jip.openSynchronousQuery(parser.parseTerm(queryString));
        term = jipQuery.nextSolution();
        if (term != null) {
            String factorString = term.getVariablesTable().get("Value").toString();
            double factor = Double.parseDouble(factorString);
            return factor;
        } else {
            System.out.println("Error with factor calculation.");
            return -1;
        }
    }

    public boolean isQualifiedDriver(int driverID) {
        String queryString = "isQualifiedDriverForClient(" + driverID + ").";
        jipQuery = jip.openSynchronousQuery(parser.parseTerm(queryString));
        if (jipQuery.nextSolution() != null) {
            return true;
        } else {
            return false;
        }
    }

    public double getDriverRank(int driverID) {
        String queryString = "driverRank(" + driverID + ",Rank).";
        jipQuery = jip.openSynchronousQuery(parser.parseTerm(queryString));
        term = jipQuery.nextSolution();
        if (term != null) {
            String rankString = term.getVariablesTable().get("Rank").toString();
            double rank = Double.parseDouble(rankString);
            return rank;
        } else {
            System.out.println("Error with rank calculation.");
            return -1;
        }
    }
}
