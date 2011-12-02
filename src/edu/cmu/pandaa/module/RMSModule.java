package edu.cmu.pandaa.module;

import edu.cmu.pandaa.stream.GeometryFileStream;
import edu.cmu.pandaa.stream.RMSFileStream;

public class RMSModule {
	
	public static void main(String[] args) throws Exception
	{
		int arg = 0;
	    String outArg = args[arg++];
	    String inArg1 = args[arg++];
	    String inArg2 = args[arg++];
	    if (args.length > arg || args.length < arg) {
	      throw new IllegalArgumentException("Invalid number of arguments");
	    }
		
	    System.out.println("RMS: " + outArg + " " + inArg1 + " " + inArg2);
	    
	    RMSFileStream rIn1 = new RMSFileStream(inArg1);
	    RMSFileStream rIn2 = new RMSFileStream(inArg2);
	    RMSFileStream rOut = new RMSFileStream(outArg, true);

	    
	}

}
