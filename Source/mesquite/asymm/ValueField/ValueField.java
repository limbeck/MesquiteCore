package mesquite.asymm.ValueField;import java.applet.*;import java.awt.*;import mesquite.lib.*;import mesquite.lib.duties.*;/*========================================*//*A simple example module that displays a tree window that depends on a tree in a standard tree window*/public class ValueField extends MesquiteModule {	ValueFieldWindow window;		public Class getDutyClass(){		return ValueField.class;	}  	 /*--------------------------------------*/	/*The basic substitute for a constructor for modules  <b>(overrides method of MesquiteModule)</b>*/	public boolean startJob(String arguments, Object condition, CommandRecord commandRec, boolean hiredByName) { 		return true;  	 }	/*.................................................................................................................*/	public boolean isSubstantive(){		return false;	}  	 /*--------------------------------------*/	/*Makes the module shut down when the go-away box of the window is touched  <b>(overrides method of MesquiteModule)</b>*/ 	public void windowGoAway(MesquiteWindow whichWindow) {		whichWindow.hide();		whichWindow.dispose();		iQuit();	}	public void showField(double[][] values, String comment){		if (window == null){	 		window= new ValueFieldWindow( this);	 		setModuleWindow(window);	 		window.setVisible(true);	 		resetContainingMenuBar();			resetAllWindowsMenus();		}		window.setValues(values, comment);	}  	 /*--------------------------------------*/	/*Indicates to the name of this module for purposes of menu listings and documentation.  <b>(overrides method of MesquiteModule)</b>*/    	 public String getName() {		return "Value Field Window";   	 }  	 /*--------------------------------------*/ 	/*Returns an explanation of what the module does.  <b>(overrides method of MesquiteModule)</b>*/ 	public String getExplanation() { 		return "Displays a field of values." ;   	 }}	/*========================================*/class ValueFieldWindow extends MesquiteWindow  {	FieldPanel p;	public ValueFieldWindow (ValueField ownerModule){		super(ownerModule, true); 		setBackground(ColorDistribution.veryLightGreen);		p = new FieldPanel();		p.setBackground(ColorDistribution.veryLightGreen);				addToWindow(p);		p.setVisible(true);      		setWindowSize(500,500);		resetTitle();	}  	 /*--------------------------------------*/	/* Used to get the title for window <b>(overrides abstract method of MesquiteWindow)</b>*/	public void resetTitle(){		setTitle("Values"); 	}	public void setValues(double[][] values, String comment){		p.setValues(values);		Debugg.println("====================================\n" + comment + "\n-------------------\n" + p.getText() + "\n====================================\n\n");	}  	 /*--------------------------------------*/	/*Sets the size of the window (setSize and setBounds should not be used!!!>  <b>(overrides method of MesquiteWindow)</b>*/	public void setWindowSize(int w, int h){		super.setWindowSize(w,h);		p.setBounds(0, 0, getWidth()-5, getHeight()-5);	}  	 /*--------------------------------------*/	/* Called when the window has been resized, e.g. by user. <b>(overrides method of MesquiteWindow)</b>*/	public void windowResized(){		p.setBounds(0, 0, getWidth()-5, getHeight()-5);	}	/*.................................................................................................................*/	/** to be overridden by MesquiteWindows for a text version of their contents*/	public String getTextContents() {		if (p == null)			return "no text contents";		return p.getText();	}}class FieldPanel extends MousePanel {	double[][] values;	public void setValues(double[][] values){		this.values = values;		repaint();	}	/*...........................................................*/	 double minV(double[][] values){		MesquiteNumber d = new MesquiteNumber(MesquiteDouble.unassigned);		for (int column = 0; column< values.length; column++)			for (int i=0; i<values[column].length; i++)				d.setMeIfIAmMoreThan(values[column][i]);		return d.getDoubleValue();	}	/*...........................................................*/	 double maxV(double[][] values){		MesquiteNumber d = new MesquiteNumber(MesquiteDouble.unassigned);		for (int column = 0; column< values.length; column++)			for (int i=0; i<values[column].length; i++)				d.setMeIfIAmLessThan(values[column][i]);		return d.getDoubleValue();	}	public String getText(){		if (values == null)			return "no text";				String s = "";		for (int column = 0; column<values.length; column++){			for (int row = 0; row<values[column].length; row++){				if (row != 0)					s += "\t";				s += MesquiteDouble.toString(values[row][column]);			}			s += "\n";		}		return s;	}	public void paint(Graphics g){		if (values == null || values.length == 0 || values[0] == null)			return;		int numRows = values.length;		int numColumns = values[0].length;		int width = getBounds().width -2;		int height = getBounds().height -2;		int squareWidth = width/numColumns;		int squareHeight = height/numRows;		double min = minV(values);		double max = maxV(values);		int x = 1;		int y = 1;		for (int column = 0; column<values.length; column++){			for (int row = 0; row<values[column].length; row++){				if (MesquiteDouble.isCombinable(values[column][row])){					Color c = MesquiteColorTable.getGrayScale(values[column][row], min, max);					g.setColor(c);					g.fillRect(x, y, squareWidth, squareHeight);				}				y += squareHeight;			}			y = 1;			x+= squareWidth;		}		g.setColor(Color.black);		g.drawRect(0,0,width, height);	}}