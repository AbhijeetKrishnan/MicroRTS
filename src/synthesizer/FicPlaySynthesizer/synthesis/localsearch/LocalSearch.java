/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synthesizer.FicPlaySynthesizer.synthesis.localsearch;

import java.util.List;

/**
 *
 * @author rubens
 */
public interface LocalSearch {
    /**
     * 
     * @return a list of information
     */
    public List<String> performRun();
    
}