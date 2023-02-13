

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.IDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.MainDSLCompiler;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;

import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class AvaliaIndividual extends Thread {

	 IDSLCompiler compiler = new MainDSLCompiler();    
	    HashSet<String> usedCommands;
	
	    public iDSL sIA1;
	    List<iDSL> sIA2;
	    public float result;
	    int play;
	    int winner;
	    //Smart Evaluation Settings
	 
	    List<ICommand> allCommandIA1;
	    List<ICommand> allCommandIA2;
	    GameState gs2;
	    PhysicalGameState pgs;
	    UnitTypeTable utt;
	    public AI ai1;
	    int op_mutacao;
	    
	    
	    EvaluationFunction evaluation = new SimpleSqrtEvaluationFunction3();
	    boolean  acabou= true;
	
	
	    int MAXCYCLES = 4000;

	    public float getResult() {
	        return result;
	    }

	    public int getWinner() {
	        return winner;
	    }

	    public AvaliaIndividual(iDSL sIA1, List<iDSL> sIA2,int play, GameState gs,  UnitTypeTable utt,int limite,int op_mutacao) {
	        this.sIA1 = sIA1;
	        this.sIA2 = sIA2;
	       this.allCommandIA1 = new ArrayList<>();
	        this.allCommandIA2 = new ArrayList<>();
	        this.winner = -1;
	        this.gs2=gs;
	        this.play=play;
	  
	        this.utt=utt;
	        this.op_mutacao =op_mutacao;
	        this.MAXCYCLES = limite;
	        
	       
	    }

	    public List<ICommand> getAllCommandIA1() {
	        return allCommandIA1;
	    }

	    public List<ICommand> getAllCommandIA2() {
	        return allCommandIA2;
	    }
	    
	
	    public int execute() throws Exception {   
	    	
	    	if(op_mutacao==0)sIA1 = (iDSL)sIA1.clone();
	    	if(op_mutacao==1)sIA1 =BuilderDSLTreeSingleton.changeNeighbourPassively((iDSL) sIA1.clone());
	    	 
	    	ai1 = buildCommandsIA(utt, sIA1);
	    	 this.result=0;
	    	for(int lado =0;lado<2;lado++) {
	    		
		    	for (iDSL ch : sIA2) {
			        boolean gameover = false;
			       
			     
			        GameState gs3=gs2.cloneChangingUTT(utt);
			        AI ai2 = buildCommandsIA(utt, ch);
			      
			        do {
			       
			                PlayerAction pa1 = ai1.getAction(lado, gs3);
			                PlayerAction pa2 = ai2.getAction(1-lado, gs3);
			                gs3.issueSafe(pa1);
			                gs3.issueSafe(pa2);
			                /*
			                if (smartEvaluationForStop(gs2)) {
			                    this.allCommandIA1.clear();
			                    this.allCommandIA1.addAll(((DslAI) ai1).getCommands());
			                    this.allCommandIA2.clear();
			                    this.allCommandIA2.addAll(((DslAI) ai2).getCommands());
			                    result = 0.5f;    
			                    
			                    return -1;
			                }
			                */
			              
			                gameover = gs3.cycle();
			             
			        } while (!gameover && (gs3.getTime() <= MAXCYCLES));     
			       
			       
			        
			        winner = gs3.winner();
			       // this.allCommandIA1.clear();
			       // this.allCommandIA1.addAll(((DslAI) ai1).getCommands());
			       // this.allCommandIA2.clear();
			       // this.allCommandIA2.addAll(((DslAI) ai2).getCommands());  
			       if (winner==-1)acabou=false;
			     
			        this.result +=  evaluation.evaluate(lado, 1- lado, gs3);
		       
		    	}
		    	
	    	}
	
	        return -1;
	    }

	    private AI buildCommandsIA(UnitTypeTable utt, iDSL code) {
	        HashMap<Long, String> counterByFunction = new HashMap<Long, String>();
	        List<ICommand> commandsDSL = compiler.CompilerCode(code, utt);
	        AI aiscript = new DslAI(utt, commandsDSL, "P1", code, counterByFunction);
	        return aiscript;
	    }

	   

	    @Override
	    public void run() {
	        try {
	            this.winner = execute();
	        } catch (Exception ex) {
	          
	        }
	    }
	  
	
	
}
