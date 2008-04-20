/* Mesquite source code.  Copyright 1997-2007 W. Maddison and D. Maddison.Version 2.01, December 2007.Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html) */package mesquite.minimal.BasicFileCoordinator;/*~~  */import java.util.*;import java.awt.*;import java.awt.event.*;import mesquite.lib.*;import mesquite.lib.characters.CharacterData;import mesquite.lib.characters.CharacterModel;import mesquite.lib.duties.*;import mesquite.categ.lib.*;import mesquite.cont.lib.*;public class ProjectWindow extends MesquiteWindow implements MesquiteListener {	MesquiteProject proj;	boolean suppressed = false;	BasicFileCoordinator bfc;	ProjectPanel panel;	/*.................................................................................................................*/	public ProjectWindow(FileCoordinator  ownerModule){		super(ownerModule, false);		bfc = (BasicFileCoordinator)ownerModule;		proj = ownerModule.getProject();		panel = new ProjectPanel(this, proj, bfc);		addToWindow(panel);		proj.addListener(this);	}	public int getDefaultTileLocation(){		return MesquiteFrame.RESOURCES;	}	/*.................................................................................................................*/	/** Gets basic snapshot for window, including size, location. */	public Snapshot getSnapshot(MesquiteFile file) { 		Snapshot temp = new Snapshot();		MesquiteFrame f = getParentFrame();		temp.addLine("setResourcesState " + f.getResourcesFullWindow() + " " + f.getResourcesClosedWhenMinimized() + " " + f.getResourcesWidth());		temp.incorporate(super.getSnapshot(file), false);		return temp;	}	/*.................................................................................................................*/	Parser parser = new Parser();	/** Respond to commands sent to the window. */	public Object doCommand(String commandName, String arguments, CommandChecker checker) {		if (checker.compare(MesquiteWindow.class, "Sets the state of the resources panel of the window", null, commandName, "setResourcesState")) {			MesquiteFrame f = getParentFrame();			String rfwt = parser.getFirstToken(arguments);			if (rfwt != null){				boolean rfw = rfwt.equalsIgnoreCase("true");				String rcwmt = parser.getNextToken();				if (rcwmt != null){					boolean rcwm = rcwmt.equalsIgnoreCase("true");					String rwt = parser.getNextToken();					int rw = MesquiteInteger.fromString(rwt);					if (MesquiteInteger.isCombinable(rw)){						f.setResourcesState(rfw, rcwm, rw);					}				}			}		}		else if (checker.compare(MesquiteWindow.class, "Explain the incorporation options", null, commandName, "explainIncorporate")) {			explainIncorporate();		}		else return  super.doCommand(commandName, arguments, checker);		return null;	}	public void explainIncorporate(){		String html = "<html><body>";		html += "<h2>Incorporating information from another file</h2>";		html += "There are several ways to incorporate information from another file.  Follow this key to decide what is appropriate for your needs:";		html += "<ol>";				html += "<li>Do you want to incorporate taxa, matrices and/or trees from another file into this project?  (If you want to incorporate trees only, see the other options below.)";		html += "<ol>";		html += "<li><img src=\"file://" + bfc.getPath()+"projectHTML/fileLink.gif" + "\">&nbsp;Do you want the information incorporated to remain in the other file?  If so, then you want to use <b>Link File</b>. ";		html +="If you do this and you want to save the files linked together, you should keep them in the same relative position on disk, so that when you later open the home file, it can find the linked file.</li><br>";		html += "<li><img src=\"file://" + bfc.getPath()+"projectHTML/fileIncludeBasic.gif" + "\">&nbsp;Do you want the information incorporated to be copied into the home file of this project?";		html += " The other file will be read and then ignored; the other file will not remain linked and will not be re-written.";			html += "<ol>";					html += "<li><img src=\"file://" + bfc.getPath()+"projectHTML/fileInclude.gif" + "\">&nbsp;&nbsp;Do you want to incorporate taxa, matrices and trees, but in such a way that in general keeps the taxa blocks and matrices separate from those currently in the project?";					html += " Then use <b>Include File</b>.</li><br>";					html += "<li><img src=\"file://" + bfc.getPath()+"projectHTML/fileMergeTM.gif" + "\">&nbsp;&nbsp;Do you want to merge the taxa blocks and matrices from the other file into the taxa blocks and matrices currently in this project?";					html += " Then use <b>Merge Taxa/Matrices</b>.  This is a special system that permits you to fuse taxa and matrices from other files into existing taxa blocks and matrices.";					html += " This is useful for instance to add new sequences from another file into a DNA sequence matrix.</li>";			html += "</ol>";		html +="</li>";		html += "</ol>";		html +="</li>";				html += "<li>Do you want to incorporate only trees from another file?";		html += "<ol>";		html += "<li>Do you want to incorporate the trees only temporarily in a calculation, and in a way to save memory?";		html += " If so then you can request as your Tree Source, in the tree window or in various calculations, one of the following two options:<ol>";		html += "<li><b>Use Trees from Separate File</b>: this reads in the trees from the file one at a time, as needed, and therefore saves memory with large tree files.</li>";		html += "<li><b>MrBayes Trees</b>: this is a special version of Use Trees from Separate File that can also read the associated .p file to recover tree scores.</li>";		html += "</ol>These do not bring the trees into the project, and therefore the trees are available only for the tree window or calculation requested.";		html +="</li><br>";		html += "<li><img src=\"file://" + bfc.getPath()+"projectHTML/fileLinkTrees.gif" + "\">&nbsp;&nbsp;Do you want the trees incorporated to remain in the other file?";		html += " Then use <b>Link Trees</b>.  ";		html +="If you do this and you want to save the files linked together, you should keep them in the same relative position on disk, so that when you later open the home file, it can find the linked file.</li><br>";		html += "<li><img src=\"file://" + bfc.getPath()+"projectHTML/fileIncludeTrees.gif" + "\">&nbsp;&nbsp;Do you want the trees to be copied into the home file of this project?";		html += " Then use <b>Include Trees</b>.  With this option you can choose whether to include all or only some of the trees.";		html += " By sampling only some of the trees, you can save memory.  The trees will be moved into the home file of this project, and will be saved there when you save the file.</li>";		html += "</ol>";				html += "</ol>";		html += "</html></body>";		bfc.alertHTML(html, "Incorporating File", "Incorporating File", 820, 700);	}/*  From ManageTrees * 		MesquiteSubmenuSpec mss = getFileCoordinator().addSubmenu(MesquiteTrunk.treesMenu, "Import File with Trees");		getFileCoordinator().addItemToSubmenu(MesquiteTrunk.treesMenu, mss, "Link Contents...", makeCommand("linkTreeFile",  this));		getFileCoordinator().addItemToSubmenu(MesquiteTrunk.treesMenu, mss, "Include Contents...", makeCommand("includeTreeFile",  this));		getFileCoordinator().addItemToSubmenu(MesquiteTrunk.treesMenu, mss, "Include Partial Contents...", makeCommand("includePartialTreeFile",  this));	*/	public void windowResized(){		if (panel != null)			panel.setBounds(0,0,getBounds().width, getBounds().height);	}	/*.................................................................................................................*/	public void resetTitle(){		if (ownerModule==null || ownerModule.getProject() == null)			setTitle("Project");		else			setTitle(ownerModule.getProject().getName());	}	public boolean showInfoTabs(){		return false;	}	public void dispose(){		panel.dispose();		super.dispose();	}	/** passes which object changed, along with optional Notification object with details (e.g., code number (type of change) and integers (e.g. which character))*/	public void changed(Object caller, Object obj, Notification notification){		if (obj == proj && !MesquiteThread.isScripting()){			panel.refresh();		}	}	public void refresh(){		if (bfc.isDoomed() || bfc.getProject().refreshSuppression>0)			return;		panel.refresh();	}	void suppress(){		suppressed = true;		proj.incrementProjectWindowSuppression();	}	void resume(){  //BECAUSE OF decPWS below, should call this only if suppress had been called previously		resume(true);	}	void resume(boolean dpws){  		suppressed = false;		if (dpws)			proj.decrementProjectWindowSuppression();		refresh();	}}class ProjectPanel extends Panel implements ClosablePanelContainer{	Vector elements = new Vector();	MesquiteProject proj;	BasicFileCoordinator bfc;	ProjectWindow w;	Image fileIm;	public ProjectPanel(ProjectWindow w, MesquiteProject proj, BasicFileCoordinator bfc){ 		super();		this.w = w;		this.bfc = bfc;		this.proj = proj;		setLayout(null);		setBackground(ColorDistribution.veryVeryVeryLightGray);		fileIm = 	MesquiteImage.getImage(bfc.getPath()+ "projectHTML" + MesquiteFile.fileSeparator + "fileSmall.gif");	}	public void requestHeightChange(ClosablePanel panel){		resetSizes(getBounds().width, getBounds().height);	}	public void dispose(){		if (true)			return;		for (int i = 0; i<elements.size(); i++){			ClosablePanel panel = ((ClosablePanel)elements.elementAt(i));			remove(panel);			panel.dispose();		}	}	public ClosablePanel getPrecedingPanel(ClosablePanel panel){		return null;	}	void addExtraPanel(ElementPanel p){		elements.addElement(p);		add(p);		resetSizes(getWidth(), getHeight());		p.setVisible(true);	}	void resetSizes(int w, int h){		if (bfc.isDoomed() ||  bfc.getProject().refreshSuppression>0)			return;		int vertical = 2;		for (int i = 0; i<elements.size(); i++){			ElementPanel panel = ((ElementPanel)elements.elementAt(i));			int requestedlHeight = panel.getRequestedHeight(w);			if(i>0)				vertical += panel.requestSpacer();			panel.setBounds(0, vertical, w, requestedlHeight);			vertical += requestedlHeight;		}	}	public void paint(Graphics g){		super.paint(g);		/*int vertical = 2;		int w = getWidth();		for (int i = 0; i<elements.size(); i++){			ElementPanel panel = ((ElementPanel)elements.elementAt(i));			int requestedlHeight = panel.getRequestedHeight(w);			if (				vertical += panel.requestSpacer();			panel.setBounds(0, vertical, w, requestedlHeight);			vertical += requestedlHeight;		}*/	}	public void setBounds(int x, int y, int w, int h){		super.setBounds(x,y,w,h);		resetSizes(w, h);	}	public void setSize(int w, int h){		super.setSize(w,h);		resetSizes(w, h);	}		int count = 0;	boolean sequenceUpToDate(){		if (bfc.isDoomed())			return true;		MesquiteProject proj = bfc.getProject();		if (proj.refreshSuppression >0)			return true;		int e = 0;		for (int i=0; i<proj.getNumberLinkedFiles(); i++){			MesquiteFile mf = proj.getFile(i);			if (e>= elements.size())				return false;			ElementPanel panel = ((ElementPanel)elements.elementAt(e));			if (panel.element != mf)				return false;			e++;		}		if (proj.taxas.size()>0){			for (int i = 0; i< proj.taxas.size(); i++){				Taxa t = (Taxa)proj.taxas.elementAt(i);				if (e>= elements.size())					return false;				ElementPanel panel = ((ElementPanel)elements.elementAt(e));				if (panel.element != t)					return false;				e++;				if (proj.getNumberCharMatrices(t)>0){					for (int k = 0; k<proj.getNumberCharMatrices(t); k++){						CharacterData data = proj.getCharacterMatrix(t, k);						if (e>= elements.size())							return false;						panel = ((ElementPanel)elements.elementAt(e));						if (panel.element != data)							return false;						e++;					}				}				if (proj.getNumberOfFileElements(TreeVector.class)>0){					for (int k = 0; k<proj.getNumberOfFileElements(TreeVector.class); k++){						TreeVector trees = (TreeVector)proj.getFileElement(TreeVector.class, k);						if (e>= elements.size())							return false;						panel = ((ElementPanel)elements.elementAt(e));						if (panel.element != trees)							return false;						e++;					}				}			}			if (bfc.getProject().getCharacterModels().getNumNotBuiltIn()>0){				if (e>= elements.size())					return false;				ElementPanel panel = ((ElementPanel)elements.elementAt(e));				if (!(panel instanceof CharModelsPanel))					return false;				((CharModelsPanel)panel).refresh();				e++;			}			if (e<elements.size())				return false;			return true;		}		else {			if (bfc.getProject().getCharacterModels().getNumNotBuiltIn()>0){				if (e>= elements.size())					return false;				ElementPanel panel = ((ElementPanel)elements.elementAt(e));				if (!(panel instanceof CharModelsPanel))					return false;				((CharModelsPanel)panel).refresh();				e++;			}			if (elements.size() != 0)				return false;		}		return true;	}	public void refresh(){		if (sequenceUpToDate())			return;		for (int i = 0; i<elements.size(); i++){			ClosablePanel panel = ((ClosablePanel)elements.elementAt(i));			remove(panel);			panel.dispose();		}		elements.removeAllElements();		ElementPanel panel = null;		int e = 0;		MesquiteProject proj = bfc.getProject();		for (int i=0; i<proj.getNumberLinkedFiles(); i++){			MesquiteFile mf = proj.getFile(i);			addExtraPanel(panel = new FilePanel(bfc, this, mf));			panel.setLocation(0,0);		}		addExtraPanel(panel = new FileIncorporatePanel(bfc, this));		panel.setLocation(0,0);		if (proj.taxas.size()>0){			for (int i = 0; i< proj.taxas.size(); i++){				Taxa t = (Taxa)proj.taxas.elementAt(i);				addExtraPanel(panel = new TaxaPanel(bfc, this, t));				panel.setLocation(0,0);				if (proj.getNumberCharMatrices(t)>0){					for (int k = 0; k<proj.getNumberCharMatrices(t); k++){						CharacterData data = proj.getCharacterMatrix(t, k);						if (data instanceof MolecularData)							addExtraPanel(panel = new MolecMPanel(bfc, this, data));						else if (data instanceof ContinuousData)							addExtraPanel(panel = new ContMPanel(bfc, this, data));						else							addExtraPanel(panel = new CategMPanel(bfc, this, data));						panel.setLocation(0,0);					}				}				if (proj.getNumberOfFileElements(TreeVector.class)>0){					for (int k = 0; k<proj.getNumberOfFileElements(TreeVector.class); k++){						TreeVector trees = (TreeVector)proj.getFileElement(TreeVector.class, k);						if (trees.getTaxa() == t){							addExtraPanel(panel = new TreesRPanel(bfc, this, trees));							panel.setLocation(0,0);						}					}				}			}		}		if (bfc.getProject().getCharacterModels().getNumNotBuiltIn()>0){			addExtraPanel(panel = new CharModelsPanel(bfc, this));			panel.setLocation(0,0);		}		resetSizes(getBounds().width, getBounds().height);	}}/*======================================================================== */class FileIncorporatePanel extends ElementPanel {	public FileIncorporatePanel(BasicFileCoordinator bfc, ClosablePanelContainer container){		super(bfc, container, "Incorporate File...");		setColors(ColorDistribution.veryVeryVeryLightGray, ColorDistribution.veryVeryVeryLightGray, ColorDistribution.veryVeryVeryLightGray, Color.black);		addCommand(false, "queryGray.gif", null, "Explanation...",  new MesquiteCommand("explainIncorporate", bfc.getModuleWindow()));		addCommand(false, "fileLink.gif", "Link\nFile", "Link File...", new MesquiteCommand("linkFile", bfc));		addCommand(false, "fileInclude.gif", "Include\nFile", "Include File...", new MesquiteCommand("includeFile", bfc));		MesquiteCommand c = new MesquiteCommand("newAssistant", bfc);		c.setDefaultArguments("#mesquite.dmanager.FuseTaxaMatrices.FuseTaxaMatrices");		addCommand(false, "fileMergeTM.gif", "Merge Taxa\n& Matrices", "Merge Taxa/Matrices...", c);		ElementManager tm = bfc.findElementManager(TreeVector.class);		addCommand(false, "fileLinkTrees.gif", "Link\nTrees", "Link Trees...", new MesquiteCommand("linkTreeFile", ((MesquiteModule)tm)));		addCommand(false, "fileIncludeTrees.gif", "Include\nTrees", "Include Trees...", new MesquiteCommand("includeTreeFileAskPartial", ((MesquiteModule)tm)));		setWholeOpen(true);	}	/*  From ManageTrees	 * 		MesquiteSubmenuSpec mss = getFileCoordinator().addSubmenu(MesquiteTrunk.treesMenu, "Import File with Trees");			getFileCoordinator().addItemToSubmenu(MesquiteTrunk.treesMenu, mss, "Link Contents...", makeCommand("linkTreeFile",  this));			getFileCoordinator().addItemToSubmenu(MesquiteTrunk.treesMenu, mss, "Include Contents...", makeCommand("includeTreeFile",  this));			getFileCoordinator().addItemToSubmenu(MesquiteTrunk.treesMenu, mss, "Include Partial Contents...", makeCommand("includePartialTreeFile",  this));		*/}/*======================================================================== */class FilePanel extends ElementPanel {	MesquiteFile mf;	public FilePanel(BasicFileCoordinator bfc, ClosablePanelContainer container, MesquiteFile mf){		super(bfc, container, mf.getFileName());		setShowTriangle(false);		this.mf = mf;		if (mf == bfc.getProject().getHomeFile())			im = 	MesquiteImage.getImage(bfc.getPath()+ "projectHTML" + MesquiteFile.fileSeparator + "fileSmall.gif");		else			im = 	MesquiteImage.getImage(bfc.getPath()+ "projectHTML" + MesquiteFile.fileSeparator + "fileLinkedSmall.gif");		setColors(ColorDistribution.veryVeryVeryLightGray, ColorDistribution.veryVeryVeryLightGray, ColorDistribution.veryVeryVeryLightGray, Color.black);	}}/*======================================================================== */class TaxaPanel extends ElementPanel {	public TaxaPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);		setColors(ColorDistribution.veryLightGray, ColorDistribution.veryVeryLightGray, Color.lightGray, Color.black);//		setColors(Color.gray, Color.darkGray, Color.white);//		setColors(ColorDistribution.veryVeryLightGray, Color.gray, Color.darkGray);		addCommand(false, "list.gif", "List &\nManage\nTaxa", "List & Manage Taxa", new MesquiteCommand("showMe", element));		addCommand(false, "chart.gif", "Chart\nTaxa", "Chart Taxa", new MesquiteCommand("chart", this));		addCommand(true, null, "-", "-", null);		addCommand(true, null, "Rename Taxa Block", "Rename Taxa Block", new MesquiteCommand("renameMe", element));		addCommand(true, null, "Delete Taxa Block", "Delete Taxa Block", new MesquiteCommand("deleteMe", element));		addCommand(true, null, "Edit Comment", "Edit Comment", new MesquiteCommand("editComment", element));		//	addCommand(true, null, "ID " + element.getID(), "ID " + element.getID(), new MesquiteCommand("id", this));	}	public int requestSpacer(){		return 16;	}	void chart(){		String mID = Long.toString(((FileElement)element).getID());		MesquiteThread.addHint(new MesquiteString("TaxonValuesChart", mID));		if (MesquiteDialog.useWizards)			MesquiteThread.triggerWizard();		bfc.showChartWizard("Taxa");		if (MesquiteDialog.useWizards)			MesquiteThread.detriggerWizard();	}	public String getNotes(){		return Integer.toString(((Taxa)element).getNumTaxa()) + " Taxa";	}}/*======================================================================== */class MElementPanel extends ElementPanel {	public MElementPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);		addCommand(false, getShowMatrixIconFileName(), "Show\nMatrix", "Show Matrix", new MesquiteCommand("showMe", element));		addCommand(false, "list.gif", "List &\nManage\nCharacters", "List & Manage Characters", new MesquiteCommand("list", this));		addCommand(false, "chart.gif", "Chart\nCharacters", "Chart Characters", new MesquiteCommand("chart", this));		addCommand(true, null, "-", "-", null);		addCommand(true, null, "Rename Matrix", "Rename Matrix", new MesquiteCommand("renameMe", element));		addCommand(true, null, "Delete Matrix", "Delete Matrix", new MesquiteCommand("deleteMe", element));		addCommand(true, null, "Edit Comment", "Edit Comment", new MesquiteCommand("editComment", element));		//addCommand(true, null, "ID " + element.getID(), "ID " + element.getID(), new MesquiteCommand("id", this));	}	public Object doCommand(String commandName, String arguments, CommandChecker checker) {		if (checker.compare(this.getClass(), "Lists the characters", null, commandName, "list")) {			((CharacterData)element).showList();		}		else			return  super.doCommand(commandName, arguments, checker);		return null;	}	public String getNotes(){		return Integer.toString(((CharacterData)element).getNumChars()) + " Characters";	}	public String getShowMatrixIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixCateg.gif";	}	void chart(){		String mID = Long.toString(((FileElement)element).getID());		String tID = Long.toString(((CharacterData)element).getTaxa().getID());		MesquiteThread.addHint(new MesquiteString("CharacterValuesChart", tID));		MesquiteThread.addHint(new MesquiteString("CharSrcCoordObed", "#StoredCharacters"));		MesquiteThread.addHint(new MesquiteString("StoredCharacters", mID));		if (MesquiteDialog.useWizards)			MesquiteThread.triggerWizard();		bfc.showChartWizard("Characters");		if (MesquiteDialog.useWizards)			MesquiteThread.detriggerWizard();	}}/*======================================================================== */class CategMPanel extends MElementPanel {	public CategMPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);	}	public String getShowMatrixIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixCateg.gif";	}	public String getIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixCategSmall.gif";	}}/*======================================================================== */class MolecMPanel extends MElementPanel {	public MolecMPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);	}	public String getShowMatrixIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixMolec.gif";	}	public String getIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixMolecSmall.gif";	}}/*======================================================================== */class ContMPanel extends MElementPanel {	public ContMPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);	}	public String getShowMatrixIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixCont.gif";	}	public String getIconFileName(){ //for small 16 pixel icon at left of main bar		return "matrixContSmall.gif";	}}/*======================================================================== */class TreesRPanel extends ElementPanel {	public TreesRPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);		addCommand(false, "treeView.gif", "View\nTrees", "View Trees", new MesquiteCommand("showTreesInWindow", element));		addCommand(false, "trees.gif", "List &\nManage\nTrees", "List & Manage Trees", new MesquiteCommand("showMe", element));		addCommand(false, "chart.gif", "Chart\nTrees", "Chart Trees", new MesquiteCommand("chart", this));		addCommand(true, null, "-", "-", null);		addCommand(true, null, "Rename Trees Block", "Rename Trees Block", new MesquiteCommand("renameMe", element));		addCommand(true, null, "Delete Trees Block", "Delete Trees Block", new MesquiteCommand("deleteMe", element));		addCommand(true, null, "Edit Comment", "Edit Comment", new MesquiteCommand("editComment", element));		//	addCommand(true, null, "ID " + element.getID(), "ID " + element.getID(), new MesquiteCommand("id", this));	}	void chart(){		String mID = Long.toString(((TreeVector)element).getID());		String tID = Long.toString(((TreeVector)element).getTaxa().getID());		MesquiteThread.addHint(new MesquiteString("TreeValuesChart", tID));		MesquiteThread.addHint(new MesquiteString("TreeValuesChart", "#StoredTrees"));		MesquiteThread.addHint(new MesquiteString("StoredTrees", mID));		if (MesquiteDialog.useWizards)			MesquiteThread.triggerWizard();		bfc.showChartWizard("Trees");		if (MesquiteDialog.useWizards)			MesquiteThread.detriggerWizard();	}	public String getIconFileName(){ //for small 16 pixel icon at left of main bar		return "treesSmall.gif";	}	public String getNotes(){		return Integer.toString(((TreeVector)element).size()) + " Trees";	}}/*======================================================================== */class CharModelsPanel extends HierarchicalElementPanel {	public CharModelsPanel(BasicFileCoordinator bfc, ClosablePanelContainer container){		super(bfc, container, "Character Models");		setColors(ColorDistribution.veryVeryLightGray, ColorDistribution.veryVeryVeryLightGray, Color.lightGray, Color.darkGray);		MesquiteProject proj = bfc.getProject();		proj.getCentralModelListener().addListener(this);		refresh();	}	//PUT COMMANDS AT TOP????	public void dispose(){		if (bfc != null && !bfc.isDoomed())			bfc.getProject().getCentralModelListener().removeListener(this);		super.dispose();	}	public String getNotes(){		return null;	}	public boolean upToDate(){		int e = 0;		MesquiteProject proj = bfc.getProject();		for (int i=0; i<proj.getNumModels(); i++){			CharacterModel m = proj.getCharacterModel(i);			if (!m.isBuiltIn()){				if (e>= subPanels.size())					return false;				ElementPanel panel = ((ElementPanel)subPanels.elementAt(e));				if (panel.element != m)					return false;				if (!StringUtil.stringsEqual(panel.element.getName(), panel.getTitle())){					panel.setTitle(panel.element.getName());				}				e++;			}		}		if (e<subPanels.size())			return false;		return true;	}	public void refresh(){		if (upToDate()) {			resetSizes(getWidth(), getHeight());			repaintAllPanels();			return;		}		for (int i = 0; i<subPanels.size(); i++){			ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));			remove(panel);			panel.dispose();		}		subPanels.removeAllElements();		MesquiteProject proj = bfc.getProject();		for (int i=0; i<proj.getNumModels(); i++){			CharacterModel m = proj.getCharacterModel(i);			if (!m.isBuiltIn()){				CharModelPanel panel = null;				addExtraPanel(panel = new CharModelPanel(bfc, this, m), false);				panel.setBounds(0, 0, 0, 0);			}		}	}	public int requestSpacer(){		return 16;	}	public ClosablePanel getPrecedingPanel(ClosablePanel panel){		return null;	}	public void changed(Object caller, Object obj, Notification notification){		refresh();	}}/*======================================================================== */class HierarchicalElementPanel extends ElementPanel implements ClosablePanelContainer {	Image im;	Vector subPanels = new Vector();	public HierarchicalElementPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, String title){		super(bfc, container, title);		setColors(ColorDistribution.veryVeryLightGray, ColorDistribution.veryVeryVeryLightGray, Color.lightGray, Color.darkGray);		refresh();	}	//PUT COMMANDS AT TOP????	public void dispose(){		for (int i = 0; i<subPanels.size(); i++){			ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));			panel.dispose();		}		super.dispose();	}	void addExtraPanel(ElementPanel p, boolean doReset){		subPanels.addElement(p);		add(p);		if (doReset) {			resetSizes(getWidth(), getHeight());			p.setVisible(true);		}	}	public String getNotes(){		return null;	}	public void repaintAllPanels(){		for (int i = 0; i<subPanels.size(); i++){			ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));			panel.repaint();		}	}	public boolean upToDate(){		return false;	}	public void refresh(){	}	public void requestHeightChange(ClosablePanel panel){		resetSizes(getWidth(), getHeight());		container.requestHeightChange(this);	}	void resetSizes(int w, int h){		if (bfc.isDoomed() || bfc.getProject().refreshSuppression>0)			return;		if (!open){			for (int i = 0; i<subPanels.size(); i++){				ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));				panel.setBounds(0, 0, 0, 0);				panel.setVisible(false);			}			return;		}		int vertical = MINHEIGHT+2;		for (int i = 0; i<subPanels.size(); i++){			ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));			int requestedlHeight = panel.getRequestedHeight(w);			panel.setVisible(false);			panel.setBounds(20, vertical, w, requestedlHeight);			panel.setVisible(true);			vertical += requestedlHeight;		}	}	boolean sizesMatch(int w, int h){		if (!open){			for (int i = 0; i<subPanels.size(); i++){				ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));				if (panel.getWidth() != 0 || panel.getHeight() != 0)					return false;			}			return true;		}		for (int i = 0; i<subPanels.size(); i++){			ClosablePanel panel = ((ClosablePanel)subPanels.elementAt(i));			int requestedlHeight = panel.getRequestedHeight(w);			if (panel.getWidth() != w || panel.getHeight() != requestedlHeight)				return false;		}		return true;	}	public void paint(Graphics g){		if (!sizesMatch(getWidth(), getHeight())){			resetSizes(getWidth(), getHeight());			repaintAllPanels();		}		super.paint(g);	}	public ClosablePanel getPrecedingPanel(ClosablePanel panel){		return null;	}	public int getRequestedHeight(int width){		if (!open)			return MINHEIGHT;		int total = MINHEIGHT;		for (int i=0; i<subPanels.size(); i++){			ClosablePanel mp = (ClosablePanel)subPanels.elementAt(i);			total += mp.getRequestedHeight(width-20);		}		return total;	}	public void setBounds(int x, int y, int w, int h){		super.setBounds(x,y,w,h);		resetSizes(w, h);	}	public void setSize(int w, int h){		super.setSize(w,h);		resetSizes(w, h);	}	}/*======================================================================== */class CharModelPanel extends ElementPanel {	public CharModelPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(bfc, container, element);		addCommand(true, null, "Edit Model", "Edit Model", new MesquiteCommand("editMe", element));		addCommand(true, null, "Rename Model", "Rename Model", new MesquiteCommand("renameMe", element));		addCommand(true, null, "Delete Model", "Delete Model", new MesquiteCommand("deleteMe", element));		addCommand(true, null, "Edit Comment", "Edit Comment", new MesquiteCommand("editComment", element));		//	addCommand(true, null, "ID " + element.getID(), "ID " + element.getID(), new MesquiteCommand("id", this));	}	public String getIconFileName(){ //for small 16 pixel icon at left of main bar		return "charModelSmall.gif";	}	public String getNotes(){		return null; //applicability	}}/*======================================================================== */class ElementPanel extends ClosablePanel implements MesquiteListener {	Listable element;	Vector commands = new Vector();	MesquitePopup popup=null;	BasicFileCoordinator bfc;	StringInABox notes, commandBox;	int notesWidth = 560;	int notesLeft = 10;	int commandBoxWidth = 64;	Image im;	public ElementPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, FileElement element){		super(container, element.getName());		this.element = element;		setColors(Color.white, ColorDistribution.veryVeryVeryLightGray, ColorDistribution.veryVeryLightGray, Color.darkGray);		if (getIconFileName() != null)			im = 	MesquiteImage.getImage(bfc.getPath()+ "projectHTML" + MesquiteFile.fileSeparator + getIconFileName());		this.bfc = bfc;		currentHeight = 84;		if (element !=null)			element.addListener(this);		setTightness(2);		notes =  new StringInABox(getNotes(), new Font("SansSerif", Font.PLAIN, 10), notesWidth);		commandBox =  new StringInABox(getNotes(), new Font("SansSerif", Font.PLAIN, 10), commandBoxWidth);	}	public ElementPanel(BasicFileCoordinator bfc, ClosablePanelContainer container, String name){		super(container, name);		this.bfc = bfc;		setColors(Color.white, ColorDistribution.veryVeryVeryLightGray, ColorDistribution.veryVeryLightGray, Color.darkGray);//		setColors(ColorDistribution.veryVeryLightGray, ColorDistribution.veryLightGray, Color.darkGray);		currentHeight = 84;		setTightness(2);		notes =  new StringInABox(getNotes(), new Font("SansSerif", Font.PLAIN, 10), notesWidth);		commandBox =  new StringInABox(getNotes(), new Font("SansSerif", Font.PLAIN, 10), commandBoxWidth);	}	public String getIconFileName(){ //for small 16 pixel icon at left of main bar		return null;	}	public Image getIcon(){ //for small 16 pixel icon at left of main bar		return im;	}	public int requestSpacer(){		return 0;	}	protected int getFontSize(){		return 10;	}	public void paint(Graphics g){		g.setColor(Color.black);		notes.draw(g,notesLeft, MINHEIGHT-4);		g.setColor(ColorDistribution.veryVeryLightGray);		g.fillRect(0, getHeight()-3, getWidth(), 3);		g.setColor(Color.black);		if (commands.size()>0){			int left = notesLeft;			for (int i=0; i<commands.size(); i++){				ElementCommand ec = (ElementCommand)commands.elementAt(i);				if (!ec.menuOnly){					ec.left=left;					if (ec.icon !=null){						g.drawImage(ec.icon, left, MINHEIGHT+16, this);						left += ec.icon.getWidth(this) + 2;					}					if (ec.label != null){						commandBox.setString(ec.label);						g.setColor(Color.blue);						commandBox.draw(g,left, MINHEIGHT+12);												g.setColor(Color.black);						left += commandBox.getMaxWidth() + 2;					}					ec.right=left;					left += 12;				}			}		}		super.paint(g);	}		public String getNotes(){		return "";	}	public void setBounds(int x, int y, int w, int h){		super.setBounds(x,y,w,h);	}	public void setSize(int w, int h){		super.setSize(w,h);	}		protected void addCommand(boolean menuOnly, String iconFileName, String label, String shortLabel, MesquiteCommand command){		ElementCommand ec = new ElementCommand(menuOnly, iconFileName, label, shortLabel, command);		if (iconFileName != null)			ec.icon = MesquiteImage.getImage(bfc.getPath() + "projectHTML" + MesquiteFile.fileSeparator + iconFileName);		commands.addElement(ec);	}	/*.................................................................................................................*/	void chart(){  //to be overridden to respond to command to chart the element	}	/*.................................................................................................................*/	public Object doCommand(String commandName, String arguments, CommandChecker checker) {		if (checker.compare(this.getClass(), "Charts the file element", null, commandName, "chart")) {			chart();		}		else			return  super.doCommand(commandName, arguments, checker);		return null;	}	public void changed(Object caller, Object obj, Notification notification){		if (obj == element && element != null) {			setTitle(element.getName());			notes.setString(getNotes());			repaint();		}	}	public void disposing(Object obj){	}	public boolean okToDispose(Object obj, int queryUser){		return true;	}	public void mouseDown (int modifiers, int clickCount, long when, int x, int y, MesquiteTool tool) {		//if modifiers include right click/control, then do dropdown menu		if (MesquiteEvent.rightClick(modifiers)) {			redoMenu();			popup.show(this, x,y);			return;		}		else if (y<= MINHEIGHT) {			super.mouseDown(modifiers,  clickCount,  when,  x,  y,  tool);			return;		}		else {			for (int i=0; i<commands.size(); i++){				ElementCommand ec = (ElementCommand)commands.elementAt(i);				if (!ec.menuOnly){					if (x>=ec.left && x<ec.right){						ec.command.doItMainThread(null, null, this);						return;					}				}			}		}	}	/*.................................................................................................................*/	void redoMenu() {		if (popup==null)			popup = new MesquitePopup(this);		popup.removeAll();		for (int i=0; i<commands.size(); i++) {			ElementCommand m = (ElementCommand)commands.elementAt(i);			MesquiteMenuItem mItem = new MesquiteMenuItem(m.shortLabel, bfc, m.command);			popup.add(mItem);		}		add(popup);	}	public void mouseUp(int modifiers, int x, int y, MesquiteTool tool) {		if (!MesquiteEvent.rightClick(modifiers) && y<= MINHEIGHT) {			super.mouseUp(modifiers,  x,  y,  tool);			return;		}	}	public void dispose(){		if (popup!=null)			remove(popup);		if (element != null && element instanceof FileElement)			((FileElement)element).removeListener(this);	}}class ElementCommand {	boolean menuOnly;	String iconFileName;	String label;	String shortLabel;	MesquiteCommand command;	Image icon=null;	int left = -1;	int right = -1;	public ElementCommand(boolean menuOnly, String iconFileName, String label, String shortLabel, MesquiteCommand command){		this.menuOnly =menuOnly;		this.iconFileName =iconFileName;		this.label =label;		this.shortLabel =shortLabel;		this.command =command;	}}