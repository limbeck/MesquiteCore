package mesquite.molec.CGBiasOfTaxon;

import mesquite.categ.lib.*;
import mesquite.lib.EmployeeNeed;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.MesquiteString;
import mesquite.lib.Notification;
import mesquite.lib.Taxa;
import mesquite.lib.Taxon;
import mesquite.lib.characters.CharInclusionSet;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CharacterState;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.duties.MatrixSourceCoord;
import mesquite.lib.duties.NumberForTaxon;

public class CGBiasOfTaxon extends NumberForTaxon {
	long A = CategoricalState.makeSet(0);
	long C = CategoricalState.makeSet(1);
	long G = CategoricalState.makeSet(2);
	long T = CategoricalState.makeSet(3);
	long AT = A | T;
	long CG =  C | G;
	long ACGT = A | C | G | T;

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(MatrixSourceCoord.class, getName() + "  needs a source of characters.",
		"The source of characters is arranged initially");
	}
	MatrixSourceCoord matrixSourceTask;
	Taxa currentTaxa = null;
	MCharactersDistribution observedStates =null;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, DNAState.class, "DNA Matrix on which to calculate compositional bias"); //TODO: allow resetting of source (e.g., to simulation)
		if (matrixSourceTask==null)
			return sorry(getName() + " couldn't start because no source of character matrices was obtained.");
		return true;
	}

	/*.................................................................................................................*/
	/** Generated by an employee who quit.  The MesquiteModule should act accordingly. */
	public void employeeQuit(MesquiteModule employee) {
		if (employee == matrixSourceTask)  // character source quit and none rehired automatically
			iQuit();
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return false;  
	}

	/** Called to provoke any necessary initialization.  This helps prevent the module's intialization queries to the user from
   	happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/
	public void initialize(Taxa taxa){
		currentTaxa = taxa;
		matrixSourceTask.initialize(currentTaxa);
	}

	public void calculateNumber(Taxon taxon, MesquiteNumber result, MesquiteString resultString){
		if (result==null)
			return;
		clearResultAndLastResult(result);
		Taxa taxa = taxon.getTaxa();
		int it = taxa.whichTaxonNumber(taxon);
		if (taxa != currentTaxa || observedStates == null ) {
			observedStates = matrixSourceTask.getCurrentMatrix(taxa);
			currentTaxa = taxa;
		}
		if (observedStates==null)
			return;
		DNAData data = (DNAData)observedStates.getParentData();
		CharInclusionSet inclusion = null;
		if (data !=null)
			inclusion = (CharInclusionSet)data.getCurrentSpecsSet(CharInclusionSet.class);
		int numChars = observedStates.getNumChars();
		int charExc = 0;
		int tot = 0;
		int count = 0;
		double value = 0.0;
		if (numChars != 0) {
			for (int ic = 0; ic<data.getNumChars(); ic++) {
				if (inclusion == null || inclusion.isBitOn(ic)){
					long s = data.getState(ic,it);
					if (!CategoricalState.isUnassigned(s) && !CategoricalState.isInapplicable(s)) {
							if (s == A || s == T || s == AT) //monomorphic A or T or A&T or uncertain A or T
								tot++;
							else if (s == C || s == G || s == CG) { //monomorphic C or G or C&G or uncertain C or G
								tot++;
								count++;
							}
						
					}
				} else
					charExc++;
			}
			if (tot == 0)
				value = MesquiteDouble.unassigned; //changed from 0,  26 jan '14
			else
				value = ((double)count)/tot;
			result.setValue(value);
		}	
		String exs = "";
		if (charExc > 0)
			exs = " (" + Integer.toString(charExc) + " characters excluded)";

		
		if (resultString!=null)
			resultString.setValue("Proportion of CG in taxon "+ observedStates.getName() + exs + ": " + result.toString());
		saveLastResult(result);
		saveLastResultString(resultString);
	}
	/*.................................................................................................................*/
	public void employeeParametersChanged(MesquiteModule employee, MesquiteModule source, Notification notification) {
		observedStates = null;
		super.employeeParametersChanged(employee, source, notification);
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Proportion CG in taxon";  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return true;
	}
	public String getParameters() {
		return "Proportion CG in taxon in matrix from: " + matrixSourceTask.getParameters();
	}
	/*.................................................................................................................*/

	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Reports the proportion of CG in a taxon for a DNA matrix." ;
	}

}


