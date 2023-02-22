/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synthesizer.FicPlaySynthesizer;

import ai.synthesis.dslForScriptGenerator.DslAI;
import synthesizer.Synthesizer;
import synthesizer.FicPlaySynthesizer.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import synthesizer.FicPlaySynthesizer.synthesis.localsearch.LocalSearch;
import synthesizer.FicPlaySynthesizer.synthesis.localsearch.SimpleProgramSynthesisForFPTableV5;
import synthesizer.FicPlaySynthesizer.synthesis.localsearch.searchImplementation.SAForFPTableV5;

/**
 *
 * @author thaty, rubens
 */
public class FicPlaySynthesizer extends Synthesizer {
    private static String defaultName = "FicPlaySynthesizer";

    public FicPlaySynthesizer() {
        super.name = FicPlaySynthesizer.defaultName;
    }

    public DslAI generate(String map) { // TODO:
        SettingsAlphaDSL.setMode_debug(false);
        //SettingsAlphaDSL.setMAP(args[0]);
        SettingsAlphaDSL.setMAP(map);
        SettingsAlphaDSL.setAPPLY_RULES_REMOVE(false);
        SettingsAlphaDSL.setCLEAN_EMPTY(false);
        SettingsAlphaDSL.setNUMBER_SA_STEPS(5);

        System.out.println("Map " + SettingsAlphaDSL.get_map());

        SAForFPTableV5 FPtB = new SAForFPTableV5();
        LocalSearch skSAneal = new SimpleProgramSynthesisForFPTableV5(FPtB);
                
        skSAneal.performRun();
        return (DslAI)null;
    }
    
    public static void main(String[] args) {
        SettingsAlphaDSL.setMode_debug(false);
        //SettingsAlphaDSL.setMAP(args[0]);
        SettingsAlphaDSL.setMAP("maps/8x8/basesWorkers8x8A.xml");
        SettingsAlphaDSL.setAPPLY_RULES_REMOVE(false);
        SettingsAlphaDSL.setCLEAN_EMPTY(false);
        SettingsAlphaDSL.setNUMBER_SA_STEPS(5);
        
        System.out.println("Map " + SettingsAlphaDSL.get_map());
        
        SAForFPTableV5 FPtB = new SAForFPTableV5();
        LocalSearch skSAneal = new SimpleProgramSynthesisForFPTableV5(FPtB);
                
        skSAneal.performRun();
    }
    
}
