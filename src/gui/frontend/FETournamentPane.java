/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.frontend;

import ai.core.AI;
import ai.synthesis.dslForScriptGenerator.DslAI;
import gui.JTextAreaWriter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;
import rts.units.UnitTypeTable;
import synthesizer.Synthesizer;
import tournaments.FixedOpponentsTournament;
import tournaments.LoadTournamentAIs;
import tournaments.RoundRobinTournament;


/**
 *
 * @author santi
 */
public class FETournamentPane extends JPanel {
    private static final String TOURNAMENT_ROUNDROBIN = "Round Robin";
    private static final String TOURNAMENT_FIXED_OPPONENTS = "Fixed Opponents";
    
    private JComboBox tournamentTypeComboBox;

    private DefaultListModel availableSynsListModel;
    private JList availableSynsList;
    private DefaultListModel selectedSynsListModel;
    private JList selectedSynsList;
    
    private DefaultListModel availableAIsListModel;
    private JList availableAIsList;
    private DefaultListModel selectedAIsListModel;
    private JList selectedAIsList;
    private DefaultListModel opponentAIsListModel;
    private JList opponentAIsList;
    private JButton opponentAddButton;
    private JButton opponentRemoveButton;
    
    private JFileChooser mapFileChooser = new JFileChooser();
    private JList mapList;
    private DefaultListModel mapListModel;
    
    private JFormattedTextField iterationsField;
    private JFormattedTextField maxGameLengthField;
    private JFormattedTextField timeBudgetField;
    private JFormattedTextField iterationsBudgetField;
    private JFormattedTextField preAnalysisTimeField;
    
    private JComboBox unitTypeTableBox;
    private JCheckBox fullObservabilityCheckBox;
    private JCheckBox selfMatchesCheckBox;
    private JCheckBox timeoutCheckBox;
    private JCheckBox gcCheckBox;
    private JCheckBox tracesCheckBox;
    
    private JTextArea tournamentProgressTextArea;
    
    private JFileChooser fileChooser = new JFileChooser();
    
    public FETournamentPane() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        {
            String tournamentTypes[] = {TOURNAMENT_ROUNDROBIN, TOURNAMENT_FIXED_OPPONENTS};
            tournamentTypeComboBox = new JComboBox(tournamentTypes);
            tournamentTypeComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            tournamentTypeComboBox.setAlignmentY(Component.TOP_ALIGNMENT);
            tournamentTypeComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    JComboBox combo = (JComboBox)e.getSource();
                    if (combo.getSelectedIndex()==1) {
                        opponentAIsList.setEnabled(true);
                        opponentAddButton.setEnabled(true);
                        opponentRemoveButton.setEnabled(true);
                    } else {
                        opponentAIsList.setEnabled(false);
                        opponentAddButton.setEnabled(false);
                        opponentRemoveButton.setEnabled(false);
                    }
                }
            });
            tournamentTypeComboBox.setMaximumSize(new Dimension(300,24));
            add(tournamentTypeComboBox);
        }
        
        {
            JPanel p1 = new JPanel();
            p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));

            JPanel availableAIsPanel;
            JPanel availableSynsPanel;
            JPanel selectedAIsPanel;
            JPanel selectedSynsPanel;
            JPanel opponentAIsPanel;
            
            {
                availableAIsPanel = new JPanel();
                availableAIsPanel.setLayout(new BoxLayout(availableAIsPanel, BoxLayout.Y_AXIS));
                availableAIsPanel.add(new JLabel("Available AIs"));

                availableAIsListModel = new DefaultListModel();

                for(int i = 0;i<FEStatePane.AIs.length;i++) {
                    availableAIsListModel.addElement(FEStatePane.AIs[i]);
                }                
                availableAIsList = new JList(availableAIsListModel);
                availableAIsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                availableAIsList.setLayoutOrientation(JList.VERTICAL);
                availableAIsList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(availableAIsList);
                listScroller.setPreferredSize(new Dimension(200, 200));
                availableAIsPanel.add(listScroller);
                
                JButton loadJAR = new JButton("Load Specific JAR");
                loadJAR.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        int returnVal = fileChooser.showOpenDialog((Component)null);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            try {
                                List<Class> cl = LoadTournamentAIs.loadTournamentAIsFromJAR(file.getAbsolutePath());
                                for(Class c:cl) {
                                    boolean exists = false;
                                    for(int i = 0;i<availableAIsListModel.size();i++) {
                                        Class c2 = (Class)availableAIsListModel.get(i);
                                        if (c2.getName().equals(c.getName())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) availableAIsListModel.addElement(c);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                       }
                    }
                });
                availableAIsPanel.add(loadJAR);
                
                JButton loadJARFolder = new JButton("Load All JARS from Folder");
                loadJARFolder.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int returnVal = fileChooser.showOpenDialog((Component)null);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = fileChooser.getSelectedFile();
                            try {
                                List<Class> cl = LoadTournamentAIs.loadTournamentAIsFromFolder(file.getAbsolutePath());
                                for(Class c:cl) {
                                     boolean exists = false;
                                    for(int i = 0;i<availableAIsListModel.size();i++) {
                                        Class c2 = (Class)availableAIsListModel.get(i);
                                        if (c2.getName().equals(c.getName())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) availableAIsListModel.addElement(c);
                               }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                       }
                    }
                });
                availableAIsPanel.add(loadJARFolder);
            }
            {
                availableSynsPanel = new JPanel();
                availableSynsPanel.setLayout(new BoxLayout(availableSynsPanel, BoxLayout.Y_AXIS));
                availableSynsPanel.add(new JLabel("Available Synthesizers"));

                availableSynsListModel = new DefaultListModel();

                for(int i = 0; i < FEStatePane.Synthesizers.length; i++) {
                    availableSynsListModel.addElement(FEStatePane.Synthesizers[i]);
                }                
                availableSynsList = new JList(availableSynsListModel);
                availableSynsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                availableSynsList.setLayoutOrientation(JList.VERTICAL);
                availableSynsList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(availableSynsList);
                listScroller.setPreferredSize(new Dimension(200, 200));
                availableSynsPanel.add(listScroller);
                p1.add(availableSynsPanel);
            }
            {
                selectedAIsPanel = new JPanel();
                selectedAIsPanel.setLayout(new BoxLayout(selectedAIsPanel, BoxLayout.Y_AXIS));
                selectedAIsPanel.add(new JLabel("Selected AIs"));

                selectedAIsListModel = new DefaultListModel();
                selectedAIsList = new JList(selectedAIsListModel);
                selectedAIsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                selectedAIsList.setLayoutOrientation(JList.VERTICAL);
                selectedAIsList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(selectedAIsList);
                listScroller.setPreferredSize(new Dimension(200, 200));
                selectedAIsPanel.add(listScroller);
                JButton add = new JButton("+");
                add.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selected[] = availableAIsList.getSelectedIndices();
                        for(int idx:selected) {
                            selectedAIsListModel.addElement(availableAIsList.getModel().getElementAt(idx));
                        }
                    }
                });
                selectedAIsPanel.add(add);
                JButton remove = new JButton("-");
                remove.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selectedIndex = selectedAIsList.getSelectedIndex();
                        if (selectedIndex>=0) selectedAIsListModel.remove(selectedIndex);
                    }
                });
                selectedAIsPanel.add(remove);
            }
            {
                selectedSynsPanel = new JPanel();
                selectedSynsPanel.setLayout(new BoxLayout(selectedSynsPanel, BoxLayout.Y_AXIS));
                selectedSynsPanel.add(new JLabel("Selected Synthesizers"));

                selectedSynsListModel = new DefaultListModel();
                selectedSynsList = new JList(selectedSynsListModel);
                selectedSynsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                selectedSynsList.setLayoutOrientation(JList.VERTICAL);
                selectedSynsList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(selectedSynsList);
                listScroller.setPreferredSize(new Dimension(200, 200));
                selectedSynsPanel.add(listScroller);
                JButton add = new JButton("+");
                add.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selected[] = availableSynsList.getSelectedIndices();
                        for (int idx : selected) {
                            selectedSynsListModel.addElement(availableSynsList.getModel().getElementAt(idx));
                        }
                    }
                });
                selectedSynsPanel.add(add);
                JButton remove = new JButton("-");
                remove.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selectedIndex = selectedSynsList.getSelectedIndex();
                        if (selectedIndex >= 0) selectedSynsListModel.remove(selectedIndex);
                    }
                });
                selectedSynsPanel.add(remove);
            }
            {
                opponentAIsPanel = new JPanel();
                opponentAIsPanel.setLayout(new BoxLayout(opponentAIsPanel, BoxLayout.Y_AXIS));
                opponentAIsPanel.add(new JLabel("Opponent AIs"));

                opponentAIsListModel = new DefaultListModel();
                opponentAIsList = new JList(opponentAIsListModel);
                opponentAIsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                opponentAIsList.setLayoutOrientation(JList.VERTICAL);
                opponentAIsList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(opponentAIsList);
                listScroller.setPreferredSize(new Dimension(200, 200));
                opponentAIsPanel.add(listScroller);
                opponentAddButton = new JButton("+");
                opponentAddButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selected[] = availableAIsList.getSelectedIndices();
                        for(int idx:selected) {
                            opponentAIsListModel.addElement(availableAIsList.getModel().getElementAt(idx));
                        }
                    }
                });
                opponentAIsPanel.add(opponentAddButton);
                opponentRemoveButton = new JButton("-");
                opponentRemoveButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selectedIndex = opponentAIsList.getSelectedIndex();
                        if (selectedIndex>=0) opponentAIsListModel.remove(selectedIndex);
                    }
                });
                opponentAIsPanel.add(opponentRemoveButton);
                
                opponentAIsList.setEnabled(false);
                opponentAddButton.setEnabled(false);
                opponentRemoveButton.setEnabled(false);
            }

            p1.add(availableSynsPanel);
            p1.add(selectedSynsPanel);
            p1.add(availableAIsPanel);
            p1.add(selectedAIsPanel);
            p1.add(opponentAIsPanel);
            add(p1);
        }
        add(new JSeparator(SwingConstants.HORIZONTAL));
                
        {
            JPanel p2 = new JPanel();
            p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));

            {
                JPanel p2maps = new JPanel();
                p2maps.setLayout(new BoxLayout(p2maps, BoxLayout.Y_AXIS));
                p2maps.add(new JLabel("Maps"));
                mapListModel = new DefaultListModel();
                mapList = new JList(mapListModel);
                mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                mapList.setLayoutOrientation(JList.VERTICAL);
                mapList.setVisibleRowCount(-1);
                JScrollPane listScroller = new JScrollPane(mapList);
                listScroller.setPreferredSize(new Dimension(200, 100));
                p2maps.add(listScroller);
                JButton add = new JButton("+");
                add.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int returnVal = mapFileChooser.showOpenDialog((Component)null);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = mapFileChooser.getSelectedFile();
                            try {
                                mapListModel.addElement(file.getPath());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                       }
                    }
                });
                p2maps.add(add);
                JButton remove = new JButton("-");
                remove.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        int selected = mapList.getSelectedIndex();
                        if (selected>=0) {
                            mapListModel.remove(selected);
                        }
                    }
                });
                p2maps.add(remove);
                
                p2.add(p2maps);
            }
            p2.add(new JSeparator(SwingConstants.VERTICAL));

            {
                JPanel p2left = new JPanel();
                p2left.setLayout(new BoxLayout(p2left, BoxLayout.Y_AXIS));
                
                // N, maxgame length, time budget, iterations budget
                iterationsField = FEStatePane.addTextField(p2left,"Iterations:", "10", 4);
                maxGameLengthField = FEStatePane.addTextField(p2left,"Max Game Length:", "3000", 4);
                timeBudgetField = FEStatePane.addTextField(p2left,"Time Budget:", "100", 5);
                iterationsBudgetField = FEStatePane.addTextField(p2left,"Iterations Budget:", "-1", 8);
                preAnalysisTimeField = FEStatePane.addTextField(p2left,"pre-Analisys time budget:", "1000", 8);
                p2left.setMaximumSize(new Dimension(1000,1000));    // something sufficiently big for all these options
                p2.add(p2left);            
            }            
            p2.add(new JSeparator(SwingConstants.VERTICAL));

            {
                JPanel p2right = new JPanel();
                p2right.setLayout(new BoxLayout(p2right, BoxLayout.Y_AXIS));
                
                {
                    JPanel ptmp = new JPanel();
                    ptmp.setLayout(new BoxLayout(ptmp, BoxLayout.X_AXIS));
                    ptmp.add(new JLabel("UnitTypeTable"));
                    unitTypeTableBox = new JComboBox(FEStatePane.unitTypeTableNames);
                    unitTypeTableBox.setAlignmentX(Component.CENTER_ALIGNMENT);
                    unitTypeTableBox.setAlignmentY(Component.CENTER_ALIGNMENT);
                    unitTypeTableBox.setMaximumSize(new Dimension(160,20));
                    ptmp.add(unitTypeTableBox);
                    p2right.setMaximumSize(new Dimension(1000,1000));    // something sufficiently big for all these options
                    p2right.add(ptmp);
                }                
                
                fullObservabilityCheckBox = new JCheckBox("Full Obsservability");
                fullObservabilityCheckBox.setSelected(true);
                p2right.add(fullObservabilityCheckBox);
                selfMatchesCheckBox = new JCheckBox("Include self-play matches");
                selfMatchesCheckBox.setSelected(false);
                p2right.add(selfMatchesCheckBox);
                timeoutCheckBox = new JCheckBox("Game over if AI times out");
                timeoutCheckBox.setSelected(true);
                p2right.add(timeoutCheckBox);
                gcCheckBox = new JCheckBox("Call garbage collector right before each AI call");
                gcCheckBox.setSelected(false);                
                p2right.add(gcCheckBox);
                tracesCheckBox = new JCheckBox("Save game traces");
                tracesCheckBox.setSelected(false);                
                p2right.add(tracesCheckBox);
//                preGameAnalysisCheckBox = new JCheckBox("Give time to the AIs before game starts to analyze initial game state");
//                preGameAnalysisCheckBox.setSelected(false);                
//                p2right.add(preGameAnalysisCheckBox);
                p2.add(p2right);
            }            
            add(p2);
        }
        
        JButton run = new JButton("Run Tournament");
        add(run);        
        run.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                try {
                    // get all the necessary info:
                    UnitTypeTable utt = FEStatePane.unitTypeTables[unitTypeTableBox.getSelectedIndex()];
                    String tournamentType = (String)tournamentTypeComboBox.getSelectedItem();
                    List<Synthesizer> selectedSyns = new ArrayList<>();
                    List<AI> selectedAIs = new ArrayList<>();
                    List<AI> opponentAIs = new ArrayList<>();
                    List<String> maps = new ArrayList<>();
                    for (int i = 0; i < selectedSynsListModel.getSize(); i++) {
                        Class c = (Class)selectedSynsListModel.get(i);
                        Constructor cons = c.getConstructor();
                        selectedSyns.add((Synthesizer)cons.newInstance());
                    }
                    for(int i = 0;i<selectedAIsListModel.getSize();i++) {
                        Class c = (Class)selectedAIsListModel.get(i);
                        Constructor cons = c.getConstructor(UnitTypeTable.class);
                        selectedAIs.add((AI)cons.newInstance(utt));
                    }
                    for(int i = 0;i<opponentAIsListModel.getSize();i++) {
                        Class c = (Class)opponentAIsListModel.get(i);
                        Constructor cons = c.getConstructor(UnitTypeTable.class);
                        opponentAIs.add((AI)cons.newInstance(utt));
                    }
                    for(int i = 0;i<mapListModel.getSize();i++) {
                        String mapname = (String)mapListModel.getElementAt(i);
                        maps.add(mapname);
                    }

                    for (Synthesizer syn : selectedSyns) {
                        for (String mapPath : maps) {
                            System.out.println(mapPath);
                            DslAI script = syn.generate(mapPath);
                            selectedAIs.add((AI)script);
                        }
                    }
                    
                    int iterations = Integer.parseInt(iterationsField.getText());
                    int maxGameLength = Integer.parseInt(maxGameLengthField.getText());
                    int timeBudget = Integer.parseInt(timeBudgetField.getText());
                    int iterationsBudget = Integer.parseInt(iterationsBudgetField.getText());
                    int preAnalysisBudget = Integer.parseInt(preAnalysisTimeField.getText());
                    
                    boolean fullObservability = fullObservabilityCheckBox.isSelected();
                    boolean selfMatches = selfMatchesCheckBox.isSelected();
                    boolean timeOutCheck = timeoutCheckBox.isSelected();
                    boolean gcCheck = gcCheckBox.isSelected();
                    boolean preGameAnalysis = preAnalysisBudget > 0;

                    String prefix = "tournament_";
                    int idx = 0;
//                    String sufix = ".tsv";
                    File file;
                    do {
                        idx++;
                        file = new File(prefix + idx);
                    }while(file.exists());
                    file.mkdir();
                    String tournamentfolder = file.getName();
                    final File fileToUse = new File(tournamentfolder + "/tournament.csv");
                    final String tracesFolder = (tracesCheckBox.isSelected() ? tournamentfolder + "/traces":null);
                                                            
                    if (tournamentType.equals(TOURNAMENT_ROUNDROBIN)) {
                        if (selectedSyns.size() + selectedAIs.size() < 2) {
                            tournamentProgressTextArea.append("Select at least two AIs\n");
                        } else if (maps.isEmpty()) {
                            tournamentProgressTextArea.append("Select at least one map\n");
                        } else {
                            try {
                                Runnable r = new Runnable() {
                                    public void run() {
                                        try {
                                            Writer writer = new FileWriter(fileToUse);
                                            Writer writerProgress = new JTextAreaWriter(tournamentProgressTextArea);
                                            new
                                            RoundRobinTournament(selectedAIs).runTournament(-1, maps,
                                                                               iterations, maxGameLength, timeBudget, iterationsBudget, 
                                                                               preAnalysisBudget, 1000, // 1000 is just to give 1 second to the AIs to load their read/write folder saved content
                                                                               fullObservability, selfMatches, timeOutCheck, gcCheck, preGameAnalysis, 
                                                                               utt, tracesFolder,
                                                                               writer, writerProgress,
                                                                               tournamentfolder);
                                            writer.close();
                                        } catch(Exception e2) {
                                            e2.printStackTrace();
                                        }
                                    }                                
                                };
                                (new Thread(r)).start();
                            } catch(Exception e3) {
                                e3.printStackTrace();
                            }
                        }
                    } else if (tournamentType.equals(TOURNAMENT_FIXED_OPPONENTS)) {
                        if (selectedAIs.isEmpty()) {
                            tournamentProgressTextArea.append("Select at least one AI\n");
                        } else if (opponentAIs.isEmpty()) {
                            tournamentProgressTextArea.append("Select at least one opponent AI\n");
                        } else if (maps.isEmpty()) {
                            tournamentProgressTextArea.append("Select at least one map\n");
                        } else {
                            try {
                                Runnable r = new Runnable() {
                                    public void run() {
                                        try {
                                            Writer writer = new FileWriter(fileToUse);
                                            Writer writerProgress = new JTextAreaWriter(tournamentProgressTextArea);
                                            new FixedOpponentsTournament(selectedAIs, opponentAIs).runTournament(maps,
                                                                               iterations, maxGameLength, timeBudget, iterationsBudget, 
                                                                               preAnalysisBudget, 1000, // 1000 is just to give 1 second to the AIs to load their read/write folder saved content
                                                                               fullObservability, timeOutCheck, gcCheck, preGameAnalysis, 
                                                                               utt, tracesFolder,
                                                                               writer, writerProgress,
                                                                               tournamentfolder);
                                            writer.close();
                                        } catch(Exception e2) {
                                            e2.printStackTrace();
                                        }
                                    }                                
                                };
                                (new Thread(r)).start();
                            } catch(Exception e3) {
                                e3.printStackTrace();
                            }
                        }                        
                    }
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        tournamentProgressTextArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(tournamentProgressTextArea);
        tournamentProgressTextArea.setEditable(false);
        scrollPane.setPreferredSize(new Dimension(512, 192));
        add(scrollPane);
        DefaultCaret caret = (DefaultCaret)tournamentProgressTextArea.getCaret(); //autoscroll the progress Text Area
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);        
    }
}
