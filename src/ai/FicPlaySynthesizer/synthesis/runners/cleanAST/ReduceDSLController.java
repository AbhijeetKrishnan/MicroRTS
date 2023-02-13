/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FicPlaySynthesizer.synthesis.runners.cleanAST;

import ai.core.AI;
import FicPlaySynthesizer.synthesis.DslLeague.Runner.SettingsAlphaDSL;
import ai.synthesis.dslForScriptGenerator.DSLCommand.AbstractBasicAction;
import ai.synthesis.dslForScriptGenerator.DSLCommand.DSLBasicBoolean.IfFunction;
import ai.synthesis.dslForScriptGenerator.DSLCommand.DSLBasicLoops.ForFunction;
import ai.synthesis.dslForScriptGenerator.DSLCommandInterfaces.ICommand;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.IDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DSLCompiler.MainDSLCompiler;
import ai.synthesis.dslForScriptGenerator.DslAI;
import ai.synthesis.grammar.dslTree.CDSL;
import ai.synthesis.grammar.dslTree.CommandDSL;
import ai.synthesis.grammar.dslTree.EmptyDSL;
import ai.synthesis.grammar.dslTree.S1DSL;
import ai.synthesis.grammar.dslTree.S2DSL;
import ai.synthesis.grammar.dslTree.S3DSL;
import ai.synthesis.grammar.dslTree.S4DSL;
import ai.synthesis.grammar.dslTree.S5DSL;
import ai.synthesis.grammar.dslTree.S5DSLEnum;
import ai.synthesis.grammar.dslTree.builderDSLTree.BuilderDSLTreeSingleton;
import ai.synthesis.grammar.dslTree.interfacesDSL.iCommandDSL;
import ai.synthesis.grammar.dslTree.interfacesDSL.iDSL;
import ai.synthesis.grammar.dslTree.interfacesDSL.iEmptyDSL;
import ai.synthesis.grammar.dslTree.interfacesDSL.iNodeDSLTree;
import ai.synthesis.grammar.dslTree.interfacesDSL.iS1ConstraintDSL;
import ai.synthesis.grammar.dslTree.interfacesDSL.iS4ConstraintDSL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 *
 * @author rubens
 */
public class ReduceDSLController {

    private final static boolean DEBUG = false;

    public static iDSL removeUnactivatedParts(iDSL dsl, List<ICommand> commands) {
        if (SettingsAlphaDSL.get_apply_rules_remove()) {
//            System.out.println("------------------------------------------------------------");        
            //get all unactivated elements
            List<iDSL> parts = getUnactivatedParts(commands);
            //System.out.println("        -- Old DSL " + dsl.translate());
            //log -- remove this
//            for (iDSL part : parts) {
//                System.out.println("        -- Part to remove " + part.translate());
//            }

            //remove the iDSL fragments from the DSL.
            //removeParts(parts);
            iDSL original = (iDSL) dsl.clone();
            try {
                for (iDSL part : parts) {
                    //System.out.println("        -- Removed frag " + part.translate());
                    List<iDSL> tp = new ArrayList<>();
                    tp.add(part);
                    removeParts(tp);
                    checkAndRemoveInconsistentIf(dsl);

                    //log                
                    //BuilderDSLTreeSingleton.formatedStructuredDSLTreePreOrderPrint((iNodeDSLTree) dsl);
                    //System.out.println("  ---*****---   ");
                }
                dsl = removeEmptyFromDSL(dsl);
            } catch (Exception e) {
                dsl = original;
                dsl = removeEmptyFromDSL(dsl);
                return dsl;
            }

            //check if the DSL continues working.            
            //verifyIntegrity((iNodeDSLTree) dsl);
            //check for if without then and else.
//            if (dsl.translate().contains("if")) {   
//                BuilderDSLTreeSingleton.formatedStructuredDSLTreePreOrderPrint((iNodeDSLTree) dsl);
//                checkAndRemoveInconsistentIf(dsl);
//            }
            //BuilderDSLTreeSingleton.formatedStructuredDSLTreePreOrderPrint((iNodeDSLTree) dsl);
//            if (dsl.translate().contains("for(u) ()")) {
//                removeInconsistentFor(dsl);
//            }
            //log -- remove this
            //System.out.println("        -- New DSL " + dsl.translate());
            //BuilderDSLTreeSingleton.formatedStructuredDSLTreePreOrderPrint((iNodeDSLTree) dsl);
//            System.out.println("--------------------------------\n");
        }
        return dsl;
    }

    private static List<iDSL> getUnactivatedParts(List<ICommand> commands) {
        //List<iDSL> tparts = new ArrayList<>();
        HashSet<iDSL> tparts = new HashSet<>();
        for (ICommand command : commands) {
            if (command instanceof AbstractBasicAction) {
                AbstractBasicAction t = (AbstractBasicAction) command;
                if (!t.isHasDSLUsed()) {
                    tparts.add(t.getDslFragment());
                }
            } else if (command instanceof ForFunction) {
                ForFunction t = (ForFunction) command;
                tparts.addAll(getUnactivatedParts(t.getCommandsFor()));
            } else if (command instanceof IfFunction) {
                IfFunction t = (IfFunction) command;
                tparts.addAll(getUnactivatedParts(t.getCommandsThen()));
                if (!t.getCommandsElse().isEmpty()) {
                    tparts.addAll(getUnactivatedParts(t.getCommandsElse()));
                }
            }
        }
        return new ArrayList<>(tparts);
    }

    private static void removeParts(List<iDSL> parts) {
        for (iDSL part : parts) {
            removeFromFather((iNodeDSLTree) part);
        }
    }

    private static void removeFromFather(iNodeDSLTree part) {
        iNodeDSLTree father = (iNodeDSLTree) part.getFather();
        if (father.getRightNode() == part) {
            father.removeRightNode();
        } else if (father.getLeftChild() == part) {
            father.removeLeftNode();
        }
        //check integrity
        verifyIntegrity(father);
    }

    private static void verifyIntegrity(iNodeDSLTree part) {
        //if father is a S2DSL check if then exists. If not, check if the S2DSL will
        //be removed or if the else will replace his parent.
        if (part instanceof S2DSL) {
            S2DSL inst = (S2DSL) part;
            //check if the command will be removed
            if (inst.getThenCommand() == null && inst.getElseCommand() == null) {
                removeFromFather(part);
            } else if (inst.getThenCommand() == null && inst.getElseCommand() != null
                    && !(inst.getElseCommand() instanceof iEmptyDSL)) {
                /*
                 Else will be changed to then and the boolean will be converted
                 */
                inst.setThenCommand(inst.getElseCommand());
                inst.setElseCommand(null);
                //changes the boolen
                S5DSL boo = (S5DSL) inst.getBoolCommand();
                if (boo.getNotFactor() == S5DSLEnum.NONE) {
                    boo.setNotFactor(S5DSLEnum.NOT);
                } else if (boo.getNotFactor() == S5DSLEnum.NOT) {
                    boo.setNotFactor(S5DSLEnum.NONE);
                }
                inst.setBoolCommand(boo);
            }
        } else if (part instanceof S3DSL) {//if father is a S3DSL and all commands were removed, remove for. 
            S3DSL fInst = (S3DSL) part;
            if (fInst.getForCommand() == null) {
                removeS3DSLInFather(fInst.getFather(), fInst);
            }
        } else if (part instanceof S4DSL) {//if father is a S4DSL without commands inside, remove it.
            S4DSL s4 = (S4DSL) part;
            if (s4.getRightChild() == null
                    && s4.getLeftChild() == null) {
                removeS4DSLInfather(s4.getFather(), s4);
            } else if (s4.getLeftChild() == null && s4.getRightChild() instanceof S4DSL) {
                iDSL grandS4 = s4.getFather();
                if (grandS4 instanceof S4DSL) {
                    S4DSL tgrandS4 = (S4DSL) grandS4;
                    if (tgrandS4.getRightChild() == s4) {
                        tgrandS4.setNextCommand((S4DSL) s4.getRightChild());
                    }
                } else if (grandS4 instanceof S3DSL && s4.getLeftChild() instanceof S4DSL) {
                    S3DSL grandF = (S3DSL) grandS4;
                    grandF.setForCommand((S4DSL) s4.getLeftChild());
                }
            }
        } else if (part instanceof CDSL) {//if CDSL has null element check if it will be removed ou reorganized.
            CDSL c = (CDSL) part;
            //remove if left and right is null
            if (c.getRightChild() == null && c.getLeftChild() == null) {
                removeFromFather(part);
            } else if (c.getLeftNode() == null && c.getRightNode() != null) {
                //change father by CDSL in left
                changeActualCDSLByLeftCDSL(c.getFather(), c, (iDSL) c.getRightNode());
            }
        } else if (part instanceof S1DSL) {
            S1DSL s1 = (S1DSL) part;
            if (s1.getCommandS1() == null && s1.getNextCommand() == null) {
                s1.setCommandS1(new EmptyDSL());
            } else if (s1.getCommandS1() == null && s1.getNextCommand() != null
                    && s1.getNextCommand() instanceof iS1ConstraintDSL) {
                s1.setCommandS1((iS1ConstraintDSL) s1.getNextCommand());
                s1.removeLeftNode();
            } else if (s1.getCommandS1() == null) {
                s1.setCommandS1(new EmptyDSL());
            } else if (s1.getCommandS1() instanceof iEmptyDSL && s1.getNextCommand() != null
                    && !(s1.getNextCommand().getCommandS1() instanceof EmptyDSL)) {
                if (s1.getNextCommand().getCommandS1() instanceof iS1ConstraintDSL) {
                    s1.setCommandS1(s1.getNextCommand().getCommandS1());
                    s1.removeLeftNode();
                } else {
                    if (DEBUG) {
                        System.err.println(s1.getNextCommand().translate());
                        System.err.println(s1.getNextCommand());
                    }

                }
            }
            if (s1.getFather() != null && s1.getFather() instanceof S1DSL) {
                verifyIntegrity((iNodeDSLTree) s1.getFather());
            }
        }

    }

    private static void removeS2DSLInFather(iDSL father, S2DSL ifToRemove, CDSL CommandToReplace) {
        //by theory the father of a S2DSL is a S1DSL, if we change is in the future, we need to modify this method.
        if (!(father instanceof S1DSL) && !(father instanceof S4DSL)) {
            if (DEBUG) {
                System.err.println("Problem at removeS2DSLInFather.");
                System.err.println(father.translate());
            }
            return;
        }
        if (father instanceof S1DSL) {
            S1DSL part = (S1DSL) father;
            //it is always true, but I'll keep it for safe.
            if (part.getCommandS1() == ifToRemove) {
                part.setCommandS1(CommandToReplace);
            } else {
                if (DEBUG) {
                    System.err.println("Problem at removeS2DSLInFather for replace iftoRemove.");
                }
            }
        } else if (father instanceof S4DSL) {
            S4DSL s4f = (S4DSL) father;
            s4f.setFirstDSL(CommandToReplace);
        }

    }

    private static void removeS3DSLInFather(iDSL father, S3DSL forToRemove) {
        //by theory the father of a S3DSL is a S1DSL, if we change is in the future, we need to modify this method.
        if (!(father instanceof S1DSL)) {
            if (DEBUG) {
                System.err.println("Problem at removeS3DSLInFather.");
                System.err.println(father.translate());
            }
            return;
        }
        S1DSL part = (S1DSL) father;
        if (part.getCommandS1() == forToRemove) {
            part.setCommandS1(new EmptyDSL());
        } else {
            if (DEBUG) {
                System.err.println("Problem at removeS3DSLInFather for replace forToRemove.");
            }
        }
    }

    private static void removeS4DSLInfather(iDSL father, S4DSL s4ToRemove) {
        if (father instanceof S4DSL) {
            S4DSL s4Father = (S4DSL) father;
            if (s4Father.getRightChild() == s4ToRemove) {
                s4Father.removeRightNode();
                verifyIntegrity(s4Father);
            }
        } else if (father instanceof S3DSL) {
            S3DSL s3father = (S3DSL) father;
            if (s3father.getForCommand() == s4ToRemove) {
                s3father.removeLeftNode();
                removeS3DSLInFather(s3father.getFather(), s3father);
            }
        }
    }

    private static void changeActualCDSLByLeftCDSL(iDSL father, CDSL toRemove, iDSL toReplace) {
        if (father instanceof S1DSL) {
            S1DSL s1 = (S1DSL) father;
            if (s1.getRightChild() == toRemove) {
                s1.setCommandS1((iS1ConstraintDSL) toReplace);
            }
        } else if (father instanceof S2DSL) {
            S2DSL s2 = (S2DSL) father;
            if (s2.getThenCommand() == toRemove) {
                if (toReplace instanceof CDSL) {
                    s2.setThenCommand((CDSL) toReplace);
                } else {
                    s2.setThenCommand(new CDSL((iCommandDSL) toReplace));
                }
            } else if (s2.getElseCommand() == toRemove) {

                if (toReplace instanceof CDSL) {
                    s2.setElseCommand((CDSL) toReplace);
                } else {
                    s2.setElseCommand(new CDSL((iCommandDSL) toReplace));
                }
            }
        } else if (father instanceof S4DSL) {
            S4DSL s4 = (S4DSL) father;
            if (s4.getFirstDSL() == toRemove) {
                if (toReplace instanceof iS4ConstraintDSL) {
                    s4.setFirstDSL((iS4ConstraintDSL) toReplace);
                } else if (toReplace instanceof CommandDSL) {
                    s4.setFirstDSL((iS4ConstraintDSL) new CDSL((iCommandDSL) toReplace));
                }

            }
        } else if (father instanceof CDSL) {
            CDSL c = (CDSL) father;
            if (c.getNextCommand() == toRemove) {
                if (toReplace instanceof CDSL) {
                    c.setNextCommand((CDSL) toReplace);
                } else {
                    c.setNextCommand(new CDSL((iCommandDSL) toReplace));
                }

            } else if (c.getRealCommand() == toRemove) {
                if (toReplace instanceof iCommandDSL) {
                    c.setRealCommand((iCommandDSL) toReplace);
                    if (c.getNextCommand() == toReplace) {
                        c.removeRightNode();
                    }
                } else if (toReplace instanceof CDSL) {
                    iDSL grandf = c.getFather();
                    changeActualCDSLByLeftCDSL(grandf, c, toReplace);
                }

            }
        }
    }

    private static void removeInconsistentFor(iDSL dsl) {
        HashSet<iNodeDSLTree> nodes = BuilderDSLTreeSingleton.getAllNodes((iNodeDSLTree) dsl);
        for (iNodeDSLTree node : nodes) {
            if (node instanceof S3DSL) {
                if (((S3DSL) node).translate().equals("for(u) ()")) {
                    S1DSL father = (S1DSL) ((S3DSL) node).getFather();
                    if (father.getCommandS1() == node) {
                        father.setCommandS1(new EmptyDSL());
                    }
                }
            }
        }
    }

    private static iDSL removeEmptyFromDSL(iDSL dsl) {
        if (!SettingsAlphaDSL.isCLEAN_EMPTY()) {
            return dsl;
        }
        int block_lim = 1;
        int control_lim = 10;
        while (has_empty(dsl) && (block_lim < control_lim)) {
            dsl.update_relations();
            HashSet<iNodeDSLTree> nodes = BuilderDSLTreeSingleton.getInstance().getNodesWithoutDuplicity(dsl);
            List<iNodeDSLTree> ord_nodes = ordered_nodes(nodes);
            for (iNodeDSLTree node : ord_nodes) {
                if (node instanceof CDSL) {
                    clean_empty_from_CDSL((CDSL) node);
                } else if (node instanceof S1DSL) {
                    boolean restart = false;
                    iDSL old_dsl = dsl;
                    dsl = clean_empty_from_S1DSL((S1DSL) node, dsl);
                    if (dsl != old_dsl) {
                        control_lim++;
                        break;
                    }
                } else if (node instanceof S4DSL) {
                    clean_empty_from_S4DSL((S4DSL) node);
                } else if (node instanceof S2DSL) {
                    clean_empty_from_S2DSL((S2DSL) node);
                } else if (node instanceof S3DSL) {
                    clean_empty_from_S3DSL((S3DSL) node);
                }
            }
//            BuilderDSLTreeSingleton.formatedStructuredDSLTreePreOrderPrint((iNodeDSLTree) dsl);
            block_lim++;
        }

        return dsl;
    }

    private static void checkAndRemoveInconsistentIf(iDSL dsl) {
        HashSet<iNodeDSLTree> nodes = BuilderDSLTreeSingleton.getAllNodes((iNodeDSLTree) dsl);        
        for (iNodeDSLTree node : nodes) {
            if (node instanceof S2DSL) {
                S2DSL s2 = (S2DSL) node;
                if (s2.getThenCommand() != null && s2.getElseCommand() != null
                        && s2.getThenCommand().translate().equals("")
                        && s2.getElseCommand().translate().equals("")) {
                    iNodeDSLTree father = (iNodeDSLTree) s2.getFather();
                    if (father.getRightChild() == s2) {
                        father.removeRightNode();
                    } else if (father.getLeftNode() == s2) {
                        father.removeLeftNode();
                    }
                    verifyIntegrity(father);
                } else if (s2.getThenCommand() != null
                        && s2.getThenCommand().translate().equals("")
                        && s2.getElseCommand() == null) {
                    iNodeDSLTree father = (iNodeDSLTree) s2.getFather();
                    if (father.getRightChild() == s2) {
                        father.removeRightNode();
                    } else if (father.getLeftNode() == s2) {
                        father.removeLeftNode();
                    }
                    verifyIntegrity(father);
                } else if (s2.getThenCommand() != null
                        && s2.getThenCommand().translate().equals("")
                        && s2.getElseCommand() != null
                        && !(s2.getElseCommand().translate().equals(""))) {
                    s2.setThenCommand(s2.getElseCommand());
                    s2.removeLeftNode();
                }
            }
        }
    }

    private static void clean_empty_from_CDSL(CDSL cdsl) {
        //if real is empty, remove it and change position with next
        if (cdsl.getNextCommand() == null && cdsl.getRealCommand() == null) {
            cdsl.setRealCommand(new EmptyDSL());
        }
        if (cdsl.getRealCommand() == null) {
            cdsl.setRealCommand(new EmptyDSL());
        }
        if (cdsl.getRealCommand() instanceof EmptyDSL) {
            iDSL father = cdsl.getFather();
            if (father instanceof CDSL) {
                ((CDSL) father).setNextCommand(cdsl.getNextCommand());
            } else if (father instanceof S2DSL) {
                //discovery if I'm in then or else and changed
                S2DSL s2 = (S2DSL) father;
                if (s2.getThenCommand() == cdsl) {
                    s2.setThenCommand(cdsl.getNextCommand());
                } else if (s2.getElseCommand() == cdsl) {
                    s2.setElseCommand(cdsl.getNextCommand());
                }
            } else if (father instanceof S4DSL) {
                ((S4DSL) father).setFirstDSL(cdsl.getNextCommand());
            } else if (father instanceof S1DSL) {
                if (cdsl.getNextCommand() == null) {
                    S1DSL grand = (S1DSL) ((S1DSL) father).getFather();
                    if (grand == null) {
                        ((S1DSL) father).setCommandS1(new EmptyDSL());
                    } else {
                        grand.setNextCommand(((S1DSL) father).getNextCommand());
                    }
                } else {
                    ((S1DSL) father).setCommandS1(cdsl.getNextCommand());
                }

            }
        }
    }

    private static boolean has_empty(iDSL dsl) {
        HashSet<iNodeDSLTree> nodes = BuilderDSLTreeSingleton.getInstance().getNodesWithoutDuplicity(dsl);
        for (iNodeDSLTree node : nodes) {
            if (node instanceof EmptyDSL) {
                return true;
            }
            if (node instanceof S2DSL) {
                if (((S2DSL) node).getThenCommand() == null
                        && ((S2DSL) node).getElseCommand() == null) {
                    return true;
                }
                if (((S2DSL) node).getThenCommand() == null
                        && ((S2DSL) node).getElseCommand() != null) {
                    return true;
                }
            }
            if (node instanceof S3DSL) {
                if (((S3DSL) node).getForCommand() == null) {
                    return true;
                }
            }
            if (node instanceof S4DSL) {
                if (((S4DSL) node).getFirstDSL() == null
                        && ((S4DSL) node).getNextCommand() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static iDSL clean_empty_from_S1DSL(S1DSL s1, iDSL dsl) {
        //if the mandatory command is empty, changed the next S1 with it
        if (s1.getCommandS1() == null) {
            s1.setCommandS1(new EmptyDSL());
        }
        if (s1.getCommandS1() instanceof EmptyDSL) {
            if (s1.getFather() != null) {
                S1DSL father = (S1DSL) s1.getFather();
                father.setNextCommand(s1.getNextCommand());
            } else {
                S1DSL S1root = s1.getNextCommand();
                if(S1root != null){
                    S1root.setFather(null);
                    return S1root;
                }                
            }
        }

        return dsl;
    }

    private static void clean_empty_from_S4DSL(S4DSL s4) {
        //if S4 has a firstDSL empty, change the child with the father
        if (s4.getFirstDSL() == null && s4.getNextCommand() == null) {
            s4.setFirstDSL(new EmptyDSL());
        }
        if (s4.getFirstDSL() instanceof EmptyDSL) {
            if (s4.getFather() instanceof S3DSL) {
                S3DSL father = (S3DSL) s4.getFather();
                father.setForCommand(s4.getNextCommand());
            }
            if (s4.getFather() instanceof S4DSL && s4.getNextCommand() == null) {
                ((S4DSL) s4.getFather()).setNextCommand(null);
            }
            if (s4.getFather() instanceof S4DSL && s4.getNextCommand() != null) {
                ((S4DSL) s4.getFather()).setNextCommand(s4.getNextCommand());
            }
        }
        if (s4.getFirstDSL() == null && s4.getNextCommand() != null) {
            if (s4.getFather() instanceof S3DSL) {
                S3DSL father = (S3DSL) s4.getFather();
                father.setForCommand(s4.getNextCommand());
            }
        }
    }

    private static void clean_empty_from_S2DSL(S2DSL s2) {
        //remove s2 when then and else is empty
        if ((s2.getThenCommand() == null && s2.getElseCommand() == null)) {
            if (s2.getFather() instanceof S1DSL) {
                S1DSL father = (S1DSL) s2.getFather();
                father.setCommandS1(new EmptyDSL());
            } else if (s2.getFather() instanceof S4DSL) {
                ((S4DSL) s2.getFather()).setFirstDSL(new EmptyDSL());
            }
        }
        if ((s2.getThenCommand() == null) && s2.getElseCommand() != null) {
            s2.setThenCommand(s2.getElseCommand());
            s2.setElseCommand(null);
            S5DSL temp = (S5DSL) s2.getBoolCommand();
            if (temp.getNotFactor() == S5DSLEnum.NONE) {
                temp.setNotFactor(S5DSLEnum.NOT);
            } else if (temp.getNotFactor() == S5DSLEnum.NOT) {
                temp.setNotFactor(S5DSLEnum.NONE);
            }
            s2.setBoolCommand(temp);
        }
        if(s2.translate().contains("else((e))")){
            s2.setElseCommand(null);
        }
    }

    private static void clean_empty_from_S3DSL(S3DSL s3) {
        if (s3.getForCommand() == null) {
            S1DSL father = (S1DSL) s3.getFather();
            S1DSL grand = (S1DSL) father.getFather();
            if (grand != null) {
                grand.setNextCommand(father.getNextCommand());
            } else {
                father.setCommandS1(new EmptyDSL());
            }
        }
    }

    private static List<iNodeDSLTree> ordered_nodes(HashSet<iNodeDSLTree> nodes) {
        
        List<iNodeDSLTree> ret = new ArrayList<>();
        
        List<iNodeDSLTree> cdsl = new ArrayList<>();
        List<iNodeDSLTree> S2 = new ArrayList<>();
        List<iNodeDSLTree> S3 = new ArrayList<>();
        List<iNodeDSLTree> S4 = new ArrayList<>();
        List<iNodeDSLTree> other = new ArrayList<>();
        
        for (iNodeDSLTree node : nodes) {
            if(node instanceof CDSL){
                cdsl.add(node);
            }else if(node instanceof S2DSL){
                S2.add(node);
            }else if(node instanceof S3DSL){
                S3.add(node);
            } else if(node instanceof S4DSL){
                S4.add(node);
            }else{
                other.add(node);
            }
        }
        
        ret.addAll(cdsl);
        ret.addAll(S2);
        ret.addAll(S4);
        ret.addAll(S3);
        ret.addAll(other);
        
        if (ret.size() != nodes.size()) {
            System.err.println("Problem with ordered_nodes by size not match!");
            return new ArrayList<>(nodes);
        }
        
        return ret;
    }

}
