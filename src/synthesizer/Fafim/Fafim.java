package synthesizer.Fafim;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.utils.ReduceDSLController;
import ai.synthesis.grammar.dslTree.utils.SerializableController;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import synthesizer.Synthesizer;

public class Fafim extends Synthesizer {
    private static String defaultName = "Fafim";

    static String path_map = "maps/mapadavid2.xml";
    static String script_name = "out1.ser";

    static String curriculumportfolio = "empty";;
    static Random rand = new Random();
    static UnitTypeTable utt;
    static List<iDSL> sIA1;
    static List<iDSL> Camp;
    static int n_filho = 40;
    static int n_ficticions = 8;
    static int n_ficticions2 = 15;
    static int erros = 30;

    static double horas = 24;

    static int max_cicle = 5000;
    static int id_map = 0;
    static File arq;
    static PrintWriter gravarArq;
    static int cont = 0;
    static UnitType workerType;
    static UnitType baseType;
    static UnitType barracksType;
    static UnitType lightType;
    static UnitType heavyType;
    static UnitType rangedType;
    static EvaluationFunction evaluation = new SimpleSqrtEvaluationFunction3();

    public Fafim() {
        super.name = Fafim.defaultName;
    }
    
    public DslAI generate(String map) { // TODO:
        return (DslAI)null;
    }
    
    public static void main(String[] args) throws Exception {
        utt = new UnitTypeTable();
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");

        PhysicalGameState pgs = PhysicalGameState.load(path_map, utt);
        Camp = new ArrayList<>();
        GameState gs = new GameState(pgs, utt);
        long Tini = System.currentTimeMillis();
        int cont = 0;
        boolean parada = true;

        if (pgs.getHeight() == 8) {
            max_cicle = 2000;
            n_filho = 80;
            n_ficticions = 10;
            erros = 30;
        }
        if (pgs.getHeight() == 16) {
            max_cicle = 3000;
            n_filho = 80;
            n_ficticions = 10;
            erros = 20;
        }
        if (pgs.getHeight() == 24) {
            max_cicle = 5000;
            n_filho = 40;
            n_ficticions = 5;
            erros = 15;
        }
        if (pgs.getHeight() == 32) {
            max_cicle = 7000;
            n_filho = 40;
            n_ficticions = 5;
            erros = 15;
        }
        if (pgs.getHeight() == 64) {
            max_cicle = 12000;
            n_filho = 20;
            n_ficticions = 3;
            erros = 20;
        }

        while (parada) {

            sIA1 = new ArrayList<>();

            Busca(gs.clone(), max_cicle, Tini);

            if (sIA1.size() != 0) {
                iDSL LSFinal0 = sIA1.get(sIA1.size() - 1);
                System.out.println(LSFinal0.translate());
                Avalia(LSFinal0, gs, max_cicle);
                // SerializableController.saveSerializable(LSFinal0, "dsl_test_id_.ser", "./");
                cont++;
            }

            long parar = System.currentTimeMillis() - Tini;
            if ((parar * 1.0) / 1000.0 > horas * 3600.0) {

                parada = false;
                break;
            }

        }

    }

    private static void Avalia(iDSL lSFinal0, GameState gs, int limite) throws InterruptedException {
        // TODO Auto-generated method stub
        System.out.println("tt " + Camp.size());
        if (Camp.size() == 0) {
            Camp.add(lSFinal0);
            return;
        }
        AvaliaIndividual v = new AvaliaIndividual(lSFinal0, Camp, 0, gs, utt, limite, 0);
        AvaliaIndividual v2 = new AvaliaIndividual(Camp.get(Camp.size() - 1), Camp, 0, gs, utt, limite, 0);
        v.start();
        v2.start();
        v.join();
        v2.join();
        System.out.println(v.result + " " + v2.result);
        if (v.result > v2.result) {
            if (Camp.size() > n_ficticions2)
                Camp.remove(0);
            Camp.add(lSFinal0);
            SerializableController.saveSerializable(lSFinal0, script_name, "./");

        }
    }

    public static boolean Busca(GameState gs, int limite, long fim) throws Exception {

        if (sIA1.size() == 0) {
            iDSL a1 = BuilderDSLTreeSingleton.getInstance().buildS1Grammar();
            a1 = BuilderDSLTreeSingleton.changeNeighbourPassively(a1);

            sIA1.add(a1);

        }

        int atualiza = 0;
        int oo = 0;
        boolean terminou = true;
        while (atualiza < erros && oo < 30) {
            long parar = System.currentTimeMillis() - fim;
            if ((parar * 1.0) / 1000.0 > 10 * 3600.0) {
                return false;

            }
            long inicio = System.currentTimeMillis();
            terminou = true;
            oo++;

            if (getMelhor(gs, sIA1, limite)) {
                atualiza = 0;
                oo = 0;
            } else {
                atualiza += 1;
                oo += 1;
            }

            long final2 = System.currentTimeMillis() - inicio;
            double tempo_total = (final2 * 1.0) / 1000.0;

        }

        return !terminou;

    }

    public static boolean getMelhor(GameState gs, List<iDSL> ais, int limite) throws Exception {

        iDSL c1 = ais.get(ais.size() - 1);

        int id1 = 0;

        boolean atualizou = false;

        double melhor = -10000000;

        List<AvaliaIndividual> jogos = new ArrayList<>();
        ;

        jogos.add(new AvaliaIndividual(c1, ais, 0, gs, utt, limite, 0));
        jogos.get(0).start();

        for (int i = 0; i < n_filho; i++) {
            jogos.add(new AvaliaIndividual(c1, ais, 0, gs, utt, limite, 1));
            jogos.get(jogos.size() - 1).run();

        }

        for (AvaliaIndividual j : jogos) {
            j.join();
        }

        melhor = jogos.get(0).result;

        // System.out.println("Lista 0 "+c1.translate());
        int i = 0;
        for (AvaliaIndividual j : jogos) {

            if (melhor < j.result) {
                melhor = j.result;
                id1 = i;
                //
            }
            // System.out.println(j.result+" "+j.sIA1.translate());
            i++;

        }

        if (id1 != 0) {
            if (ais.size() > n_ficticions)
                ais.remove(0);
            ReduceDSLController.removeUnactivatedParts(jogos.get(id1).sIA1,
                    new ArrayList<>(((DslAI) jogos.get(id1).ai1).getCommands()));
            // System.out.println(jogos.get(id1).result+" rr
            // "+jogos.get(id1).sIA1.translate());
            ais.add(jogos.get(id1).sIA1);
            atualizou = true;
        }

        return atualizou;

    }

}
