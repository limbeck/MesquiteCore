/* Mesquite source code.  Copyright 1997-2008 W. Maddison and D. Maddison.Version 2.5, June 2008.Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)modified 26 July 01: protected against NullPointerException if null images in paint */package mesquite.lib;import java.awt.*;import java.awt.event.*;import java.util.*;/* ======================================================================== */public class SimplicityStrip extends MousePanel implements Commandable {	Font smallFont = new Font("SanSerif", Font.PLAIN, 10);	MesquiteWindow window;	MesquitePopup popup=null;	Image editing, power, simple;	public SimplicityStrip(MesquiteWindow window) {		super();		this.window = window;		setLayout(null);		setFont(smallFont);		setBackground(ColorTheme.getInterfaceBackground());		setCursor(Cursor.getDefaultCursor());		power = MesquiteImage.getImage(MesquiteModule.getRootImageDirectoryPath() + "power.gif");  		simple = MesquiteImage.getImage(MesquiteModule.getRootImageDirectoryPath() + "simple.gif");  		editing = MesquiteImage.getImage(MesquiteModule.getRootImageDirectoryPath() + "notesTool.gif");  	}	public void dispose(){		if (popup!=null)			remove(popup);		super.dispose();	}	/*.................................................................................................................*/	void redoMenu() {		if (popup==null)			popup = new MesquitePopup(this);		popup.removeAll();		MesquiteCheckMenuItem fullItem=new MesquiteCheckMenuItem("Full Interface", null, new MesquiteCommand("full", this), null, null);		fullItem.setState(InterfaceManager.mode == InterfaceManager.ALL);		popup.add(fullItem);		MesquiteCheckMenuItem simpleItem=new MesquiteCheckMenuItem("Simple Interface", null, new MesquiteCommand("simple", this), null, null);		simpleItem.setState(InterfaceManager.mode == InterfaceManager.SIMPLE);		popup.add(simpleItem);		MesquiteCheckMenuItem editItem=new MesquiteCheckMenuItem("Edit Simple Interface", null, new MesquiteCommand("edit", this), null, null);		editItem.setState(InterfaceManager.mode == InterfaceManager.EDITING);		popup.add(editItem);		popup.add(new MenuItem("-"));		popup.add(new MesquiteMenuItem("Instructions...", null, new MesquiteCommand("showInstructions", InterfaceManager.simplicityWindow), null));		popup.add(new MesquiteMenuItem("Show/Hide Packages...", null, new MesquiteCommand("showWindow", InterfaceManager.simplicityWindow), null));		popup.add(new MenuItem("-"));		InterfaceManager.getLoadSaveMenuItems(popup);		add(popup);	}	/*.................................................................................................................*/	public Object doCommand(String commandName, String arguments, CommandChecker checker) {		if (checker.compare(this.getClass(), "Sets interface to FULL", null, commandName, "full")) {			InterfaceManager.mode = InterfaceManager.ALL;			repaint();			InterfaceManager.reset();		}		else if (checker.compare(this.getClass(), "Sets interface to SIMPLE", null, commandName, "simple")) {			InterfaceManager.mode = InterfaceManager.SIMPLE;			repaint();			InterfaceManager.reset();		}		else if (checker.compare(this.getClass(), "Turns on interface editing", null, commandName, "edit")) {			InterfaceManager.mode = InterfaceManager.EDITING;			repaint();			InterfaceManager.reset();		}		else			return  super.doCommand(commandName, arguments, checker);		return null;	}	/*.................................................................................................................*/	public void paint (Graphics g) {		if (MesquiteWindow.checkDoomed(this))			return;		int left = 18;		//g.drawRect(0, 0, getWidth()-1, getHeight()-1);		if (InterfaceManager.mode == InterfaceManager.ALL){			g.drawImage(power, 0, 0, this);			g.drawString("Full Interface", left, 13);		}		else if (InterfaceManager.mode == InterfaceManager.SIMPLE){			g.drawImage(simple, 0, 0, this);			g.drawString("Simple Interface", left, 13);		}		else if (InterfaceManager.mode == InterfaceManager.EDITING){			g.setColor(Color.cyan);			g.fillRect(0, 0, getWidth(), getHeight());			g.setColor(Color.black);			g.drawImage(editing, 0, 0, this);			g.drawString("EDITING INTERFACE", left, 13);		}		MesquiteWindow.uncheckDoomed(this);	}	/*.................................................................................................................*/	public void mouseDown(int modifiers, int clickCount, long when, int x, int y, MesquiteTool tool) {		if (MesquiteWindow.checkDoomed(this))			return;			redoMenu();		popup.show(this, 0,20);		MesquiteWindow.uncheckDoomed(this);	}}