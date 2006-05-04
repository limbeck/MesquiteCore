package mesquite.correl.lib;

import java.util.Random;

//import org.jacorb.ir.gui.typesystem.remote.IRAlias;

import mesquite.categ.lib.CategoricalDistribution;
import mesquite.categ.lib.CategoricalState;
import mesquite.cont.lib.EigenAnalysis;
import mesquite.correl.Pagel94.Pagel94;
import mesquite.lib.*;
import mesquite.lib.characters.CLikelihoodCalculator;
import mesquite.lib.characters.CharacterDistribution;
import mesquite.lib.characters.CharacterModel;
import mesquite.lib.characters.CharacterState;
import mesquite.lib.characters.ProbabilityModel;
import mesquite.stochchar.lib.MkModel;
import Jama.Matrix;

public class PagelMatrixModel extends MultipleProbCategCharModel implements Evaluator {

	//protected long allStates = 0L;
	private int assumedMaxState=1;
	

	// different model 'behaviors' here, control how parameters are constrained
	// if qConstrained[i] is true, params[i] is set to params[qBound[i]] unless i=-1
	// otherwise qConstrained[i] is set to paramConstant - if this is MesquiteDouble.unassigned then error
	int [] qMapping;          // mapping from known, underparameterized models; domain is set of parameters, range is qList
	double[] qList;           // 
	boolean[] qConstrained;  // true if a q value has been constrained by user (currently unimplemented)
	int [] qBound;           // qvalue bound to another qvalue (-1 = not bound) (currently unimplemented)
	double [] qConstant;     // qvalue is set to this constant; (currently unimplemented)
	
	// These constants define model 'behaviors' 
	public static final int NOMODEL = 0;       // might be useful for flagging when a simpler model isn't available
	public static final int MODEL2PARAM = 1;
	public static final int MODEL4PARAM = 10;  //Pagel's model with two independent characters
    public static final int MODEL8PARAM = 20;  //Pagel's model with two dependent characters (correlated evolution)
    public static final int INDEPENDENTCHARACTERSMODEL = MODEL4PARAM;
    public static final int DEPENDENTCHARACTERSMODEL = MODEL8PARAM;
    public static final int MODEL7PARAMCONTINGENTCHANGEYFORWARD = 13;
    public static final int MODEL7PARAMCONTINGENTCHANGEYBACKWARD = 14;
    public static final int MODEL7PARAMCONTINGENTCHANGEXFORWARD = 15;
    public static final int MODEL7PARAMCONTINGENTCHANGEXBACKWARD = 16;
    public static final int MODEL6PARAMCONTINGENTCHANGEY = 17;
    public static final int MODEL6PARAMCONTINGENTCHANGEX = 18;
    public static final int MODEL2CHARACTERUSERDEFINED = 19;
    public static final int MODEL3CHARACTERINDEPENDENT = 100;
    public static final int MODEL4CHARACTERINDEPENDENT = 1000;

	
    private int modelType; //behavior of current model
    private double [] params;
    private boolean[] paramUnassigned;
    private double[][] probMatrix, rateMatrix, eigenVectors, inverseEigenVectors;
    private double[] eigenValues, imagEigenValues;
    //private MesquiteInteger pos = new MesquiteInteger(0);
    private boolean recalcProbsNeeded = true;
    private boolean needToPrepareMatrices = true;
    private Tree workingTree;
    private CategoricalDistribution observedStates1;
    private CategoricalDistribution observedStates2;

	double[][] downProbs;  // probability at node N given state s at base
	double[][][] downProbs2; // probabilty at node N given state s1 for char1 and s2 for char2 at base
	double[][] savedRootEstimates; // holds the estimates at the root (for simulation priors, etc.)
	double[] underflowCompDown;  // ln of compensation for underflow
	//double[] empirical;       // may be implemented sometime
	//double[][] empirical2;
	double[] parametersFromSimplerModel = null;  //holds values from 4 parameter model as starting values for 8 p. model
	int simplerModelType;
    	private int numStates;
	long underflowCheckFrequency = 2; //how often to check that not about to underflow; 1 checks every time
	long underflowCheck = 1;
	MesquiteNumber minChecker;

	private MesquiteInteger eightParameterExtraSearch = new MesquiteInteger(10);
	
    private double previousBranchLength = MesquiteDouble.unassigned;
    private boolean unassigned = true;
 	public static final int SIMPLER_AND_FLIP_FLOP = 2;  //conservative and slow
 	public static final int BASE_ON_SIMPLER= 0; 
 	public static final int FLIP_FLOP = 1;
 	
 	public static final double FLIP_FLOP_LOW = 0.01;
 	public static final double FLIP_FLOP_HIGH = 10.0;

 	int optimizationMode = BASE_ON_SIMPLER;  
 	
 	// Need to check this...
	static final int SUM = 0;  // likelihood is just sum of likelihoods of each state at root (i.e., flat prior)
	//static final int FIX = 1;
	static final int SUBROOT_PRIOR = 1;  //likelihood has stated prior; this is experimental and not yet available to the user interface
	static final int SUBROOT_EMPIRICAL = 2; //likelihood uses as prior the empirical frequencies of states in terminal taxa; this is experimental and not yet available to the user interface
	int rootMode = SUM; //what if anything is done with prior probabilities of states at subroot?  

	Random rng = new Random(System.currentTimeMillis());
	
	/**
	 * This returns the number of characters implied by the behavior code.  It's static so it
	 * can be used as an argument to the superconstructor.
	 * @param behavior specifies what kind of model this will be
	 * @return int specifies number of characters for the indicated behavior
	 */
	static int charsFromBehavior(int behavior){
		return 2;  // add cases when appropriate
	}

	/**
	 * Constructs a new model. 
	 * @param behavior The behavior (number of parameters, constraints, etc.) the model will implement
	 */
    public PagelMatrixModel(String name, Class dataClass, int behavior) {
    		super(name,dataClass,charsFromBehavior(behavior)); 
    	 	this.modelType = behavior;
        this.params=null;              // model's parameters
    	    qConstrained=null;         // true if a parameter is somehow constrained
    	    qBound=null;               // parameter bound to another parameter (-1 = not bound)
    	    qConstant=null;            // parameter is set to this value; 
    	    this.setConstraints(modelType);
        needToPrepareMatrices = true;
        recalcProbsNeeded = true;
        minChecker = new MesquiteNumber(MesquiteDouble.unassigned);        
        
        //prepareMatrices();
        if (modelType == MODEL4PARAM)
        	optimizationMode = FLIP_FLOP;
        else
        	optimizationMode = BASE_ON_SIMPLER;
    }
    
    
    /**
     * 
     * @return true if any parameters are free (not bound or assigned)
     */
    boolean anyUnassigned(){
    		boolean result = false;
    		for(int i=0;i<qConstrained.length;i++)
    			if(qConstrained[i])
    				result = true;
    		return result;
    }
    
    boolean allUnassigned(){
    		boolean result = true;
    		for(int i=0;i<qConstrained.length;i++)
    			if(qConstrained[i])
    				result = true;
    		return result;
    }
    
    /**
     * Extension of method from AsymmModel
     * @param like likelihood estimate from p??
     * @param p array of estimated parameters
     * @return false if any argument is NaN
     */
    private boolean acceptableResults(double like, double p[]){
    		if (Double.isNaN(like))
    			return false;
    		else
    			for(int i = 0;i<p.length;i++)
    				if (Double.isNaN(p[i]))
    					return false;
    		return true;
	}
    
    private void reportUnacceptableValues(double like, double p[],double start[]){
    		//Debugg.println("Likelihood is " + like);
    		//for (int i=0;i<p.length;i++)
    		//	Debugg.print("p [" + i + "] = " + p[i] + "; ");
    		//if (start != null)
    		//	Debugg.println("\n Started from " + DoubleArray.toString(start));
    }
    
    private void checkQArrays(int model){
    		if (model < MODEL3CHARACTERINDEPENDENT){
    			if (qMapping == null || qMapping.length != 8) {
    				qMapping = new int[8];
    				for (int i=0;i<8;i++)
    					qMapping[i] = i;   //default to identity mapping
    			}
   			if (qConstrained == null || qConstrained.length != 8){
    				qConstrained = new boolean[8];
    				for (int i=0;i<8;i++)
    					qConstrained[i]=false;
   			}
    			if (qBound == null || qBound.length != 8)
    				qBound = new int[8];
    			if (qConstant == null || qConstant.length != 8)
    				qConstant = new double[8];
    		}
   }
    
    private void setConstraints(int model){
    		checkQArrays(model);
    		switch (model) {
    	     	/* Initialization specific to the 4 parameter model.
    	     	 * q12 = q34 = params[0] (forward char0)
    	     	 * q13 = q24 = params[1] (forward char1)
    	     	 * q21 = q43 = params[2] (backward char0)
    	     	 * q31 = q42 = params[3] (backward char1)
    	     	 */ 
    			case MODEL4PARAM: {  //override defaults
    				qMapping[4]= 1;
    				qMapping[5]= 0;
    				qMapping[6]= 3;
    				qMapping[7]= 2;
    				break;
    			}
    			/*
    		     * Initialization specific to the 8 parameter model. 
    		     * q12 = params[0] (forward char0 | char1=0)
    		     * q13 = params[1] (forward char1 | char0=0)
    		     * q21 = params[2] (backward char0 | char1=0)
    		     * q31 = params[3] (backward char1 | char0=1)
    		     * q24 = params[4] (forward char1 | char0=1)
    		     * q34 = params[5] (forward char0 | char1=1)
    		     * q42 = params[6] (backward char1 | char0=1)
    		     * q43 = params[7] (backward char0 | char1=1)
    		     * 
    		     * The '|' indicate conditional probabilities, not arithmetic-or!
    		     * 
    		     * char1/char0 seem to be swapped?
    		     */
    			case MODEL8PARAM:{  //Pagel's model with two dependent characters (correlated evolution)
    				// most general 2char/2state model - nothing to do here
    				break;
     		}
    			case MODEL7PARAMCONTINGENTCHANGEYFORWARD: {
    				// seven parameters, q34=q12, 
    				qMapping[5]= 0;
    				qMapping[6]= 5;  //shift down to close the gap
    				qMapping[7]= 6;  
    				break;
    			}
    			case MODEL7PARAMCONTINGENTCHANGEYBACKWARD: {
    				// seven parameters q43=q21
    				qMapping[7]=2;
    				break;
    			}
    			case MODEL7PARAMCONTINGENTCHANGEXFORWARD:{
    				// seven parameters q24=q13
            		qMapping[4]=1;
            		qMapping[5]=4; // shift down to close the gap
            		qMapping[6]=5;
            		qMapping[7]=6;
        			break;
    			}
    			case MODEL7PARAMCONTINGENTCHANGEXBACKWARD:{
    				// seven parameters q42=q31
                	qMapping[6]= 3;
                	qMapping[7]= 6;
            		break;
    			}
    			case MODEL6PARAMCONTINGENTCHANGEY:{
    				// six parameters q34=q12,q43=q21
    				qMapping[5]= 0;
    				qMapping[6]= 5;
            		qMapping[7]= 2;
    				break;
    			}
    			case MODEL6PARAMCONTINGENTCHANGEX:{
    				// six parameters q24=q13,q42=q31
                	qMapping[4]= 1;
                	qMapping[5]= 4;
                 qMapping[6]= 3;
                 qMapping[7]= 5;
    				break;
    			}
    			case MODEL2CHARACTERUSERDEFINED: {
    				//TODO this for user specified models when we support them
    				break;
    			}
   		}
    }
    
    /*
     * Qlist is an internal mapping from models with fewer parameters to the rate matrix
     * for the full model.  This mapping means the optimizer only sees exactly the number of
     * parameters in the model.
     */
    private double[] prepareQList(double[] values){
    		if (qList == null)
    			qList = new double[qMapping.length];
		for (int i=0;i<qMapping.length;i++)
			qList[i] = params[qMapping[i]];
		return qList;
    }
    
    /*
     * Initializes the appropriate rate matrix then sets up the eigenvalues and eigenvectors 
     */
    private void prepareMatrices(){
        needToPrepareMatrices = false;
        boolean changed = false;
        unassigned = false;
        if (modelType < MODEL3CHARACTERINDEPENDENT){
			if (rateMatrix == null || rateMatrix.length !=4) {
				rateMatrix = new double[4][4];
				changed = true;
			}
			changed |= fill4x4MatricesFromQList(prepareQList(params));
        }
        if (changed) {
            recalcProbsNeeded = true;
            if (unassigned) {
                if (eigenVectors == null)
                    eigenVectors = new double[assumedMaxState+1][assumedMaxState+1];
                if (eigenValues == null)
                    eigenValues = new double[assumedMaxState+1];
                DoubleArray.deassignArray(eigenValues);
                Double2DArray.deassignArray(eigenVectors);
                //Debugg.println("Eigenvalues is " + eigenValues + ", length is " + eigenValues.length);
            }
            else  {
                EigenAnalysis e = new EigenAnalysis(rateMatrix, true, false, true);
                eigenValues = e.getEigenvalues();
                imagEigenValues = e.getImagEigenValues();
                eigenVectors = e.getEigenvectors();
                Matrix m = new Matrix(eigenVectors);
                Matrix r = m.inverse();
                inverseEigenVectors = r.getArrayCopy();
            }
        }
    }
        
    /* Initialization for 4x4 (2 character) models
     * 
     * Note: these matrices are indexed [column][row].  This is reflected
     * here, in the extra transpose done in the transition probabilities, and
     * is consistent with the comments in the Asym 2 parameter model.
     */
    private boolean fill4x4MatricesFromQList(double [] lParams){
		boolean changed = false;
		for (int i=0; i< 4; i++)
			for (int j=0; j< 4; j++){
				double r = MesquiteDouble.unassigned;
				if ((i+j)==3)   // prevents simultaneous transitions
					r=0.0;
				else {
					switch (i){
						case 0: {
							switch (j){
								case 0:{
									r = -(lParams[0]+lParams[1]);
									break;
								}
								case 1:{
									r = lParams[0]; 
									break;
								}
								case 2:{
									r = lParams[1]; 
									break;
								}
								case 3:
							}
							break;
						}
						case 1: {
							switch (j){
								case 0: {
									r = lParams[2]; 
									break;
								}
								case 1:{
									r = -(lParams[2]+lParams[4]);
									break;
								}
								case 2: break;
								case 3:{
									r = lParams[4];
								}
							}
							break;
						}
						case 2: {
							switch(j){
								case 0:{
									r=lParams[3]; 
									break;
								}
								case 1:break;
								case 2:{
									r= -(lParams[3]+lParams[5]);
									break;
								}
								case 3:{
									r=lParams[5];  
									break;
								}
							}
							break;
						}
						case 3: {
							switch(j){
								case 0: break;
								case 1:{
									r=lParams[6];  
									break;
								}
								case 2:{
									r=lParams[7];
									break;
								}
								case 3:{
									r= -(lParams[6]+lParams[7]);
									break;
								}
							}
							break;
						}
					} //end of switch
					if (rateMatrix[i][j]!= r) {
						rateMatrix[i][j] = r;
						changed = true;
						if (!MesquiteDouble.isCombinable(r))
							unassigned = true;
					}
				} // else
			}//j
	   		// Debugg.println("Checking rate matrix: " +Double2DArray.toStringRC(rateMatrix));
		return changed;
    }
    
    
    /*
     * 
     */
	public boolean inStates(int state,int c) { 
		if (state>maxState[c] || state<0)
			return false;
		else
			return (CategoricalState.isElement(allStates[c], state));
	}


	/*
	 * recodes a pair of characters into a single state for the transition matrix index
	 */
	private int recodeStatePair(int i, int j) {		//todo extend beyond binary characters?
		return 3-(2*i+j); 
	}
	
	private int[] statePairFromCode(int code){
		int[] result = new int[2];
		switch (code){
			case 0:{
				result[0]=1;
				result[1]=1;
				break;
			}
			case 1:{
				result[0]=1;
				result[1]=0;
				break;
			}
			case 2:{
				result[0]=0;
				result[1]=1;
				break;
			}
			case 3:
			default:{
				result[0]=0;
				result[1]=0;
				break;
			}
		}
		return result;
	}
	
	public double transitionProbability (int beginState[], int endState[], Tree tree, int node){
		if ((beginState.length != numChars) || (endState.length != numChars)){
			Debugg.printStackTrace("Array for beginState or endState don't match models stated number of characters");
			return 0;
		}
		boolean inStateFlag = true;
		for (int i=0;i<numChars;i++){
			inStateFlag &= inStates(beginState[i],i);
			inStateFlag &= inStates(endState[i],i);
		}
		if (!inStateFlag){
			Debugg.printStackTrace("An character state of either beginState or endState is not valid");
			return 0;
		}
		if (tree== null){
			Debugg.printStackTrace("No tree specified");
			return 0;
		}
		if (needToPrepareMatrices)
			prepareMatrices();
		double branchLength = tree.getBranchLength(node,1.0);
		if (recalcProbsNeeded || previousBranchLength != branchLength)
			recalcProbabilities(branchLength);
		if (probMatrix == null)
			return 0;
		// the following only works for two binary characters -- needs to be extended
		return probMatrix[recodeStatePair(beginState[0],beginState[1])][recodeStatePair(endState[0],endState[1])];
	}
	
	/**
	 * This is the version for two characters
	 * @param beginState1 specifies the beginning state of the first character
	 * @param endState1 specifies the ending state of the first character
	 * @param beginState2 specifies the beginning state of the second character
	 * @param endState2 specifies the ending state of the second character
	 * @param tree the tree assumed for this test
	 * @param node where we are in the tree
	 * @return double indicating transition probability from begin1,begin2 => end1,end2
	 */
	public double transitionProbability (int beginState1,int endState1,int beginState2,int endState2,Tree tree, int node){
		if (!inStates(beginState1,0) || !inStates(endState1,0) || !inStates(beginState2,1) || !inStates(endState2,1)){
			Debugg.println("Bailed out of transitionProbability " + beginState1 + " " + endState1 + " " + beginState2 + " " + endState2);
			return 0;
		}
		else {
			if (tree==null)
				return 0;
			if (needToPrepareMatrices)
				prepareMatrices();
			double branchLength = tree.getBranchLength(node,1.0);
			if (recalcProbsNeeded || previousBranchLength != branchLength)
				recalcProbabilities(branchLength);
			if (probMatrix ==null)
				return 0;
			return probMatrix[recodeStatePair(beginState1,beginState2)][recodeStatePair(endState1,endState2)];
		}
	}
	
	
	/**
	 * Updates the transition probability matrix for a branch with the specified length
	 * @param branchLength the length of the branch
	 */
	public void recalcProbabilities(double branchLength){
		if (unassigned)
			return;
		previousBranchLength = branchLength;
		if (eigenValues == null)
			return;
		//Debugg.println("Sanity check: eigenValues are " + DoubleArray.toString(eigenValues) + "bl is " + branchLength);
		int evl = eigenValues.length;
		double[][] tent  =  new double[evl][evl];
		for (int i=0; i< evl; i++)
			for (int j=0; j< evl; j++){
				if (i== j)
					tent[i][j] = Math.exp(eigenValues[i]*branchLength);
				else
					tent[i][j] = 0;
			}
			//Debugg.println("Sanity check: tent is " + Double2DArray.toString(tent));
			//Debugg.println("EigenVectors are " + Double2DArray.toString(eigenVectors));
			//Debugg.println("inverseEigenVectors are " + Double2DArray.toString(inverseEigenVectors));
			
		double[][] p = Double2DArray.multiply(eigenVectors, tent);
		probMatrix = Double2DArray.multiply(p, inverseEigenVectors);

		probMatrix = Double2DArray.squnch(probMatrix);
        	probMatrix = Double2DArray.transpose(probMatrix);
        	//boolean bogus = false;
        	//for (int i=0; i<probMatrix.length;i++)
        	//	for (int j=0;j<probMatrix[0].length;j++)
        	//		if (probMatrix[i][j]<= 0)
        	//			bogus = true;
        	//if (bogus){
        		//Debugg.println("Bad values in recalcProbabilities");
        	//	Debugg.println("rate matrix is " + Double2DArray.toString(rateMatrix));
        	//	Debugg.println("eigenValues are                       " + DoubleArray.toString(eigenValues));
        	//	Debugg.println("imaginary portions of eigenvalues are " + DoubleArray.toString(imagEigenValues));
        	//	//Debugg.println("tent is " + Double2DArray.toString(tent));
        	//	Debugg.println("EigenVectors are " + Double2DArray.toString(eigenVectors));
        	//	Debugg.println("inverseEigenVectors are " + Double2DArray.toString(inverseEigenVectors));
        	//	Debugg.println("result is " + Double2DArray.toString(probMatrix));
        	//}
        	
        	//Debugg.println("Trying alternate exp");
        //	double[][] altProbMatrix = altMatrixExp(rateMatrix, eigenValues,imagEigenValues,branchLength);
        	
        	//Debugg.println("Original calculation: " + Double2DArray.toString(probMatrix));
        	//Debugg.println("Alternate calculation: " + Double2DArray.toString(altProbMatrix));
        	//probMatrix = altProbMatrix;
        	recalcProbsNeeded = false;
	}

	// complex exponential
	double[] complexExp(double re,double im){
		double[] result = new double[2];
		result[0]=Math.cos(im)*Math.exp(re);
		result[1]=Math.sin(im)*Math.exp(re);
		return result;
	}
	
	int eigenValueStatus = UNEQUALTRIPLE;
	public final static int UNEQUALTRIPLE = 0;
	public final static int COMPLEXPAIR = 1;
	public final static int EQUALPAIR  = 2;
	public final static int EQUALTRIPLE = 3;
	
	public final static double equalTol = 0.001;
	
	double[][] altMatrixExp(double[][] baseArray,double[] eigenValues,double[] imagEigenValues,double t){
		Matrix C = new Matrix(baseArray);
		//Debugg.println("C is: ");
		
		// find the zero eigenvalue; it may only be approximately 0, but it will be the
		// largest, since the others all have negative real parts
		double largest = -1E99;  
		int largestIndex = -1;
		int evl = eigenValues.length;
		for (int i=0;i<evl;i++)
			if (eigenValues[i] > largest) {
				largest = eigenValues[i];
				largestIndex = i;
			}
		// check for a conjugate pair
		int imag1 = -1;
		int imag2 = -1;
		int realgroup1 = -1;
		int realgroup2 = -1;
		int realgroup3 = -1;
		for(int i=0;i<evl;i++){
			if (imagEigenValues[i] != 0)
				if (imag1 == -1)
					imag1 = i;
				else imag2 = i;
		}
		if (imag1 > -1){
			eigenValueStatus = COMPLEXPAIR;
			for(int i= 0; i<evl;i++)
				if (i != imag1 && i != imag2 && i != largestIndex)
					realgroup1 = i;   // hold lone negative real;
		}
		else {  // no conjugate pairs, check for multiple == reals
			for(int i=0;i<evl-1;i++)
				for(int j=i+1;j<evl;j++){
					//System.out.println("Test: " + Math.abs(eigenValues[i]-eigenValues[j]));
					if(Math.abs((eigenValues[i]-eigenValues[j])/eigenValues[i]) < equalTol){  // found a pair
						realgroup1=i;
						realgroup2=j;
						break;
					}
				}
				if (realgroup2 == -1)
					eigenValueStatus = UNEQUALTRIPLE;
				else {  //find realgroup3
					for(int i=0;i<evl;i++){
						if (i != largestIndex && i != realgroup1 && i != realgroup2)
							realgroup3 = i;
					}
					if (Math.abs((eigenValues[realgroup1]-eigenValues[realgroup3])/eigenValues[realgroup1])<equalTol)
						eigenValueStatus = EQUALTRIPLE;
					else 
						eigenValueStatus = EQUALPAIR;
				}	
		}

		Matrix x = null;
		Matrix identity = Matrix.identity(evl,evl);
		Matrix p1 = Matrix.identity(evl,evl);  // clone from identity?
		Matrix p2 = null;
		Matrix p3 = null;
		Matrix p4 = null;
		double[] workingValues = new double[evl];
		workingValues[0] = 0;      // assert that the max eigenvalue is identically zero!
		switch (eigenValueStatus) {
			case UNEQUALTRIPLE: {
				Debugg.println("Calculating unequal triple");
				int nexteigenslot=1;
				workingValues[0] = 0;
				for (int i=0;i<evl;i++){
					if(i!=largestIndex)
						workingValues[nexteigenslot++] = eigenValues[i];
				}	
				Debugg.print("Workingvalues of C are: " + DoubleArray.toString(workingValues));
				double z = workingValues[0];
				double k = workingValues[1];
				double m = workingValues[2];

				p2 = (C.minus(identity.times(z))).times(p1);              
				p3 = (C.minus(identity.times(k))).times(p2);
				p4 = (C.minus(identity.times(m))).times(p3);
				x = p1.times(r1(t,workingValues));
				x.plusEquals(p2.times(realR2(t,workingValues)));
				x.plusEquals(p3.times(realR3(t,workingValues)));
				x.plusEquals(p4.times(realR4(t,workingValues)));	
				break;
			}
			case COMPLEXPAIR: {
				double[] workingIValues = new double[evl];
				for(int i=0;i<evl;i++)
					workingIValues[i] = 0;
				workingValues[1] = eigenValues[imag1];
				workingIValues[1] = imagEigenValues[imag1];
				workingValues[2] = eigenValues[imag2];
				workingIValues[2] = imagEigenValues[imag2];
				workingValues[3] = eigenValues[realgroup1];

				Debugg.println("Calculating complex pair");
				//System.out.print("Workingvalues of C are: [");
				//for (int i=0;i<workingValues.length;i++)
					//System.out.print(workingValues[i] +" ");

				double z = workingValues[0];
				double k = workingValues[1];
				p2 = (C.minus(identity.times(z))).times(p1);
				p3 = (C.minus(identity.times(k))).times(p2);
				p4 = (C.minus(identity.times(k))).times(p3);
				x = p1.times(r1(t,workingValues));
				x.plusEquals(p2.times(complexPairR2(t,workingValues,workingIValues)));
				x.plusEquals(p3.times(complexPairR3(t,workingValues,workingIValues)));
				x.plusEquals(p4.times(complexPairR4(t,workingValues,workingIValues)));			
				break;
			}
			case EQUALPAIR: {
				workingValues[0] = 0;
				workingValues[1] = eigenValues[realgroup1];
				workingValues[2] = eigenValues[realgroup1];   // if they're close, force them equal
				workingValues[3] = eigenValues[realgroup3];
				
				Debugg.println("Calculating equal pair");
				Debugg.println("Working values are: " + DoubleArray.toString(workingValues));
		
				double z = workingValues[0];
				double k = workingValues[1];
				p2 = (C.minus(identity.times(z))).times(p1);
				p3 = (C.minus(identity.times(k))).times(p2);
				p4 = (C.minus(identity.times(k))).times(p3);
				
				x = p1.times(r1(t,workingValues));
				
				x.plusEquals(p2.times(realR2(t,workingValues)));
				x.plusEquals(p3.times(realPairR3(t,workingValues)));
				x.plusEquals(p4.times(realPairR4(t,workingValues)));	

				break;
			}
			case EQUALTRIPLE: {
				workingValues[0] = 0;
				workingValues[1] = eigenValues[realgroup1];
				workingValues[2] = eigenValues[realgroup2];
				workingValues[3] = eigenValues[realgroup3];
				Debugg.println("Calculating equal triple");
				//System.out.print("Workingvalues of C are: [");
				//for (int i=0;i<workingValues.length;i++)
					//System.out.print(workingValues[i] +" ");
				double z = workingValues[0];
				double k = workingValues[1];
				p2 = (C.minus(p1.times(z))).times(p1);
				p3 = (C.minus(p1.times(k))).times(p2);
				p4 = (C.minus(p1.times(k))).times(p3);
				
				x = p1.times(r1(t,workingValues));
				x.plusEquals(p2.times(realR2(t,workingValues)));
				x.plusEquals(p3.times(realPairR3(t,workingValues)));
				x.plusEquals(p4.times(realTripleR4(t,workingValues)));			
				break;
			}
		}
		return x.getArray();
	}
	
	// support functions for Putzer exponentiation
	double r1(double t,double[] realEigenValues){
		//double z = realEigenValues[0];
		//return Math.exp(t*z);
		return 1;
	}

	double realR2(double t, double[] realEigenValues){
		double k = realEigenValues[1];
		return (Math.exp(k*t) - 1)/(k);
	}
	
	double complexPairR2(double t, double[] realEigenValues, double[] imagEigenValues){
		double ka = realEigenValues[1];
		double kb = imagEigenValues[1];
		return -((ka*(1 - Math.exp(ka*t)*Math.cos(kb*t)))/(ka*ka + kb*kb)) + 
		  (Math.exp(ka*t)*kb*Math.sin(kb*t))/(ka*ka + kb*kb);
	}
	
	
	
	double realR3(double t, double[] realEigenValues){
		//double z = realEigenValues[0];
		double k = realEigenValues[1];
		double m = realEigenValues[2];
		return (Math.exp(m*t)*k - k - Math.exp(k*t)*m + m)/((-k + m)*(k)*(m)); 
	}
	
	double complexPairR3(double t, double[] realEigenValues, double[] imagEigenValues){
		double ka = realEigenValues[1];
		double kb = imagEigenValues[1];
		return (2*kb - 2*Math.exp(ka*t)*kb*Math.cos(kb*t) + 2*Math.exp(ka*t)*ka*Math.sin(kb*t))/ 
		  (2*ka*ka*kb + 2*kb*kb*kb);
	}
	
	double realPairR3(double t, double[] values){
		double k = values[1];
		//(1 - E^(k*t) + E^(k*t)*k*t)/k^2
		return (1 - Math.exp(k*t) + Math.exp(k*t)*k*t)/(k*k);
	}
	
	double realR4(double t, double[] realEigenValues){
		double k = realEigenValues[1];
		double m = realEigenValues[2];
		double n = realEigenValues[3];
		return (-(Math.exp(n*t)*k*k*m) + k*k*m + 
		          Math.exp(n*t)*k*m*m - k*m*m + Math.exp(m*t)*k*k*n - 
		          k*k*n - Math.exp(k*t)*m*m*n + m*m*n - 
		          Math.exp(m*t)*k*n*n + k*n*n + Math.exp(k*t)*m*n*n - 
		          m*n*n)/((-k + m)*(-k + n)*(-m + n)*(k)*(m)*(n));
	}

	double complexPairR4(double t, double[] realEigenValues, double[] imagEigenValues){
		double ka = realEigenValues[1];
		double kb = imagEigenValues[1];
		double n = realEigenValues[3];
		return (-2*ka*ka*kb + 2*Math.exp(n*t)*ka*ka*kb - 2*kb*kb*kb + 2*Math.exp(n*t)*kb*kb*kb + 
			    4*ka*kb*n - 2*kb*n*n - 4*Math.exp(ka*t)*ka*kb*n*Math.cos(kb*t) + 
			    2*Math.exp(ka*t)*kb*n*n*Math.cos(kb*t) + 
			    2*Math.exp(ka*t)*ka*ka*n*Math.sin(kb*t) - 
			    2*Math.exp(ka*t)*kb*kb*n*Math.sin(kb*t) - 2*Math.exp(ka*t)*ka*n*n*Math.sin(kb*t))/
			    (2*kb*(ka*ka + kb*kb)*n*(ka*ka + kb*kb - 2*ka*n + n*n));
	}
	
	double realPairR4(double t, double [] values){
		double k = values[1];
		double n = values[3];
		return (-k*k + Math.exp(n*t)*k*k + 2*k*n - 2*Math.exp(k*t)*k*n - 
			       n*n + Math.exp(k*t)*n*n + Math.exp(k*t)*k*k*n*t - Math.exp(k*t)*k*n*n*t)/(k*k*n*(n-k)*(n-k));
	}
	double realTripleR4(double t, double[] values){
		// TODO finish me
		double k = values[1];
		return (-2 + 2*Math.exp(k*t) - 2*Math.exp(k*t)*k*t +  Math.exp(k*t)*k*k*t*t)/(2*k*k*k);
	}

	double limit = 10000;
	int beginningAllowed = 6;
    /**
     * This version is the main version of the evaluator
     * @param values is the array of proposed parameter values
     * @param obj is ignored here and should be null (maybe just pass through command record)
     */
	public double evaluate(double[] values, Object obj) {
		if (values.length >= 4) {
			//CommandRecord cr = b.getCommandRecord();
			double height = workingTree.tallestPathAboveNode(workingTree.getRoot()); // to stop the optimization from wandering if very high rates
			if (!sanityChecks(values,limit, height))
				return MesquiteDouble.veryLargeNumber;
			params = DoubleArray.clone(values);
			//Debugg.println("Calling prepareMatrices");
			prepareMatrices();
			double result =  -this.logLikelihoodCalc(workingTree);
			//if (cr !=null)
			//	cr.tick("Estimating asymm model:  params " + param0 + " " + param1 + " -logL " + result); 
			//Debugg.println("Evaluate returned "+ result + "from parameters: " + DoubleArray.toString(values));
			return result;
		}
		else
			return 0;
	}
	
	int estCount =0;
	boolean zeroHit = false;
	/*.................................................................................................................*/
	public double logLikelihoodCalc(Tree tree){
		if (zeroHit)
			return -0.001*(++estCount);  // to shortcircuit the optimizer wandering around zero with very high rates
		//model.setCharacterDistribution(observedStates);
		double likelihood = 0.0;
		double comp;
		int root = tree.getRoot();
		if (rootMode == SUM){
			initProbs2(tree.getNumNodeSpaces(), observedStates1.getMaxState()+1,observedStates2.getMaxState()+1); 
			estCount++;
			downPass2(root,tree);
			//Debugg.println("Checking downProbs2 " + Double2DArray.toString(downProbs2[root]));
			for (int i = 0; i<=maxState[0]; i++)
				for (int j = 0; j<=maxState[1];j++) {
					savedRootEstimates[j][i] = downProbs2[root][j][i];
					//Debugg.println("Estimates at root from [" + i + ", " + j +"]" + Math.log(tmp));
					if (savedRootEstimates[j][i] < 0){      // problem, probably due to bad matrix operation
						likelihood = 0;
						return(-1*MesquiteDouble.veryLargeNumber);
					}
					else
						likelihood += savedRootEstimates[j][i];
				}
			//Debugg.println("Sum is " + likelihood);
		}
		else 
			Debugg.println("Subroot prior and subroot empirical not yet supported");
		//}
		//else if (rootMode == SUBROOT_PRIOR){
		//	if (tree.getRooted())
		//		for (int i=0; i<numStates; i++) 
		//			empirical[i] = model.priorProbability(i);
		//	for (int i=0;  i<=model.getMaxState(); i++) {
		//		likelihood += downProbs[root][i]*probFromSection(tree, root, i, empirical, model, false);
		//	}
		//}
		//else if (rootMode == SUBROOT_EMPIRICAL){
		//	calculateEmpirical(tree, observedStates, empirical);
		//	for (int i=0;  i<=model.getMaxState(); i++) {
		//		likelihood += downProbs[root][i]*probFromSection(tree, root, i, empirical, model, false);
		//	}
		//}
		comp = underflowCompDown[root];
		//Debugg.println("Likelihood is " + likelihood + " and comp is " + comp);
		double logLike = Math.log(likelihood) - comp;
		//if (Double.isNaN(logLike)){
		//	Debugg.println("logLikelihoodCalc returned Nan from :" + DoubleArray.toString(params));
		//	Debugg.println("Raw likelihood was" + likelihood + " underflow comp was " + comp);
		//}

	//	if (logLike> -0.00001) {
	//		zeroHit = true;
	//	}
		return (logLike);
	}
 		
 	private void initProbs2(int nodes, int numStates1, int numStates2) {
 		if (numStates1 != numStates2) {
 			//Debugg.println("Warning - different numbers of states in the two characters in Pagel94");
 			if (numStates1 >= numStates2)
 				this.numStates = numStates1;
 			else
 				this.numStates = numStates2;
 		}
 		else if (numStates1 == 1) 
 			numStates =2;
 			else numStates = numStates1;
 		if  (downProbs2==null || downProbs2.length!=nodes || downProbs2[0].length!=numStates1 || downProbs2[0][0].length!=numStates2){
 		 		downProbs2 = new double[nodes][numStates][numStates];
 		 		underflowCompDown = new double[nodes];
 		 		//empirical2 = new double[numStates][numStates];
 		 		savedRootEstimates = new double[numStates][numStates];
 		}
 		//Double3DArray.zeroArray(downProbs2);
 		for(int i=0;i<nodes;i++){
 			Double2DArray.zeroArray(downProbs2[i]);
 		}
 		DoubleArray.zeroArray(underflowCompDown);
 		//Double2DArray.zeroArray(empirical2);
 		Double2DArray.deassignArray(savedRootEstimates);
 	}
 		
	// overloading for 2 characters
	private double probFromSection(Tree tree, int d, int i, int j, double[][] ProbsD, boolean downwards){
		double prob=0;
		if(downwards){
			for (int k=0;k<numStates;k++)
				for (int l=0;l<numStates;l++){
					prob += ProbsD[k][l]*transitionProbability(i,k,j,l,tree,d);
				}
		}
		else {
			for (int k=0;k<numStates;k++)
				for (int l=0;l<numStates;l++)
					prob += transitionProbability(k,i,l,j,tree,d)*ProbsD[i][j];
		}
		//if (prob <0)
		//	Debugg.println("In prob from section final prob is " + prob + " at node " + d + " for i="+ i + " and for j=" + j);
		return prob;
	}
	
	
	/*.................................................................................................................*/	
	// overloading for 2 characters
	private double checkUnderflow(double[][] probs){
		minChecker.setValue(MesquiteDouble.unassigned);
		for (int i=0;i<probs.length;i++)
			for (int j=0;j<probs[0].length;j++)
				minChecker.setMeIfIAmMoreThan(probs[i][j]);
		double q = minChecker.getDoubleValue();
		if (q == 0)
			return 0;
		else {
			for (int i=0;i<probs.length;i++)
				for (int j=0;j<probs[0].length;j++)
					probs[i][j] /= q;
		}
		//if (Double.isNaN(Math.log(q)))
		//	Debugg.println("Caught Nan in checkUnderFlow, orginal value was " + q);
		return -Math.log(q);
	}


	/*.................................................................................................................*/
	//can't overload since no arrays passed
	// assumes hard polytomies
	private void downPass2(int node, Tree tree) {
		if (tree.nodeIsTerminal(node)) {
			long observed1 = ((CategoricalDistribution)observedStates1).getState(tree.taxonNumberOfNode(node));
			long observed2 = ((CategoricalDistribution)observedStates2).getState(tree.taxonNumberOfNode(node));
			int obs1 = CategoricalState.minimum(observed1); //NOTE: just minimum observed!
			int obs2 = CategoricalState.minimum(observed2);
			Double2DArray.zeroArray(downProbs2[node]);
			if (obs1>=0 && obs1 < downProbs2[node].length && !CategoricalState.isUnassigned(observed1) && !CategoricalState.isInapplicable(observed1) &&
				obs2>=0 && obs2 < downProbs2[node][0].length && !CategoricalState.isUnassigned(observed2) && !CategoricalState.isInapplicable(observed2)) {
				downProbs2[node][obs1][obs2] = 1;
			}
		}
		else {
			Double2DArray.zeroArray(downProbs2[node]);
			underflowCompDown[node]=0;
			for (int d= tree.firstDaughterOfNode(node); tree.nodeExists(d); d = tree.nextSisterOfNode(d)) {
				downPass2(d,tree);
				underflowCompDown[node] += underflowCompDown[d];
			}			
			for (int i=0;i<numStates;i++)
				for (int j=0;j<numStates;j++){
					double prob = 1.0;
					for (int d = tree.firstDaughterOfNode(node); tree.nodeExists(d); d=tree.nextSisterOfNode(d)){
						prob *= probFromSection(tree,d,i,j,downProbs2[d],true);
					}
					downProbs2[node][i][j] = prob;
					//if (prob <0)
					//	Debugg.println("Found negative prob in downPass2; prob = " + prob +" node =" + node + "i,j = " + i + ", " + j);
			}
			if (++underflowCheck % underflowCheckFrequency == 0){
				underflowCompDown[node] += checkUnderflow(downProbs2[node]);
			}
		}
	}

	int allowedEdgeHits = 0; // to stop the optimization from wandering if very high rates; edge allowed to be hit only a certain number of tiems
	private boolean sanityChecks(double [] eParams, double limit, double height){
		for(int i=0;i<eParams.length;i++){
			if (eParams[i]*height>limit && allowedEdgeHits--<0) { // too big
				return false;
			}
			if (eParams[i]<=0){  
				return false;
			}
		}
		return true;
	}
	

	/**
	 * This is really a stub
	 * 
	 */
	public double evaluate(MesquiteDouble param, Object obj) {
		// TODO Maybe add a class for multivariate evaluators
		return 0;
	}
	
	//Wayne: this is here so you can use it in Pagel94	- feel free to change the default value
	public MesquiteInteger getExtraSearch() {
		return eightParameterExtraSearch;
	}

	
	/*
	 * This just rescales the fixed parameters (as yet unimplemented) by rescale
	 */
	private void rescaleFixedParameters(double rescale){
		for(int i=0;i<qConstant.length;i++)
			if (qConstrained[i] && qBound[i]<0 && MesquiteDouble.isCombinable(qConstant[i]))
				qConstant[i] *= rescale;
	}

	private void rescaleAllParameters(double rescale){
		for(int i=0;i<params.length;i++)
			params[i] *= rescale;
	}

    static final boolean scaleRescale = true;  
 
    public void estimateParameters(Tree originalTree, CategoricalDistribution observedStates1, CategoricalDistribution observedStates2, CommandRecord commandRec) {
    		this.observedStates1 = observedStates1;
    		this.observedStates2 = observedStates2;
    		// Special treatment for constant states just isn't working out here
    		if (observedStates1.getMaxState()>= 1)
    			maxState[0] = observedStates1.getMaxState();
    		else
    			maxState[0] = 1;
    		if (observedStates2.getMaxState()>= 1)
        		maxState[1] = observedStates2.getMaxState();
    		else
    			maxState[1] = 1;

		allStates[0] = CategoricalState.span(0,maxState[0]);
		allStates[1] = CategoricalState.span(0,maxState[1]);
		double height = 0;
		double invHeight = MesquiteDouble.unassigned;
		if (scaleRescale){  //rescales tree to 1.0 height to help optimization, which takes longer with larger numbers
			MesquiteTree mTree = originalTree.cloneTree();
			mTree.setAllUnassignedBranchLengths(1.0, false);
			height = mTree.tallestPathAboveNode(mTree.getRoot(), 1.0); // to adjust start point depending on how tall is tree
			if (height != 0){
				invHeight = 1.0/height;
				mTree.scaleAllBranchLengths(invHeight, false);
				rescaleFixedParameters(height);
			}
			workingTree= mTree;
		}
		else  
			workingTree = originalTree;
		Optimizer opt = new Optimizer(this);
		double best = Double.MAX_VALUE;
		// **** estParams is the matrix of trial values
		double [] b = null;
		double next;
		switch (modelType) {
			case MODEL8PARAM:{  // first try two versions of a 6-parameter model
				PagelMatrixModel model6_1 = new PagelMatrixModel("",CategoricalState.class,MODEL6PARAMCONTINGENTCHANGEY);
				PagelMatrixModel model6_2 = new PagelMatrixModel("",CategoricalState.class,MODEL6PARAMCONTINGENTCHANGEX);
				double estParams[] = new double[8];
				// first a model with changes in Y contingent on changes in X
				if (parametersFromSimplerModel == null || simplerModelType != MODEL6PARAMCONTINGENTCHANGEY){
					//Debugg.println("Getting start point from MODEL6PARAMCONTINGENTCHANGEY");
				    model6_1.setParametersFromSimplerModel(parametersFromSimplerModel,simplerModelType);
					model6_1.estimateParameters(workingTree,observedStates1, observedStates2, commandRec);
					double[] m6_1Params = model6_1.getParams();
					for(int i=0;i<4;i++)
						estParams[i]=m6_1Params[i];
					estParams[4]=m6_1Params[4];
					estParams[5]=m6_1Params[0];
					estParams[6]=m6_1Params[5];
					estParams[7]=m6_1Params[2];
				}
				else { 
					for(int i=0;i<4;i++)
						estParams[i]=parametersFromSimplerModel[i];
					estParams[4]=parametersFromSimplerModel[4];
					estParams[5]=parametersFromSimplerModel[0];
					estParams[6]=parametersFromSimplerModel[5];
					estParams[7]=parametersFromSimplerModel[2];
				}
				allowedEdgeHits = beginningAllowed;
				double [] backupEst = (double [])estParams.clone();
				//Debugg.println("Input to optimizer for 8 parameter model is " + DoubleArray.toString(estParams));
				next = opt.optimize(estParams, null); // try optimizing from the contingentchangeY best parameters
				if (!acceptableResults(next, estParams)) {
					MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
					reportUnacceptableValues(next, estParams,backupEst);
				}
				else {     //shouldn't need to compare best with next here
					best = next;
					if (b == null)
						b = DoubleArray.clone(estParams);	
					else
						for (int i=0; i< estParams.length; i++)
							b[i] = estParams[i];
				}
				//Debugg.println("best is now " + best + " b is now " + DoubleArray.toString(b) );
				// now the other 6-parameter model (changes in X contingent on Y)
				if (parametersFromSimplerModel == null || simplerModelType != MODEL6PARAMCONTINGENTCHANGEX){
				    model6_2.setParametersFromSimplerModel(parametersFromSimplerModel,simplerModelType);
					//Debugg.println("Getting start point from MODEL6PARAMCONTINGENTCHANGEY");
					//Debugg.println("Started at " + System.currentTimeMillis());
					model6_2.estimateParameters(workingTree,observedStates1, observedStates2, commandRec);
					//Debugg.println("6PContingentX Finished at " + System.currentTimeMillis());
					double[] m6_2Params = model6_2.getParams();
					for(int i=0;i<4;i++)
						estParams[i]=m6_2Params[i];
					estParams[4]=m6_2Params[1];
					estParams[5]=m6_2Params[4];
					estParams[6]=m6_2Params[3];
					estParams[7]=m6_2Params[5];
				}
				else { 
					for(int i=0;i<4;i++)
						estParams[i]=parametersFromSimplerModel[i];
					estParams[4]=parametersFromSimplerModel[1];
					estParams[5]=parametersFromSimplerModel[4];
					estParams[6]=parametersFromSimplerModel[3];
					estParams[7]=parametersFromSimplerModel[5];
				}
				allowedEdgeHits = beginningAllowed;
				//Debugg.println("Input to optimizer for 8 parameter model is " + DoubleArray.toString(estParams));
				backupEst = (double [])estParams.clone();
				next = opt.optimize(estParams, null); // try optimizing from the contingentchangeX best parameters
				if (!acceptableResults(next, estParams)) {
					MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
					reportUnacceptableValues(next, estParams,backupEst);
				}
				else if (next<best) {
					best = next;
					if (b == null)
						b = DoubleArray.clone(estParams);	
					else
						for (int i=0; i< estParams.length; i++)
							b[i] = estParams[i];
				}
				//PETER: added these tick notifications to keep user informed of progress of search
				if (commandRec != null) commandRec.tick("8 parameter model preliminary -ln likelihood: " + MesquiteDouble.toString(best));
				//Debugg.println("best is now " + best + " b is now " + DoubleArray.toString(b) );
				if (eightParameterExtraSearch.getValue() > 0) {
					//Debugg.println("Trying random linear combinations of parameters from 6p models");
					//Debugg.println("Model 1 params are " + DoubleArray.toString(model6_1.getParams()));
					//Debugg.println("Model 2 params are " + DoubleArray.toString(model6_2.getParams()));
					double [] m6_1Params = model6_1.getParams();
					double [] m6_2Params = model6_2.getParams();
					for (int i = 0; i< eightParameterExtraSearch.getValue();i++){
					//Debugg.println("Combination " + i);
						for(int j=0;j<4;j++)
							estParams[j]=Math.abs(m6_1Params[j] + (2*rng.nextDouble()-1)*(m6_2Params[j]+m6_1Params[j]));
						estParams[4]=(1+0.2*rng.nextGaussian())*m6_1Params[4];
						estParams[5]=(1+0.2*rng.nextGaussian())*m6_2Params[4];
						estParams[6]=(1+0.2*rng.nextGaussian())*m6_1Params[5];
						estParams[7]=(1+0.2*rng.nextGaussian())*m6_2Params[5];
						allowedEdgeHits = beginningAllowed;
						//Debugg.println("Input to optimizer for 8 parameter model is " + DoubleArray.toString(estParams));
						backupEst = (double [])estParams.clone();
						next = opt.optimize(estParams, null); // try optimizing from the contingentchangeX best parameters
						//Debugg.println("Score is " + next);
						//Debugg.println(" " +next);
						if (!acceptableResults(next, estParams)) {
							MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
							reportUnacceptableValues(next, estParams,backupEst);
						}
						else if (next<best) {
							best = next;
							if (b == null)
								b = DoubleArray.clone(estParams);	
							else
								for (int m=0; m< estParams.length; m++)
									b[m] = estParams[m];
						}
						//Debugg.println("best is now " + best); // + " b is now " + DoubleArray.toString(b) );
						if (commandRec != null) commandRec.tick("8 parameter model -ln likelihood after " + (i+1) + " searches: " + MesquiteDouble.toString(best));
				}
				}
				break;
			}
			case MODEL6PARAMCONTINGENTCHANGEX:
			case MODEL6PARAMCONTINGENTCHANGEY: {
				double[] estParams = new double[6];
				if (parametersFromSimplerModel == null || simplerModelType != MODEL4PARAM) {
					//Debugg.println("Getting start point from MODEL4PARAM");
					PagelMatrixModel model4 = new PagelMatrixModel("",CategoricalState.class,MODEL4PARAM);
					model4.estimateParameters(workingTree,observedStates1, observedStates2, commandRec);
					double [] m4Params = model4.getParams();
					for(int i=0;i<4;i++)
						estParams[i]=m4Params[i];
					if (modelType == MODEL6PARAMCONTINGENTCHANGEX){  //this really needs to be checked
						estParams[4]=m4Params[2];
						estParams[5]=m4Params[3];
					}
					else {
						estParams[4]=m4Params[0];
						estParams[5]=m4Params[1];
					}
				}
				else { // TODO need to get the parameters from the previous model
					for(int i=0;i<4;i++)
						estParams[i]=parametersFromSimplerModel[i];
					if (modelType == MODEL6PARAMCONTINGENTCHANGEX){  //this really needs to be checked
						estParams[4]=parametersFromSimplerModel[2];
						estParams[5]=parametersFromSimplerModel[3];
					}
					else {
						estParams[4]=parametersFromSimplerModel[0];
						estParams[5]=parametersFromSimplerModel[1];
					}
				}
				allowedEdgeHits = beginningAllowed;
				double [] backupEst = (double [])estParams.clone();
				next = opt.optimize(estParams, null); // try optimizing from the contingentchangeX best parameters
				if (!acceptableResults(next, estParams)) {
					MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
					reportUnacceptableValues(next, estParams,backupEst);
					if (b == null)
						b = DoubleArray.clone(estParams);	//only do this to get b set to something...
				}
				else if (next<best) {
					best = next;
					if (b == null)
						b = DoubleArray.clone(estParams);	
					else
						for (int i=0; i< estParams.length; i++)
							b[i] = estParams[i];
				}
				//Debugg.println("best is now " + best + " b is now " + DoubleArray.toString(b) );
				break;
			}
			case MODEL4PARAM: {  // for now, just use flip/flop, though we might try getting estimates from MODEL2PARAM in the future
				double[] estParams = new double[4];
				estParams[2] = FLIP_FLOP_HIGH;
				estParams[3] = FLIP_FLOP_HIGH;
				estParams[0] = FLIP_FLOP_HIGH;
				estParams[1] = FLIP_FLOP_HIGH;
				if (b == null)
					b = DoubleArray.clone(estParams);	
				double [] backupEst = (double [])estParams.clone();
		 		next = opt.optimize(estParams, null); //bundle);
		 		if (!acceptableResults(next, estParams)) {
		 			MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
					reportUnacceptableValues(next, estParams,backupEst);
		 		}
		 		else if (next<best) {
		 			best = next;
					if (b == null)
						b = DoubleArray.clone(estParams);	
					else
						for (int i=0; i< estParams.length; i++)
							b[i] = estParams[i];
		 		}
				estParams[0] = FLIP_FLOP_LOW;
				estParams[2] = FLIP_FLOP_LOW; //
				estParams[1] = FLIP_FLOP_HIGH;  //forward
				estParams[3] = FLIP_FLOP_HIGH;
			 	params = DoubleArray.clone(estParams);
				allowedEdgeHits = beginningAllowed;
				backupEst = (double [])estParams.clone();
		 		next = opt.optimize(estParams, null); //bundle);
		 		if (!acceptableResults(next, estParams)) {
		 			MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
					reportUnacceptableValues(next, estParams,backupEst);
		 		}
		 		else if (next<best) {
		 			best = next;
					if (b == null)
						b = DoubleArray.clone(estParams);	
					else
						for (int i=0; i< estParams.length; i++)
							b[i] = estParams[i];
		 		}
		 		estParams[0] = FLIP_FLOP_HIGH;
				estParams[2] = FLIP_FLOP_HIGH; //
				estParams[1] = FLIP_FLOP_LOW;  //forward
				estParams[3] = FLIP_FLOP_LOW;
			 	params = DoubleArray.clone(estParams);
				allowedEdgeHits = beginningAllowed;
				backupEst = (double [])estParams.clone();
		 		next = opt.optimize(estParams, null); //bundle);
		 		if (!acceptableResults(next, estParams)) {
		 			MesquiteMessage.println("Warning: NaN encountered in PagelMatrixModel optimization");
					reportUnacceptableValues(next, estParams,backupEst);
		 		}
		 		else if (next<best) {
		 			best = next;
					if (b == null)
						b = DoubleArray.clone(estParams);	
					else
						for (int i=0; i< estParams.length; i++)
							b[i] = estParams[i];
		 		}
				if (commandRec != null) commandRec.tick("4 parameter model -ln likelihood: " + MesquiteDouble.toString(best));
				break;
			}
			case MODEL2PARAM: {  // NO-OP FOR NOW, the model exists, the framework for using it doesn't
			 	MkModel model2 = new MkModel("asymm helper", CategoricalState.class); //used to provide preliminary guess at rate
				Debugg.println("The 2 parameter model isn't currently supported here");
				break;
			}
		} // end switch (??)
		params = DoubleArray.clone(b);
		if (scaleRescale && height != 0){ //UNDO the scaling of the tree
			rescaleAllParameters(invHeight);
			workingTree = originalTree;  // faster, less error prone than rescaling from mTree??
		 }
		//prepareMatrices();
	}
    
	
    /**
     * 
     * @return array of doubles containing the parameters estimated for this model
     */
    public double[] getParams() {
    		return params;
    }
    
    /**
     * 
     */
    public double[][] getRootPriors(){
    		double[][] rootPriors;
    		if (savedRootEstimates == null)
    			return null;
    		else {
    			double total = 0;
    			int estimateLength = savedRootEstimates.length;  // what are these really?
    			int estimateWidth = savedRootEstimates[0].length;
    			rootPriors = new double[estimateLength][estimateWidth];
    			for (int i=0;i<estimateLength;i++)
    				for (int j=0;j<estimateWidth;j++)
    					total += savedRootEstimates[i][j];
    			for (int i=0;i<estimateLength;i++)
    				for (int j=0;j<estimateLength;j++)
    					rootPriors[i][j] = savedRootEstimates[i][j]/total;
    		}
    		return rootPriors;
    }
    
    /**
     * This provides a mechanism to pass best parameter estimates from a simpler (independent/4 parameter) model
     * @param hint array of doubles containing the parameter estimates
     */
    public void setParametersFromSimplerModel(double[] hint,int mtype){
		parametersFromSimplerModel = hint;
		simplerModelType = mtype;
    }

    

	public void deassignParameters() {
		// TODO Auto-generated method stub
	}
	
	public long[] getRootStates(Tree tree, double[][] rootStatePriors){
		long[] result = new long[charsFromBehavior(modelType)];   // always a safe choice
		if ((rootStatePriors != null) && (modelType < MODEL3CHARACTERINDEPENDENT)){
			double r = randomNumGen.nextDouble();
			double accumProb = 0;
			for (int i=0;i<rootStatePriors[0].length;i++) {
				for (int j=0;j<rootStatePriors.length;j++) {
					accumProb += rootStatePriors[j][i];
					if (r<accumProb){
						result[0]=CategoricalState.makeSet(i);
						result[1]=CategoricalState.makeSet(j);
						return result;
					}
				}
			}
			result[0]=rootStatePriors[0].length;
			result[1]=rootStatePriors[1].length;
			return result;
		}
		else {
			for(int c=0;c<result.length;c++){
				result[c]=getRootState(tree,c);
			}
			return result;
		}
	}
	
	public long getRootState (Tree tree,int c){
		if (maxState[c] <= 0) 
			return (1L);  //if only state 0 allowed, return it
		else {
			double r = randomNumGen.nextDouble();
			double accumProb = 0;
			for (int i=0; i<maxState[c]; i++) {
				accumProb +=  priorProbability(i,c);
				if (r< accumProb)
					return CategoricalState.makeSet(i);
			}
			return CategoricalState.makeSet(maxState[c]);
		}
	}
	
	
	/**
	 * return a flat prior for a character estimate at the root
	 */
	public double flatPriorProbability (int state,int c){
		if (!inStates(state,c)) 
			return 0;
		else
			if (maxState[c] == 0)
				return (0.5);  // flat prior
			else
				return (1.0/(maxState[c]+1));  // flat prior
	}

	/* ---------------------------------------------*/	
	public int[] evolveState (int beginState[], Tree tree, int node){
		int index = recodeStatePair(beginState[0],beginState[1]);
		double r = randomNumGen.nextDouble();
		double branchLength = tree.getBranchLength(node, 1.0);
		if (needToPrepareMatrices)
			prepareMatrices();
		if (recalcProbsNeeded || previousBranchLength != branchLength)
			recalcProbabilities(branchLength);
		if (probMatrix == null)
			return null;
		if (index> probMatrix.length)  //TODO replace with appropriate test
			return null;
		double accumProb = 0;
		int resultCode = recodeStatePair(maxState[0],maxState[1]);
		for (int i=0; i<probMatrix[index].length; i++) {
			accumProb +=  probMatrix[index][i];
			if (r< accumProb){
				resultCode = i;
				break;
			}
		}
		return statePairFromCode(resultCode);
	}

	
	public void evolveState (CharacterState[] beginState, CharacterState[] endState, Tree tree, int node){
		if (beginState == null || endState==null) {
			return;
		}
		if (!(beginState instanceof CategoricalState[]) || !(endState instanceof CategoricalState[])){
			for(int i=0;i<endState.length;i++)
				endState[i].setToUnassigned();
			return;
		}
		CategoricalState[] bState = (CategoricalState[])beginState;
		CategoricalState[] eState = (CategoricalState[])endState;
		int [] bsa = new int[bState.length];
		for(int i=0;i<bsa.length;i++)
			bsa[i] = CategoricalState.minimum(bState[i].getValue());
		int []r = evolveState(bsa, tree, node); //todo: issue error if beginning is polymorphic?  choose randomly among beginning?
		Debugg.println("Evolving from " + IntegerArray.toString(bsa) + " to " + IntegerArray.toString(r));
		for(int i=0;i<r.length;i++)
			eState[i].setValue(CategoricalState.makeSet(r[i]));
	}

	public String getModelTypeAsString() {
		switch (modelType){
			case MODEL4PARAM:{
				return "4 Parameter model";
			}
			case MODEL8PARAM:{
				return "8 parameter model";
			}
			case MODEL7PARAMCONTINGENTCHANGEYFORWARD: {
				return "7 parameter model y forward change contingent on x";
			}
			case MODEL7PARAMCONTINGENTCHANGEYBACKWARD: { 
				return "7 parameter model y backward change contigent on x";
			}
			case MODEL7PARAMCONTINGENTCHANGEXFORWARD: {
				return "7 parameter model x forward change contingent on y";
			}
			case MODEL7PARAMCONTINGENTCHANGEXBACKWARD: {
				return "7 parameter model x forward change contingent on y";
			}
			case MODEL6PARAMCONTINGENTCHANGEY: {
				return "6 parameter model y change contingent on x";
			}
			case MODEL6PARAMCONTINGENTCHANGEX:{
				return "6 parameter model x forward change contingent on y";
			}
		}
		return null;
	}
	public String getParameters() {
		switch (modelType){
			case MODEL4PARAM:{
				return "q12(alpha1) =" + params[1] + "\nq13(alpha2) = " 
				             + params[0] + "\nq21(beta1) = " + params[3] + "\nq31(beta2) = " + params[2];
			}
			case MODEL8PARAM:{
				return "q12 = " + params[0] + "\nq13 = " + params[1] + 
				         "\nq21 = " + params[2] + "\nq31 = " + params[3] +
				         "\nq24 = " + params[4] + "\nq34 = " + params[5] +
				         "\nq42 = " + params[6] + "\nq43 = " + params[7];

			}
			case MODEL7PARAMCONTINGENTCHANGEYFORWARD: {  //ToDo check me
				return "q12 = " + params[0] + " q13 = " + params[1] + 
		                  " q21 = " + params[2] + " q31 = " + params[3] +
		                  " q24 = " + params[4] + " q34 = " + params[0] +
		                  " q42 = " + params[5] + " q43 = " + params[6];
			}
			case MODEL7PARAMCONTINGENTCHANGEYBACKWARD: { //TODO check me
				return "q12 = " + params[0] + " q13 = " + params[1] + 
		                  " q21 = " + params[2] + " q31 = " + params[3] +
		                  " q24 = " + params[4] + " q34 = " + params[5] +
		                  " q42 = " + params[6] + " q43 = " + params[2];
			}
			case MODEL7PARAMCONTINGENTCHANGEXFORWARD: { //TODO check me
				return "q12 = " + params[0] + " q13 = " + params[1] + 
		                  " q21 = " + params[2] + " q31 = " + params[3] +
		                  " q24 = " + params[1] + " q34 = " + params[4] +
		                  " q42 = " + params[5] + " q43 = " + params[6];
			}
			case MODEL7PARAMCONTINGENTCHANGEXBACKWARD: { //TODO check me
				return "q12 = " + params[0] + " q13 = " + params[1] + 
		                  " q21 = " + params[2] + " q31 = " + params[3] +
		                  " q24 = " + params[4] + " q34 = " + params[5] +
		                  " q42 = " + params[3] + " q43 = " + params[6];
			}
			case MODEL6PARAMCONTINGENTCHANGEY: { //TODO check me
				return "q12 = " + params[0] + " q13 = " + params[1] + 
		                  " q21 = " + params[2] + " q31 = " + params[3] +
		                  " q24 = " + params[4] + " q34 = " + params[0] +
		                  " q42 = " + params[5] + " q43 = " + params[2];
			}
			case MODEL6PARAMCONTINGENTCHANGEX:{  //TODO trim me
				return "q12 = " + params[0] + " q13 = " + params[1] + 
		                  " q21 = " + params[2] + " q31 = " + params[3] +
		                  " q24 = " + params[1] + " q34 = " + params[4] +
		                  " q42 = " + params[3] + " q43 = " + params[5];
			}
		}
		return null;
	}
	
	
	public void copyToClone(CharacterModel md){
		if (md == null || !(md instanceof PagelMatrixModel))
			return;
		super.copyToClone(md);
		PagelMatrixModel model = (PagelMatrixModel)md;
		model.params = (double[])params.clone();
		model.recalcProbsNeeded = true;
		model.needToPrepareMatrices = true;
		model.qMapping = (int [])qMapping.clone();          // mapping from known, underparameterized models
		model.qConstrained = (boolean[]) qConstrained.clone();  // true if a q value is somehow constrained
		model.qBound = (int []) qBound;           // qvalue bound to another qvalue (-1 = not bound)
		model.qConstant = (double []) qConstant;     // qvalue is set to this constant; 
		model.savedRootEstimates = (double [][])savedRootEstimates;
		model.prepareMatrices();
		model.notifyListeners(model, new Notification(MesquiteListener.UNKNOWN));
	}
	


	public CharacterModel cloneModelWithMotherLink(CharacterModel formerClone){
		PagelMatrixModel j = new PagelMatrixModel(name, getStateClass(), modelType);
		copyToClone(j);
		j.recalcProbsNeeded = true;
		j.needToPrepareMatrices = true;
		completeDaughterClone(formerClone, j);
		j.prepareMatrices();
		return j;			
	}

	public boolean isFullySpecified() {
		boolean result = true;
		for (int i = 0;i<params.length;i++)
			if (params[i] == MesquiteDouble.unassigned)
				result = false;
		return result;
	}

	public String getModelTypeName() {
		// TODO Auto-generated method stub
		return "Stochastic matrix model as described in Pagel1994";
	}

	public String getExplanation() {
		return "Estimates and simulates discrete characters using the matrices described in Pagel 1994";
	}
	
	
    public void showSurface(Tree tree,CategoricalDistribution dist1, CategoricalDistribution dist2, int divisions) {
    	    Tree savedTree = workingTree;
    	    workingTree = tree;
    	    CategoricalDistribution savedObservedStates1 = observedStates1;
    	    observedStates1 = dist1;
    	    CategoricalDistribution savedObservedStates2 = observedStates2;
    	    observedStates2 = dist2;
        double [] outBounds = new double[params.length];
        double [] upperBounds = new double[params.length];
        for (int i = 0;i<upperBounds.length;i++)
        		upperBounds[i] = 8.5;
        double [] lowerBounds = new double[params.length];
        for (int i = 0;i<lowerBounds.length;i++)
        		lowerBounds[i] = 0.5;
        getLikelihoodSurface(16,outBounds,upperBounds,lowerBounds);
        // restore fields
        workingTree = savedTree;
        observedStates1 = savedObservedStates1;
        observedStates2 = savedObservedStates2;
    }
    private void getLikelihoodSurface(int divisions,double[] outputBounds,double[]upperBounds,double[]lowerBounds){ //also pass upper & lower bounds
        // hold paramCount-2 parameters fixed, vary pairs through fixed bounds; just call evaluate
        // put values into surface, then write values to mesquite console
        Optimizer opt = new Optimizer(this);
        boolean savedUnassigned []= (boolean [])paramUnassigned.clone();
        double savedParams[] = (double [])params.clone();
        paramUnassigned = new boolean[params.length];       
        double[] localParams = new double[params.length];
       // for (int i=0;i<localParams.length;i++)
      //	   localParams[i] = 8.0;
        double[][] surface = new double[divisions][divisions];
        for (int j= 0;j<divisions;j++){
        		double x = (j*(upperBounds[1]-lowerBounds[1])/divisions)+lowerBounds[1];
             for (int k=0;k<divisions;k++){
                 double y = (k*(upperBounds[1]-lowerBounds[1])/divisions)+lowerBounds[1];
                 for(int l=0;l<divisions;l++){
                	    double z = (l*(upperBounds[1]-lowerBounds[1])/divisions)+lowerBounds[1];
                	    for(int m=0;m<divisions;m++) {
                	    	   double w = (m*(upperBounds[1]-lowerBounds[1])/divisions)+lowerBounds[1];
                	    	   localParams[0]=x;
                	    	   localParams[1]=y;
                	    	   localParams[2]=z;
                	    	   localParams[3]=w;
                	    	   //surface[j][k]=evaluate(localParams,null);
                	    	   Debugg.println(x +"\t" +y +"\t" + z + "\t" + w + "\t" + evaluate(localParams,null));
                	    }
                }
            }
//            localParams[2*i] = savedParams[2*i];
//            localParams[2*i+1] = savedParams[2*i+1];
        }
        if (outputBounds == null) {
            Debugg.println("No double[][] array supplied to receive output bounds or array too small");
        }
        if (outputBounds.length != 2) {
            Debugg.println("Array supplied to receive output bounds too small");
        }
        //THIS DOESN:T work at present
        //arbitrarily choose the midpoints
        if (upperBounds != null && lowerBounds != null){
            for(int i=0;i<params.length;i++){
                if (MesquiteDouble.isCombinable(upperBounds[i]) &&
                    MesquiteDouble.isCombinable(lowerBounds[i]))
                    params[i] = (upperBounds[i]+lowerBounds[i])/2;
                else
                    params[i] = 0.001;  //avoid zero in case of model pathology
                }
            }
            else {
                for(int i=0;i<params.length;i++)
                    params[i] = 0.001;
            }
            opt.optimize(params, null);
            if (upperBounds == null)
                upperBounds = opt.getUpperBoundsSearched();
            if (lowerBounds == null)
                lowerBounds = opt.getLowerBoundsSearched();
            for(int i=0;i<upperBounds.length;i++){  //assumes upperBounds.length == lowerBounds.length
                if (upperBounds[i]<0)
                    upperBounds[i] = 0;
                if (lowerBounds[i]<0)
                    lowerBounds[i] = 0;
            }
        paramUnassigned = savedUnassigned;
        this.params = savedParams;
    }
    
    public double[][] grid(double[] params,int[]freeParams,double min1, double max1, double min2, double max2, int divisions){
        double[][] g = new double[divisions][divisions];
        double[] x = new double[params.length];
        for (int i = 0;i<params.length;i++)
            if (i!=freeParams[0] && i!=freeParams[1])
                x[i]=params[i];
        for (int i = 0; i<divisions; i++){
            x[freeParams[0]] =i* (max1-min1)/divisions + min1;
            for (int j = 0; j<divisions; j++){
                x[freeParams[1]] =j* (max2-min2)/divisions + min2;
                double fcn = evaluate(x, null);
                g[i][j] = fcn;
                //System.out.println(x[0] + "\t" + x[1] + "\t" + fcn);
            }
        }
        return g;
    }

    /* All the following are useful for debugging, but shouldn't be called in released code. */
    
    public void setTree(Tree tree) {
    		this.workingTree = tree;
    }
    
    public void setObserved1(CategoricalDistribution dist1) {
    		this.observedStates1 = dist1;
    }
    
    // fix this
    public void setObserved2(CategoricalDistribution dist2) {
		this.observedStates2 = dist2;
		maxState[0] = observedStates1.getMaxState();    	
		maxState[1] = observedStates2.getMaxState();
		allStates[0] = CategoricalState.span(0,maxState[0]);
		allStates[1] = CategoricalState.span(0,maxState[1]);
    }


}   
