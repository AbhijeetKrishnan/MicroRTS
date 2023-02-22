package synthesizer.random;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.IDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.MainDSLCompiler;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import rts.units.UnitTypeTable;
import synthesizer.Synthesizer;

public class RandomSynthesizer extends Synthesizer {
    private static String defaultName = "RandomSynthesizer";

    public RandomSynthesizer() {
        super.name = RandomSynthesizer.defaultName;
    }

    public DslAI generate(String mapPath) {
        // generating a random AST
        iDSL program = BuilderDSLTreeSingleton.getInstance().buildS1Grammar();
        System.out.println(program.formatted_translation());
        
        // converting an AST to a runnable script (in memory only for now, no actual source code generated)
        UnitTypeTable utt = new UnitTypeTable();
        IDSLCompiler compiler = new MainDSLCompiler();
        HashMap<Long, String> counterByFunction = new HashMap<Long, String>();
        List<ICommand> commandsDSL = compiler.CompilerCode(program, utt);
        
        DslAI script = new DslAI(utt, commandsDSL, this.name + "-" + this.getMapName(mapPath), program, counterByFunction);
        return script;
    }
    public static void main(String[] args) {
        Path inputMapPath = Paths.get("maps/8x8/basesWorkers8x8A.xml"); // should be input to the method
        try {
            String map = Files.readString(inputMapPath);
            RandomSynthesizer syn = new RandomSynthesizer();
            DslAI script = syn.generate(map);
            System.out.println(script.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        // define location of other AI
        // obtain AI object
        // run match between opponent AI and script AI
    }
}
