package com.mygdx.game.gametree;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.mygdx.game.coordsystem.Hexagon;
import com.mygdx.game.coordsystem.Hexagon.state;
import com.mygdx.game.scoringsystem.ScoringEngine;
import com.mygdx.game.screens.GameScreen;

public class Node {
    private ArrayList<Hexagon> field;
    private ArrayList<Node> listOfChildren;
    private int hexQ;
    private int hexR;
    private Hexagon.state hexState;
    private Node parent;
    private int depth;
    private double combinedScore;
    private double nodeScore;
    private int visitScore;
    private ArrayList<Hexagon> usedHex;
    private ScoringEngine SEngine = new ScoringEngine();
    private GameScreen.state Phase;

    //constructor for root.
    public Node(ArrayList<Hexagon> field, GameScreen.state Phase){
        parent = null;
        depth = 0;
        listOfChildren = new ArrayList<Node>();
        hexState = null;
        visitScore = 0;
        nodeScore = 0;
        combinedScore = 0;
        this.Phase = Phase;

        ArrayList<Hexagon> clone = new ArrayList<Hexagon>();
        try {
            for(Hexagon h : field) {
                clone.add(h.clone());
            }
        } catch (Exception e) {}
        this.field = clone;
    }

    // if node is a non-root node:
    public Node(Node Parent, int hexQ, int hexR, Hexagon.state statePlaced, GameScreen.state Phase){
        parent = Parent;
        depth = parent.depth + 1;
        listOfChildren = new ArrayList<Node>();
        hexState = statePlaced;
        this.Phase = Phase; //TODO AUTO CALCULATE THIS ON CREATION BASED ON PARENT PHASE

        ArrayList<Hexagon> clone = new ArrayList<Hexagon>();
        try {
            for(Hexagon h : parent.getField()) {
                clone.add(h.clone());
            }
        } catch (Exception e) {}

        this.field = clone;

        //actually place the piece on the board. (how can this be improved?) Hashmap?
        for(Hexagon hex:this.field){
            if(hex.getQ()==hexQ && hex.getR()==hexR){
                hex.setMyState(statePlaced);
            }
        }

        this.hexQ = hexQ;
        this.hexR = hexR;
        assignScore();
    }


    public void assignScore(){
        SEngine.calculate(field);

        nodeScore = SEngine.getRedScore();//TODO implement score? based on evaluation function rn its just red score.

        double childrenTotalScore = 0;
        for (Node n:
             listOfChildren) {
            childrenTotalScore+=n.getCombinedScore();
        }

        combinedScore = nodeScore+childrenTotalScore;
    }

    public ArrayList<Hexagon> getField(){
        return field;
    }

    public int getDepth(){
        return depth;
    }

    public Node getParent(){
        return parent;
    }

    public double getCombinedScore() {
        return combinedScore;
    }

    public List<Node> getChildArray(){
        return listOfChildren;
    }
    
    public int getQ(){
        return hexQ;
    }

    public int getR(){
        return hexR;
    }

    public Hexagon.state getState(){
        return hexState;
    }

    public void setState(Hexagon.state newState){
        this.hexState = newState;
    }

    public String toString(){ // could be expanded
        return("Depth: " + depth + " Score: " + nodeScore + " Q: " + hexQ + " R: " + hexR + " S: " + (-hexQ-hexR) + " State: " + hexState);
    }

    public void listChildren(){
        int sum = 0;
        for(Node node:listOfChildren){
            System.out.println(node.toString());
            sum++;
        }
        System.out.println("Total children: " + sum);
        if(listOfChildren.size() == 0){
            System.out.println("Array is EMPTY");
        }
    }
}