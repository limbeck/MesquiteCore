/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


import java.io.IOException;

import mesquite.lib.*;
import mesquite.lib.duties.*;
		addMenuItem("Show Columns for All Matrices", new MesquiteCommand("showAll", this));
	
	void makeColumn(Puppeteer puppeteer, ListModule listModule, CharacterData data){
		String commands = "newAssistant #TaxaListHasData; " +
				"tell It; getMatrixSource #mesquite.charMatrices.CharMatrixCoordIndep.CharMatrixCoordIndep; " +
				"tell It; setCharacterSource #mesquite.charMatrices.StoredMatrices.StoredMatrices; tell It; setDataSet " + getProject().getCharMatrixReferenceExternal(data) + "; " +
				"endTell; endTell; endTell;";
		puppeteer.execute(listModule,  commands, new MesquiteInteger(0), null, false);
		
	}
			if (taxa == null)
				return null;
			int numMatrices = getProject().getNumberCharMatrices(taxa);
			if (numMatrices<1)
				return null;
			Vector datas = new Vector();
			for (int i = 0; i<numMatrices; i++){
				CharacterData data = getProject().getCharacterMatrix(taxa, i);
				if (data.isUserVisible())
					datas.addElement(data);
			}
				ListModule listModule = (ListModule)getEmployer();
				Vector v = listModule.getAssistants();
				for (int k = 0; k< v.size(); k++){
					ListAssistant a = (ListAssistant)v.elementAt(k);
					if (a instanceof mesquite.molec.TaxaListHasData.TaxaListHasData){
						mesquite.molec.TaxaListHasData.TaxaListHasData tLHD = (mesquite.molec.TaxaListHasData.TaxaListHasData)a;
						CharacterData data = tLHD.getCharacterData();
						if (datas.indexOf(data)>=0)
							datas.removeElement(data);
					}
				}
				Puppeteer puppeteer = new Puppeteer(this);
				CommandRecord prevR = MesquiteThread.getCurrentCommandRecord();
				CommandRecord cRecord = new CommandRecord(true);
				MesquiteThread.setCurrentCommandRecord(cRecord);
				//at this point the vector should include only the ones not being shown.
				for (int i = 0; i<datas.size(); i++)
					makeColumn(puppeteer, listModule, (CharacterData)datas.elementAt(i));
			
				MesquiteThread.setCurrentCommandRecord(prevR);
			}