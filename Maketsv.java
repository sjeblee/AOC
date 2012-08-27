/** Maketsv.java
* Makes a tsv file of all the webcrawl stats
*/

import java.util.Scanner;
import java.util.StringTokenizer;
import java.io.*;

public class Maketsv{

public static void main(String[] args){

	if(args.length < 1){
		System.out.println("Usage: java Maketsv filelist");
		System.exit(1);
	}

	String filename = args[0];

	try{
		Scanner filelist = new Scanner(new FileReader(filename));
		FileWriter outfile = new FileWriter("webcrawl-stats.tsv");
		outfile.write("website\tMSA lines\tMSA percent\tEGY lines\tEGY percent\t" 
				+ "GLF lines\tGLF percent\tLEV lines\tLEV percent\tCountry of origin\n");
		while(filelist.hasNextLine()){
			String statfile = filelist.nextLine();
			Scanner infile = new Scanner(new FileReader(statfile));
			int counter = 0;
			while(infile.hasNextLine()){
				String line = infile.nextLine();
				StringTokenizer tok = new StringTokenizer(line);
				if(counter==0){
					outfile.write(tok.nextToken() + "\t");
				}
				if((counter>0) && (counter<5)){
					outfile.write(tok.nextToken() + "\t");
					tok.nextToken();
					outfile.write(tok.nextToken() + "\t");
				}
				if(counter==5){
					tok.nextToken(); //Country
					tok.nextToken(); //of
					tok.nextToken(); //origin:
					while(tok.hasMoreTokens())
						outfile.write(tok.nextToken() + " ");
				}
				counter++;
			}//end while infile
			outfile.write("\n");
			infile.close();	
		}//end while filelist

		outfile.close();
		filelist.close();

	}//end try
	catch(IOException e){
		System.out.println(e.getMessage());
		System.exit(1);
	}

}//end main

}
