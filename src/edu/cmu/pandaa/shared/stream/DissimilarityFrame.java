package edu.cmu.pandaa.shared.stream;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DissimilarityFrame extends FrameStream{

	double[][] dissimilarity; /*input for scaling algorithm*/
	String[] id;
	private File dissimilarityFile; /*input file generated from previous stages */
	String tokenBuffer;
	
	public DissimilarityFrame(String fileName)
	{
		dissimilarityFile = new File(fileName);
		if (this.dissimilarityFile == null) {
		      throw new IllegalStateException("file does not exist");
		}
	}
	
	public void readDissimilarityFile()
	{
		/*reads the dissimilarityFile and creates a String tokenBuffer*/
		try {
			BufferedReader br = new BufferedReader( new FileReader(dissimilarityFile));
			String strLine;
			tokenBuffer = new String();
			try {
				while ((strLine = br.readLine()) != null){
					tokenBuffer.concat(strLine);
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
	
	
	public void tokenizeDissimilarityFile()
	{
		/* parses the tokenBuffer and generates  double[][] dissimilarity*/
		
	}
	
	public void setDissimilarity(double[][] inputDissimilarity) {
	    this.dissimilarity = inputDissimilarity;
	  }

	
	/*returns double[][] dissimilarity*/
	public double[][] getDissimilarity() {
		return dissimilarity;
	}		
}
