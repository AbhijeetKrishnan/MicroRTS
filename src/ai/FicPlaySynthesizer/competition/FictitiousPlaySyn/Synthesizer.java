/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FicPlaySynthesizer.competition.FictitiousPlaySyn;

import FicPlaySynthesizer.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import FicPlaySynthesizer.synthesis.localsearch.LocalSearch;
import FicPlaySynthesizer.synthesis.localsearch.SimpleProgramSynthesisForFPTableV5;
import FicPlaySynthesizer.synthesis.localsearch.searchImplementation.SAForFPTableV5;

/**
 *
 * @author thaty, rubens
 */
public class Synthesizer {
    
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
