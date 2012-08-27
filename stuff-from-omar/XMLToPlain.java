import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

import java.awt.*;

public class XMLToPlain
{

  static public void main(String[] args) throws Exception
  {
    // java XMLToPlain in.xml out.txt
    BufferedReader inFile = getReader(args[0]);
    BufferedWriter outFile = getWriter (args[1]);

    String inLine = inFile.readLine();
    String outLine = "";

    while (inLine != null) {
      outLine = inLine.trim();
      if (inLine.startsWith("<seg ") && inLine.endsWith("</seg>")) {
        int i1 = outLine.indexOf(">");
        int i2 = outLine.lastIndexOf("</seg>");
        outLine = outLine.substring(i1+1,i2);
        outLine = outLine.trim();
        writeLine(outLine,outFile);
      }
      inLine = inFile.readLine();
    }

    inFile.close();
    outFile.close();

    System.exit(0);

  } // main(String[] args)

  static public BufferedReader getReader(String inFileName) { return getReader(inFileName,"utf8"); }

  static public BufferedReader getReader(String inFileName, String format)
  {
    BufferedReader inFile = null;

    try {
      InputStream inStream = new FileInputStream(new File(inFileName));
      inFile = new BufferedReader(new InputStreamReader(inStream, format));
    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in getReader(): " + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in getReader(): " + e.getMessage());
      System.exit(99902);
    }

    return inFile;
  }

  static public BufferedWriter getWriter(String outFileName) { return getWriter(outFileName,false,"utf8"); }
  static public BufferedWriter getWriter(String outFileName, boolean append) { return getWriter(outFileName,append,"utf8"); }
  static public BufferedWriter getWriter(String outFileName, String format) { return getWriter(outFileName,false,format); }

  static public BufferedWriter getWriter(String outFileName, boolean append, String format)
  {
    BufferedWriter outFile = null;

    try {
      FileOutputStream outStream = new FileOutputStream(outFileName, append);
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, format);
      outFile = new BufferedWriter(outStreamWriter);
    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in getWriter(): " + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in getWriter(): " + e.getMessage());
      System.exit(99902);
    }

    return outFile;
  }

  static public void writeLine(String line, BufferedWriter writer)
  {
    try {
      writer.write(line, 0, line.length());
      writer.newLine();
      writer.flush();
    } catch (IOException e) {
      System.err.println("IOException in writeLine(): " + e.getMessage());
      System.exit(99902);
    }
  }

  static private void println(Object obj) { System.out.println(obj); }
  static private void print(Object obj) { System.out.print(obj); }

}
