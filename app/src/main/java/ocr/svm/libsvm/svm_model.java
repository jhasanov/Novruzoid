//
// svm_model
//
package ocr.svm.libsvm;

public class svm_model implements java.io.Serializable
{
	public svm_parameter param;	// parameter
	public int nr_class;		// number of classes, = 2 in regression/one class svm
	public int l;			// total #SV
	public svm_node[][] SV;	// SVs (SV[l])
	public double[][] sv_coef;	// coefficients for SVs in decision functions (sv_coef[k-1][l])
	public double[] rho;		// constants in decision functions (rho[k*(k-1)/2])
	public double[] probA;         // pariwise probability information
	public double[] probB;
	public int[] sv_indices;       // sv_indices[0,...,nSV-1] are values in [1,...,num_traning_data] to indicate SVs in the training set

	// for classification only

	public int[] label;		// label of each class (label[k])
	public int[] nSV;		// number of SVs for each class (nSV[k])
				// nSV[0] + nSV[1] + ... + nSV[k-1] = l


	/* Added by Jamal
	 This array keeps the count of elements for each SV (as there's no dynamic arrays in Java).
	 Example: Let's assume, we have SV[100]255] - 100 SVs with 255 features each. But according to
	 LibSVM logic, we keep only Not Null elements. Like, if 34th SV has only 75 Not Null features,
	 the call to  SV[100][76] will raise NullPointerException. To avoid this, we use this colIdx,
	 which stores coldIdx[100] = 75. In all loops we use:
	 	for (i=0; i<colIdx[sv_seq_no); i++)
	 		instead of
	 	for (i=0; i<SV[sv_seq_no).length; i++)
	*/
	public int[] colIdx;
}
