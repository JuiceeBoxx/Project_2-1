package com.mygdx.ann;

import com.mygdx.ann.RL;
import com.mygdx.game.bots.MCST.MCST;
import com.mygdx.game.bots.MCST.Node_MCST;
import com.mygdx.game.bots.RandomBot;
import com.mygdx.game.coordsystem.Hexagon;
import com.mygdx.game.rundev;
import com.mygdx.game.scoringsystem.ScoringEngine;
import com.mygdx.game.screens.GameScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MCSTLearning {
    private static final boolean DEBUG = true;
    public boolean player1;
    public ANN NET;


    public Hexagon.state getColour(GameScreen.state state){
        if(state == GameScreen.state.P1P1 || state == GameScreen.state.P2P1)
            return Hexagon.state.RED;
        else
            return Hexagon.state.BLUE;
    }

    // Find an available hexagon
    public List<Integer> available_moves(ArrayList<Hexagon> field){
        List<Integer> moves = new ArrayList<>();
        for (int i = 0; i < field.size(); i++) {
            if(field.get(i).getMyState()==Hexagon.state.BLANK){
                moves.add(i);
            }
        }
        return moves;
    }

    // Runs the MCTS algorithm for a fixed number of iterations and returns the best move
    public Node_MCST runMCST(ArrayList<Hexagon> field, GameScreen.state STATE, boolean Player1, ANN NET) {
        this.player1 = Player1;
        this.NET = NET;

        //for this to work the root node needs to be the state before the hexagon we want to place, fastest solution
        switch (STATE){
            case P1P1: STATE = GameScreen.state.P2P2;break;
            case P1P2: STATE = GameScreen.state.P1P1;break;
            case P2P1: STATE = GameScreen.state.P1P2;break;
            case P2P2: STATE = GameScreen.state.P2P1;break;
            default:
                throw new IllegalStateException("Unexpected value of the STATE");
        }

        int numIterations = 500;
        long start_time = System.nanoTime();
        long end_time = System.nanoTime();

        List<Integer> moves = available_moves(field);
        //here I assume the root node is always P1P1, we can change it when we call the method with different moves
        Node_MCST rootNode = new Node_MCST(field, moves,-1, STATE,1);
        int count=0;
        RL rl  = new RL();

        for (int i = 0; i < numIterations; i++) {
            //while((end_time-start_time)/1000000000<0.01){
            /*
            if(count%1000==0)
                System.out.println(count);
            count++;*/
            // Selection step: starting from the root node, traverse the tree using the UCB1 formula until a leaf node is reached
            end_time = System.nanoTime();

            Node_MCST currentNode = rootNode;


            while (!currentNode.isLeaf()) {
                currentNode = selectChild(currentNode);
            }

            Hexagon.state colour = getColour(currentNode.phase);
            ArrayList<Double> input = rl.getInputfromState(field,colour);
            ArrayList<Double> ymain = NET.execFP(input);


            // Expansion step: if the leaf node is not a terminal node, create child nodes for all possible moves and choose one at random
            if (!currentNode.isTerminal(currentNode.moves,field)) {
                currentNode = expandNode(currentNode,ymain);
            }
            // Backpropagation step: update the win counts and visit counts of all nodes on the path from the leaf to the root
            while (currentNode != null) {
                currentNode.visitCount++;
                currentNode.actionValue += (ymain.get(ymain.size()-1))/currentNode.visitCount;
                currentNode = currentNode.parent;
            }
        }

        // Return the move corresponding to the child node with the highest win rate

        return rootNode;
    }

    public Node_MCST selectChild(Node_MCST currentNode) {
        Node_MCST selectedChild = currentNode.children.get(0);
        double bestUCB1 = Double.NEGATIVE_INFINITY;
        for (Node_MCST child : currentNode.children) {

            double ucb1 = calcUCB1(child);
            //System.out.println(ucb1);
            if (ucb1 > bestUCB1) {

                bestUCB1 = ucb1;
                selectedChild = child;
            }
        }
        //System.out.println();
        return selectedChild;
    }
    double calcUCB1(Node_MCST node) {
        // Calculate the exploitation term
        double exploitation = node.actionValue;
        if (node.visitCount == 0) {
            exploitation = Double.POSITIVE_INFINITY;
        }
        //System.out.println("this is exploitation " + exploitation);

        // Calculate the exploration term
        double exploration = node.priorProb/(double)(1+node.visitCount);
        if (node.visitCount == 0) {
            exploration = Double.POSITIVE_INFINITY;
        }
        //System.out.println("this is exploration " + exploration);

        // Return the UCB1 score
        return exploitation + exploration;
    }

    Node_MCST expandNode(Node_MCST currentNode, ArrayList<Double> outputProb) {
        // Generate a list of all possible moves
        List<Integer> movescopy = new ArrayList<>();
        for (Integer move: currentNode.moves  ) {
            movescopy.add(move);
        }
        List<Integer> moves = movescopy;

        //System.out.println("different moves " + moves);
        GameScreen.state child_phase;
        switch (currentNode.phase){
            case P1P1: child_phase = GameScreen.state.P1P2;break;
            case P1P2: child_phase = GameScreen.state.P2P1;break;
            case P2P1: child_phase = GameScreen.state.P2P2;break;
            case P2P2: child_phase = GameScreen.state.P1P1;break;
            default:
                throw new IllegalStateException("Unexpected value: " + currentNode.phase);
        }

        for (Integer move_played : moves) {
            // Create a child node for each move
            ArrayList<Hexagon> copy_field = new ArrayList<Hexagon>();
            try {
                for(Hexagon h : currentNode.boardState) {
                    copy_field.add(h.clone());
                }
            } catch (Exception e) {}

            //TODO need the outputProb to be working correctly
            double value = outputProb.get(move_played);
            Node_MCST child = new Node_MCST(copy_field,moves,move_played,child_phase,value);

            //child.boardState = new ArrayList<Hexagon>(currentNode.boardState);
            if(child_phase==GameScreen.state.P1P1 || child_phase==GameScreen.state.P2P1)
                child.boardState.get(move_played).setMyState(Hexagon.state.RED);
            else if(child_phase==GameScreen.state.P1P2 || child_phase==GameScreen.state.P2P2){
                child.boardState.get(move_played).setMyState(Hexagon.state.BLUE);
            }
            else{
                throw new IllegalStateException("The children phase is not assign correctly: ");
            }
            //need to print out all the different moves per child
            child.moves = new ArrayList<Integer>(moves);
            //System.out.println(child.moves);
            child.moves.remove(move_played);

            child.parent = currentNode;
            currentNode.children.add(child);
        }

        //TODO probability need to double check

        double bestMove = Integer.MIN_VALUE;
        int bestChild = 0;
        for (int i = 0; i < currentNode.children.size(); i++) {
            if(currentNode.children.get(i).priorProb>bestMove){
                bestMove = currentNode.children.get(i).priorProb;
                bestChild = i;
            }
        }
        return currentNode.children.get(bestChild);

    }

    Node_MCST selectBestChild(Node_MCST currentNode) {
        Node_MCST bestChild = null;
        double bestWinRate = Double.NEGATIVE_INFINITY;
        for (Node_MCST child : currentNode.children) {
            double winRate = (double) child.winCount / child.visitCount;
            if (winRate > bestWinRate) {
                bestWinRate = winRate;
                bestChild = child;
            }
        }
        return bestChild;
    }
    public static void main(String[] args) {


        MCSTLearning mcst = new MCSTLearning();
        ArrayList<Hexagon> field = mcst.createHexagonFieldDefault();
        Node_MCST currentNode = new Node_MCST(field, new ArrayList<Integer>(),0, GameScreen.state.P1P1,1);
        System.out.println(currentNode.visitCount);
        System.out.println(currentNode.winCount);
        mcst.calcUCB1(currentNode);

    }
    public ArrayList<Hexagon> createHexagonFieldDefault() {
        int s;
        int fieldsize = 5;
        ArrayList<Hexagon> field = new ArrayList<>();
        for (int q = -fieldsize; q <= fieldsize; q++) {
            for (int r = fieldsize; r >= -fieldsize; r--) {
                s = -q - r;
                if (s <= fieldsize && s >= -fieldsize) {
                    field.add(new Hexagon(q, r, 50,0,0));
                }
            }
        }
        return field;
    }
}