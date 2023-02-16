package synthesizer.random;

import java.util.HashMap;
import java.util.List;

import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.core.AI;
import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.IDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.MainDSLCompiler;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import rts.units.UnitTypeTable;

public class Synthesizer {
    private String map; // should ideally be a map object; reading the file should not be handled by the synthesizer

    public Synthesizer(String map) {
        this.map = map;
    }
    public static void main(String[] args) {
        String inputMap = "maps/8x8/basesWorkers8x8A.xml"; // should be input to the method
        
        // generating a random AST
        iDSL program = BuilderDSLTreeSingleton.getInstance().buildS1Grammar();
        System.out.println(program.formatted_translation());
        
        // converting an AST to a runnable script (in memory only for now, no actual source code generated)
        UnitTypeTable utt = new UnitTypeTable();
        IDSLCompiler compiler = new MainDSLCompiler();
        HashMap<Long, String> counterByFunction = new HashMap<Long, String>();
        List<ICommand> commandsDSL = compiler.CompilerCode(program, utt);
        
        AI script = new DslAI(utt, commandsDSL, "P1", program, counterByFunction);
        System.out.println(script.toString());
    
        // define location of other AI
        // obtain AI object
        // run match between opponent AI and script AI
    }
}
