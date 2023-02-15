package synthesizer.random;

import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;

public class Synthesizer {
    private String map; // should ideally be a map object; reading the file should not be handled by the synthesizer

    public Synthesizer(String map) {
        this.map = map;
    }
    public static void main(String[] args) {
        String inputMap = "maps/8x8/basesWorkers8x8A.xml"; // should be input to the method
        iDSL program = BuilderDSLTreeSingleton.getInstance().buildS1Grammar();
    }
}
