/* Mesquite source code.  Copyright 1997-2005 W. Maddison and D. Maddison. Version 1.06, August 2005.Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.molec.ResolveStateAmbiguity;/*~~  */import java.util.*;import java.awt.*;import java.awt.image.*;import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.lib.duties.*;import mesquite.categ.lib.*;import mesquite.lib.table.*;/* ======================================================================== */public class ResolveStateAmbiguity extends DNADataAlterer {	MesquiteTable table;	CharacterData data;	/*.................................................................................................................*/	public boolean startJob(String arguments, Object condition, CommandRecord commandRec, boolean hiredByName) {		return true;	}		 	/*.................................................................................................................*/  	/** Called to alter data in those cells selected in table*/   	public boolean alterData(CharacterData data, MesquiteTable table, CommandRecord commandRec){ 			if (!(data instanceof DNAData)){				MesquiteMessage.warnProgrammer("Attempt to resolve ambiguities in non-DNA data");				return false;			}			return alterContentOfCells(data,table, commandRec);   	}	/*.................................................................................................................*/   	public void alterCell(CharacterData ddata, int ic, int it, CommandRecord commandRec){   		DNAData data = (DNAData)ddata;		long s = data.getState(ic,it);		if (!CategoricalState.isUncertain(s))			return;		long result = 0L;		int card = CategoricalState.cardinality(s);		if (card>1) {			int resolve = (int)Math.round(Math.random()*card+0.5);			int count=0;			for (int e=0; e<=data.getMaxPossibleState(); e++) {				if (CategoricalState.isElement(s, e)) {					count++;					if (count>=resolve) {						result=CategoricalState.addToSet(result,e);						break;					}				}			}			if (result!=0L)				data.setState(ic,it, result);		}	}		/*.................................................................................................................*/    	 public boolean isPrerelease() {		return false;   	 }	/*.................................................................................................................*/   	 public boolean showCitation(){   	 	return true;   	 }	/*.................................................................................................................*/    	 public String getName() {		return "Arbitrarily Resolve Ambiguities";   	 }	/*.................................................................................................................*/ 	/** returns an explanation of what the module does.*/ 	public String getExplanation() { 		return "Alters nucleotide data by arbitrarily resolving ambiguous sites to one of the possible bases." ;   	 }   	 }