/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.mcts.informedmcts;

import ai.*;
import ai.core.AI;
import ai.core.InterruptibleAIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.machinelearning.bayes.ActionInterdependenceModel;
import ai.machinelearning.bayes.BayesianModelByUnitTypeWithDefaultModel;
import ai.machinelearning.bayes.featuregeneration.FeatureGeneratorSimple;
import ai.stochastic.UnitActionProbabilityDistribution;
import ai.stochastic.UnitActionProbabilityDistributionAI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jdom.input.SAXBuilder;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 *
 * @author santi
 */
public class InformedNaiveMCTS extends InterruptibleAIWithComputationBudget {
    public static int DEBUG = 0;
    public EvaluationFunction ef = null;
    UnitTypeTable utt = null;
       
    Random r = new Random();
    public AI playoutPolicy = new RandomBiasedAI();
    UnitActionProbabilityDistribution bias = null;
    long max_actions_so_far = 0;
    
    GameState gs_to_start_from = null;
    InformedNaiveMCTSNode tree = null;
    int current_iteration = 0;
            
    public int MAXSIMULATIONTIME = 1024;
    public int MAX_TREE_DEPTH = 10;
    
    int player;
    
    public float epsilon_0 = 0.2f;
    public float epsilon_l = 0.25f;
    public float epsilon_g = 0.0f;

    // these variables are for using a discount factor on the epsilon values above. My experiments indicate that things work better without discount
    // So, they are just maintained here for completeness:
    public float initial_epsilon_0 = 0.2f;
    public float initial_epsilon_l = 0.25f;
    public float initial_epsilon_g = 0.0f;
    public float discount_0 = 0.999f;
    public float discount_l = 0.999f;
    public float discount_g = 0.999f;
    
    public int global_strategy = InformedNaiveMCTSNode.E_GREEDY;
    
    // statistics:
    public long total_runs = 0;
    public long total_cycles_executed = 0;
    public long total_actions_issued = 0;
    public long total_time = 0;
    
    
    public InformedNaiveMCTS(UnitTypeTable a_utt) throws Exception {
        this(100,-1,100,10,
             0.3f,0.0f,0.4f,
             new UnitActionProbabilityDistributionAI(
                    new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                        "data/bayesianmodels/pretrained/ActionInterdependenceModel-WR.xml").getRootElement(), a_utt,
                        new ActionInterdependenceModel(null, 0, 0, 0, a_utt, new FeatureGeneratorSimple(), ""), "AIM-WR"), 
                     a_utt, "ActionInterdependenceModel-Acc-WR"), 
             new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                    "data/bayesianmodels/pretrained/ActionInterdependenceModel-WR.xml").getRootElement(), a_utt,
                    new ActionInterdependenceModel(null, 0, 0, 0, a_utt, new FeatureGeneratorSimple(), ""), "AIM-WR"), 
             new SimpleSqrtEvaluationFunction3(), a_utt);
    }
    
    
    public InformedNaiveMCTS(int available_time, int max_playouts, int lookahead, int max_depth, 
                           float e1, float discout1,
                           float e2, float discout2, 
                           float e3, float discout3, 
                           AI policy, 
                           UnitActionProbabilityDistribution a_bias,
                           EvaluationFunction a_ef,
                           UnitTypeTable a_utt) {
        super(available_time, max_playouts);
        utt = a_utt;
        MAXSIMULATIONTIME = lookahead;
        playoutPolicy = policy;
        bias = a_bias;
        MAX_TREE_DEPTH = max_depth;
        initial_epsilon_l = epsilon_l = e1;
        initial_epsilon_g = epsilon_g = e2;
        initial_epsilon_0 = epsilon_0 = e3;
        discount_l = discout1;
        discount_g = discout2;
        discount_0 = discout3;
        ef = a_ef;
    }    

    public InformedNaiveMCTS(int available_time, int max_playouts, int lookahead, int max_depth, 
                           float e1, float e2, float e3, 
                           AI policy, 
                           UnitActionProbabilityDistribution a_bias,
                           EvaluationFunction a_ef,
                           UnitTypeTable a_utt) {
        super(available_time, max_playouts);
        utt = a_utt;
        MAXSIMULATIONTIME = lookahead;
        playoutPolicy = policy;
        bias = a_bias;
        MAX_TREE_DEPTH = max_depth;
        initial_epsilon_l = epsilon_l = e1;
        initial_epsilon_g = epsilon_g = e2;
        initial_epsilon_0 = epsilon_0 = e3;
        discount_l = 1.0f;
        discount_g = 1.0f;
        discount_0 = 1.0f;
        ef = a_ef;
    }    
    
    public InformedNaiveMCTS(int available_time, int max_playouts, int lookahead, int max_depth, 
                           float e1, float e2, float e3, int a_global_strategy, 
                           AI policy, 
                           UnitActionProbabilityDistribution a_bias,
                           EvaluationFunction a_ef) {
        super(available_time, max_playouts);
        MAXSIMULATIONTIME = lookahead;
        playoutPolicy = policy;
        bias = a_bias;
        MAX_TREE_DEPTH = max_depth;
        initial_epsilon_l = epsilon_l = e1;
        initial_epsilon_g = epsilon_g = e2;
        initial_epsilon_0 = epsilon_0 = e3;
        discount_l = 1.0f;
        discount_g = 1.0f;
        discount_0 = 1.0f;
        global_strategy = a_global_strategy;
        ef = a_ef;
    }        
    
    public void reset() {
        tree = null;
        gs_to_start_from = null;
        total_runs = 0;
        total_cycles_executed = 0;
        total_actions_issued = 0;
        total_time = 0;
        current_iteration = 0;
    }    
        
    
    public AI clone() {
        return new InformedNaiveMCTS(TIME_BUDGET, ITERATIONS_BUDGET, MAXSIMULATIONTIME, MAX_TREE_DEPTH, epsilon_l, discount_l, epsilon_g, discount_g, epsilon_0, discount_0, playoutPolicy, bias, ef, utt);
    }    
    
    
    public void startNewComputation(int a_player, GameState gs) throws Exception {
        player = a_player;
        current_iteration = 0;
        tree = new InformedNaiveMCTSNode(player, 1-player, gs, bias, null, ef.upperBound(gs), current_iteration++);
        
        max_actions_so_far = Math.max(tree.moveGenerator.getSize(),max_actions_so_far);
        gs_to_start_from = gs;
        
        epsilon_l = initial_epsilon_l;
        epsilon_g = initial_epsilon_g;
        epsilon_0 = initial_epsilon_0;        
    }    
    
    
    public void resetSearch() {
        if (DEBUG>=2) System.out.println("Resetting search...");
        tree = null;
        gs_to_start_from = null;
    }
    

    public void computeDuringOneGameFrame() throws Exception {        
        if (DEBUG>=2) System.out.println("Search...");
        long start = System.currentTimeMillis();
        long end = start;
        long count = 0;
        while(true) {
            if (!iteration(player)) break;
            count++;
            end = System.currentTimeMillis();
            if (TIME_BUDGET>=0 && (end - start)>=TIME_BUDGET) break; 
            if (ITERATIONS_BUDGET>=0 && count>=ITERATIONS_BUDGET) break;             
        }
//        System.out.println("HL: " + count + " time: " + (System.currentTimeMillis() - start) + " (" + available_time + "," + max_playouts + ")");
        total_time += (end - start);
        total_cycles_executed++;
    }
    
    
    public boolean iteration(int player) throws Exception {
        
        InformedNaiveMCTSNode leaf = tree.selectLeaf(player, 1-player, epsilon_l, epsilon_g, epsilon_0, global_strategy, MAX_TREE_DEPTH, current_iteration++);

        if (leaf!=null) {            
            GameState gs2 = leaf.gs.clone();
            simulate(gs2, gs2.getTime() + MAXSIMULATIONTIME);

            int time = gs2.getTime() - gs_to_start_from.getTime();
            double evaluation = ef.evaluate(player, 1-player, gs2)*Math.pow(0.99,time/10.0);

            leaf.propagateEvaluation(evaluation,null);            

            // update the epsilon values:
            epsilon_0*=discount_0;
            epsilon_l*=discount_l;
            epsilon_g*=discount_g;
            total_runs++;
            
//            System.out.println(total_runs + " - " + epsilon_0 + ", " + epsilon_l + ", " + epsilon_g);
            
        } else {
            // no actions to choose from :)
            System.err.println(this.getClass().getSimpleName() + ": claims there are no more leafs to explore...");
            return false;
        }
        return true;
    }
    
    public PlayerAction getBestActionSoFar() {
        int idx = getMostVisitedActionIdx();
        if (idx==-1) {
            if (DEBUG>=1) System.out.println("BiasedNaiveMCTS no children selected. Returning an empty asction");
            return new PlayerAction();
        }
        if (DEBUG>=2) tree.showNode(0,1,ef);
        if (DEBUG>=1) {
            InformedNaiveMCTSNode best = (InformedNaiveMCTSNode) tree.children.get(idx);
            System.out.println("BiasedNaiveMCTS selected children " + tree.actions.get(idx) + " explored " + best.visit_count + " Avg evaluation: " + (best.accum_evaluation/((double)best.visit_count)));
        }
        return tree.actions.get(idx);
    }
    
    
    public int getMostVisitedActionIdx() {
        total_actions_issued++;
            
        int bestIdx = -1;
        InformedNaiveMCTSNode best = null;
        if (DEBUG>=2) {
//            for(Player p:gs_to_start_from.getPlayers()) {
//                System.out.println("Resources P" + p.getID() + ": " + p.getResources());
//            }
            System.out.println("Number of playouts: " + tree.visit_count);
            tree.printUnitActionTable();
        }
        for(int i = 0;i<tree.children.size();i++) {
            InformedNaiveMCTSNode child = (InformedNaiveMCTSNode)tree.children.get(i);
            if (DEBUG>=2) {
                System.out.println("child " + tree.actions.get(i) + " explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)));
            }
//            if (best == null || (child.accum_evaluation/child.visit_count)>(best.accum_evaluation/best.visit_count)) {
            if (best == null || child.visit_count>best.visit_count) {
                best = child;
                bestIdx = i;
            }
        }
        
        return bestIdx;
    }
    
    
    public int getHighestEvaluationActionIdx() {
        total_actions_issued++;
            
        int bestIdx = -1;
        InformedNaiveMCTSNode best = null;
        if (DEBUG>=2) {
//            for(Player p:gs_to_start_from.getPlayers()) {
//                System.out.println("Resources P" + p.getID() + ": " + p.getResources());
//            }
            System.out.println("Number of playouts: " + tree.visit_count);
            tree.printUnitActionTable();
        }
        for(int i = 0;i<tree.children.size();i++) {
            InformedNaiveMCTSNode child = (InformedNaiveMCTSNode)tree.children.get(i);
            if (DEBUG>=2) {
                System.out.println("child " + tree.actions.get(i) + " explored " + child.visit_count + " Avg evaluation: " + (child.accum_evaluation/((double)child.visit_count)));
            }
//            if (best == null || (child.accum_evaluation/child.visit_count)>(best.accum_evaluation/best.visit_count)) {
            if (best == null || (child.accum_evaluation/((double)child.visit_count))>(best.accum_evaluation/((double)best.visit_count))) {
                best = child;
                bestIdx = i;
            }
        }
        
        return bestIdx;
    }
    
        
    public void simulate(GameState gs, int time) throws Exception {
        boolean gameover = false;

        do{
            if (gs.isComplete()) {
                gameover = gs.cycle();
            } else {
                gs.issue(playoutPolicy.getAction(0, gs));
                gs.issue(playoutPolicy.getAction(1, gs));
            }
        }while(!gameover && gs.getTime()<time);   
    }
    
    public InformedNaiveMCTSNode getTree() {
        return tree;
    }
    
    public GameState getGameStateToStartFrom() {
        return gs_to_start_from;
    }
    
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + MAXSIMULATIONTIME + ", " + MAX_TREE_DEPTH + ", " + epsilon_l + ", " + discount_l + ", " + epsilon_g + ", " + discount_g + ", " + epsilon_0 + ", " + discount_0 + ", " +  playoutPolicy + ", " + bias + ", " + ef + ")";
    }
    
    
    @Override
    public String statisticsString() {
        return "Total runs: " + total_runs + 
               ", runs per action: " + (total_runs/(float)total_actions_issued) + 
               ", runs per cycle: " + (total_runs/(float)total_cycles_executed) + 
               ", average time per cycle: " + (total_time/(float)total_cycles_executed) + 
               ", max branching factor: " + max_actions_so_far;
    }

    
    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("MaxTreeDepth",int.class,10));
        
        parameters.add(new ParameterSpecification("E_l",float.class,0.3));
        parameters.add(new ParameterSpecification("Discount_l",float.class,1.0));
        parameters.add(new ParameterSpecification("E_g",float.class,0.0));
        parameters.add(new ParameterSpecification("Discount_g",float.class,1.0));
        parameters.add(new ParameterSpecification("E_0",float.class,0.4));
        parameters.add(new ParameterSpecification("Discount_0",float.class,1.0));

        try {
            String biasNames[] = {
                "AIM-WR",
                "AIM-LR",
                "AIM-HR",
                "AIM-RR",
                "AIM-LSI500",
                "AIM-LSI10000",
                "AIM-NaiveMCTS500",
                "AIM-NaiveMCTS10000",                
            };
            UnitActionProbabilityDistribution biasOptions[] = {
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-WR.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-WR"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-LR.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-LR"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-HR.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-HR"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-RR.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-RR"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-LSI500.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-LSI500"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-LSI10000.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-LSI10000"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-NaiveMCTS500.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-NaiveMCTS500"),
                   new BayesianModelByUnitTypeWithDefaultModel(new SAXBuilder().build(
                       "data/bayesianmodels/pretrained/ActionInterdependenceModel-NaiveMCTS10000.xml").getRootElement(), utt,
                       new ActionInterdependenceModel(null, 0, 0, 0, utt, new FeatureGeneratorSimple(), ""), "AIM-NaiveMCTS10000"),
            }; 
        
            ParameterSpecification dp_ps = new ParameterSpecification("DefaultPolicy",AI.class, playoutPolicy);
            ParameterSpecification tpb_ps = new ParameterSpecification("TreePolicyBias",UnitActionProbabilityDistribution.class, bias);
            for(int i = 0;i<biasOptions.length;i++) {
                dp_ps.addPossibleValue(new UnitActionProbabilityDistributionAI(biasOptions[i], utt, biasNames[i]));
                tpb_ps.addPossibleValue(biasOptions[i]);
            }
            parameters.add(dp_ps);
            parameters.add(tpb_ps);
        } catch(Exception e) {
            e.printStackTrace();
        }
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));

        return parameters;
    }
    
    
    public int getPlayoutLookahead() {
        return MAXSIMULATIONTIME;
    }
    
    
    public void setPlayoutLookahead(int a_pola) {
        MAXSIMULATIONTIME = a_pola;
    }


    public int getMaxTreeDepth() {
        return MAX_TREE_DEPTH;
    }
    
    
    public void setMaxTreeDepth(int a_mtd) {
        MAX_TREE_DEPTH = a_mtd;
    }


    public float getE_l() {
        return epsilon_l;
    }
    
    
    public void setE_l(float a_e_l) {
        epsilon_l = a_e_l;
    }


    public float getDiscount_l() {
        return discount_l;
    }
    
    
    public void setDiscount_l(float a_discount_l) {
        discount_l = a_discount_l;
    }


    public float getE_g() {
        return epsilon_g;
    }
    
    
    public void setE_g(float a_e_g) {
        epsilon_g = a_e_g;
    }


    public float getDiscount_g() {
        return discount_g;
    }
    
    
    public void setDiscount_g(float a_discount_g) {
        discount_g = a_discount_g;
    }


    public float getE_0() {
        return epsilon_0;
    }
    
    
    public void setE_0(float a_e_0) {
        epsilon_0 = a_e_0;
    }


    public float getDiscount_0() {
        return discount_0;
    }
    
    
    public void setDiscount_0(float a_discount_0) {
        discount_0 = a_discount_0;
    }


    public AI getDefaultPolicy() {
        return playoutPolicy;
    }
    
    
    public void setDefaultPolicy(AI a_dp) {
        playoutPolicy = a_dp;
    }


    public UnitActionProbabilityDistribution getTreePolicyBias() {
        return bias;
    }
    
    
    public void setTreePolicyBias(UnitActionProbabilityDistribution a_bias) {
        bias = a_bias;
    }


    public EvaluationFunction getEvaluationFunction() {
        return ef;
    }
    
    
    public void setEvaluationFunction(EvaluationFunction a_ef) {
        ef = a_ef;
    }
}
