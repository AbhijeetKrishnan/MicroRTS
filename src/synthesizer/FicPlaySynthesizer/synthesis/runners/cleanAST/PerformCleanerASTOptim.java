/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synthesizer.FicPlaySynthesizer.synthesis.runners.cleanAST;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.IDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.MainDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.interfacesDSL.iNodeDSLTree;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import synthesizer.FicPlaySynthesizer.synthesis.DslLeague.Runner.SettingsAlphaDSL;

/**
 *
 * @author rubens
 */
public class PerformCleanerASTOptim {

    //Smart Evaluation Settings
    static String initialState;
    private final static int CYCLES_LIMIT = 200;
    static List<iDSL> enemies_bat;

    public static iDSL clean_by_battles_against(iDSL principalAST, List<iDSL> enemies) {
        SettingsAlphaDSL.setAPPLY_RULES_REMOVE(true);
        SettingsAlphaDSL.setCLEAN_EMPTY(true);
        enemies_bat = enemies;
        try {            
            principalAST = run_clean_ast(principalAST);
        } catch (Exception ex) {
            Logger.getLogger(PerformCleanerASTOptim.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        SettingsAlphaDSL.setAPPLY_RULES_REMOVE(false);
        SettingsAlphaDSL.setCLEAN_EMPTY(false);
        return principalAST;
    }

    private static iDSL run_clean_ast(iDSL rec) throws Exception {
        String map = SettingsAlphaDSL.get_map();
        UnitTypeTable utt = new UnitTypeTable();
        PhysicalGameState pgs = PhysicalGameState.load(map, utt);

        //printMatchDetails(sIA1,sIA2,map);
        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 4000;
        int PERIOD = 20;
        boolean gameover = false;

        if (pgs.getHeight() == 8) {
            MAXCYCLES = 3000;
        }
        if (pgs.getHeight() == 16) {
            MAXCYCLES = 5000;
            //MAXCYCLES = 1000;
        }
        if (pgs.getHeight() == 24) {
            MAXCYCLES = 6000;
        }
        if (pgs.getHeight() == 32) {
            MAXCYCLES = 7000;
        }
        if (pgs.getHeight() == 64) {
            MAXCYCLES = 12000;
        }

        AI ai = buildCommandsIA(utt, rec);

        List<AI> bot_ais;
        bot_ais = new ArrayList(Arrays.asList(new AI[]{
            new WorkerRush(utt),
            new HeavyRush(utt),
            new RangedRush(utt),
            new LightRush(utt), 
        }));
        for (iDSL dSL : enemies_bat) {
            bot_ais.add(buildCommandsIA(utt, dSL));
        }

        int lim = 1;
        int val = 0;
        for (int i = 0; i < lim; i++) {
            for (AI bot_ai : bot_ais) {
                val += run_match(MAXCYCLES, ai, bot_ai, map, utt, PERIOD, gs.clone());
            }

            for (AI bot_ai : bot_ais) {
                val += run_match(MAXCYCLES, bot_ai, ai, map, utt, PERIOD, gs.clone());
            }
        }

        //System.out.println("Total Score: "+ val);
        rec = ReduceDSLController.removeUnactivatedParts(rec, new ArrayList<>(((DslAI) ai).getCommands()));
        //System.out.println(rec.translate());
        ai = buildCommandsIA(utt, rec);
        val = 0;
        for (int i = 0; i < lim; i++) {
            for (AI bot_ai : bot_ais) {
                val += run_match(MAXCYCLES, ai, bot_ai, map, utt, PERIOD, gs.clone());
            }

            for (AI bot_ai : bot_ais) {
                val += run_match(MAXCYCLES, bot_ai, ai, map, utt, PERIOD, gs.clone());
            }
        }
        //System.out.println("Total Score: "+ val);
        return rec;
    }

    private static AI buildCommandsIA(UnitTypeTable utt, iDSL code) {
        IDSLCompiler compiler = new MainDSLCompiler();
        HashMap<Long, String> counterByFunction = new HashMap<Long, String>();
        List<ICommand> commandsDSL = compiler.CompilerCode(code, utt);
        AI aiscript = new DslAI(utt, commandsDSL, "P1", code, counterByFunction);
        return aiscript;
    }

    private static AI buildCommandsIA2(UnitTypeTable utt, iDSL code) {
        IDSLCompiler compiler = new MainDSLCompiler();
        HashMap<Long, String> counterByFunction = new HashMap<Long, String>();
        List<ICommand> commandsDSL = compiler.CompilerCode(code, utt);
        AI aiscript = new DslAI(utt, commandsDSL, "P_Init", code, counterByFunction);
        return aiscript;
    }

    private static int run_match(int MAXCYCLES, AI ai1, AI ai2, String map, UnitTypeTable utt, int PERIOD, GameState gs) throws Exception {
        //System.out.println(ai1 + "   " + ai2);
        boolean gameover = false;
        //JFrame w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
        do {
            PlayerAction pa1 = ai1.getAction(0, gs);
            PlayerAction pa2 = ai2.getAction(1, gs);
            gs.issueSafe(pa1);
            gs.issueSafe(pa2);
            // simulate:
            if (smartEvaluationForStop(gs)) {
                gameover = true;
            } else {
                gameover = gs.cycle();
            }

            //w.repaint();            
        } while (!gameover && (gs.getTime() <= MAXCYCLES));
        //System.out.println("Winner: " + gs.winner());
        return gs.winner();
    }

    private static boolean smartEvaluationForStop(GameState gs) {
        if (gs.getTime() == 0) {
            String cleanState = cleanStateInformation(gs);
            initialState = cleanState;
        } else if (gs.getTime() % CYCLES_LIMIT == 0) {
            String cleanState = cleanStateInformation(gs);
            if (cleanState.equals(initialState)) {
                //System.out.println("** Smart Stop activate.\n Original state =\n"+initialState+
                //        " verified same state at \n"+cleanState);
                return true;
            } else {
                initialState = cleanState;
            }
        }
        return false;
    }

    private static String cleanStateInformation(GameState gs) {
        String sGame = gs.toString().replace("\n", "");
        sGame = sGame.substring(sGame.indexOf("PhysicalGameState:"));
        sGame = sGame.replace("PhysicalGameState:", "").trim();
        return sGame;
    }
}
