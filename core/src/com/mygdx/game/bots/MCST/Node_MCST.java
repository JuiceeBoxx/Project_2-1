package com.mygdx.game.bots.MCST;

import com.mygdx.game.coordsystem.Hexagon;
import com.mygdx.game.gametreemc.Node;
import com.mygdx.game.screens.GameScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node_MCST {

    protected ArrayList<Hexagon> boardState;
    protected Node_MCST parent;
    protected List<Integer> moves;
    public int move_played;
    protected int winCount;
    protected int visitCount;
    protected List<Node_MCST> children;
    public GameScreen.state phase;

    Node_MCST(ArrayList<Hexagon> field, List<Integer> moves,int move_played, GameScreen.state phase){
            this.boardState = field;
            this.parent = null;
            this.moves = moves;
            this.move_played = move_played;
            winCount = 0;
            this.phase = phase;

            visitCount = 0;
            children = new ArrayList<>();
    }

    public boolean isLeaf(){
        return children.size() == 0;
    }
    public boolean isTerminal(List<Integer> moves){
        return moves.size() < 4;
    }
}