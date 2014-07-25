package mesquite.io.ExportFusedPhylip;

import java.awt.Checkbox;

import mesquite.categ.lib.*;
import mesquite.io.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.CharactersManager;

public class ExportFusedPhylip extends InterpretPhylip {
	
	boolean badImportWarningGiven = false;

/*.................................................................................................................*/
public void setPhylipState(CharacterData data, int ic, int it, char c){
}
/*........................../*.................................................................................................................*/
public boolean canExportEver() {  
	 return true;  //
}
/*.................................................................................................................*/
public boolean canImport() {  
	 return true;
}

/*.................................................................................................................*/
public boolean canExportProject(MesquiteProject project) {  
	 return project.getNumberCharMatrices(CategoricalState.class) > 0;  //
}
/*.................................................................................................................*/
public boolean canExportData(Class dataClass) {  
	return (dataClass==ProteinState.class || dataClass==DNAState.class || dataClass==CategoricalState.class);
}
/*.................................................................................................................*
	public boolean getExportTrees(){
		return false;
	}
/*.................................................................................................................*/
public CharacterData createData(CharactersManager charTask, Taxa taxa) {  
	 return null;  //not needed as can't import
}
/*.................................................................................................................*/
public void appendPhylipStateToBuffer(CharacterData data, int ic, int it, StringBuffer outputBuffer){
	data.statesIntoStringBuffer(ic, it, outputBuffer, false);
}
boolean exportRAxMLModelFile = true;
/*.................................................................................................................*/
public boolean getExportOptions(boolean dataSelected, boolean taxaSelected){
	MesquiteInteger buttonPressed = new MesquiteInteger(1);
	PhylipExporterDialog exportDialog = new PhylipExporterDialog(this,containerOfModule(), "Export Phylip Options", buttonPressed);
	
	Checkbox excludedCharactersCheckbox = exportDialog.addCheckBox("export excluded characters", localWriteExcludedChars);
	Checkbox exportTreesCheckbox = exportDialog.addCheckBox("export trees if present", exportTrees);
	Checkbox exportRAxMLModelFileCheckBox = exportDialog.addCheckBox("save RAxML model file", exportRAxMLModelFile);

	exportDialog.completeAndShowDialog(dataSelected, taxaSelected);
		
	boolean ok = (exportDialog.query(dataSelected, taxaSelected)==0);
	
	localWriteExcludedChars = excludedCharactersCheckbox.getState();
	exportTrees = exportTreesCheckbox.getState();
	exportRAxMLModelFile = exportRAxMLModelFileCheckBox.getState();
	userSpecifiedWriteExcludedChars = true;
	taxonNameLength = exportDialog.getTaxonNamesLength();
	exportDialog.dispose();
	return ok;
}	
/*.................................................................................................................*/
public boolean exportMultipleMatrices(){
	return true;
}
/*.................................................................................................................*/
public void writeExtraFiles(Taxa taxa){
	if (exportRAxMLModelFile) {
		if (MesquiteThread.isScripting()){}
		else {
			StringBuffer sb = new StringBuffer();
			int numMatrices = getProject().getNumberCharMatricesVisible(CategoricalState.class);
			CharacterData data;
			int numCharWrite=0;
			int totalNumCharWrite=1;


			for (int im = 0; im<numMatrices; im++) {
					data = getProject().getCharacterMatrixVisible(taxa, im, CategoricalState.class);
					if (data==null) continue;
				
				 if (!writeExcludedCharacters){
					 numCharWrite =  data.numCharsCurrentlyIncluded(this.writeOnlySelectedData);
				 } else {
					 numCharWrite =  data.numberSelected(this.writeOnlySelectedData);
				 }
				 
					if (data!=null) {
						String name = StringUtil.cleanseStringOfFancyChars(data.getName());
						name = StringUtil.blanksToUnderline(name);
						if (data instanceof DNAData) 
							sb.append("DNA, ");
						else if (data instanceof ProteinData) 
						sb.append("WAG, ");
						else if (data instanceof CategoricalData) 
						sb.append("MULTI, ");
						sb.append(name+ " = " + totalNumCharWrite + "-" + (totalNumCharWrite+numCharWrite-1)+StringUtil.lineEnding());
					}
					totalNumCharWrite+= numCharWrite;
			}
			String filePath = MesquiteFile.saveFileAsDialog("Save RAxML model file");
			if (StringUtil.blank(sb.toString()) || StringUtil.blank(filePath))
				return;
			MesquiteFile.putFileContents(filePath, sb.toString(), true);
		}
	}
}
/*.................................................................................................................*/
public CharacterData findDataToExport(MesquiteFile file, String arguments) { 
	return getProject().chooseData(containerOfModule(), file, null, CategoricalState.class, "Select data to export");
}
/*.................................................................................................................*/
	 public String getName() {
	return "Phylip (fused matrices)";
	 }
/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports Phylip matrices that consist of multiple matrices." ;
	 }
	 


}
