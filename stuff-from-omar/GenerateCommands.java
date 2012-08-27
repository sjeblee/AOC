import java.util.*;
import java.io.*;

public class GenerateCommands
{
  static public void main(String[] args) throws Exception
  {
    if (args.length < 0) {
      println("Wrong usage.");
      System.exit(1);
    }

    String source = args[0]; // e.g. alghad
    int minIndex = Integer.parseInt(args[1]);
    int maxIndex = Integer.parseInt(args[2]);
    String outFileName = args[3];

      FileOutputStream outStream = new FileOutputStream(outFileName, false);
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      String outLine = "";

      for (int i = minIndex; i <= maxIndex; ++i) {
        outLine = "wget --limit-rate=100k --spider -nv " + "\"" + url(source,i) + "\"" + " -a wget.log." + source;
        writeLine(outLine,outFile);
//        outLine = "mv " + existingFileName(source,i) + " " + newFileName(source,i);
//        writeLine(outLine,outFile);
      }

    System.exit(0);

  } // main(String[] args)

  static private String url(String src, int index)
  {
    if (src.equals("alriyadh")) {
      return "http://www.alriyadh.com/newspaper/comments/"+index+"/1";
    }
    if (src.equals("alghad")) {
      return "http://www.alghad.com/?news="+index;
    }
    if (src.equals("youm7")) {
      return "http://www.youm7.com/Includes/NewsComments.asp?NewsID="+index+"&page=1";
    }

    return "";

  }

  static private String existingFileName(String src, int index)
  {
    if (src.equals("alriyadh")) {
      return "1";
    }
    if (src.equals("alghad")) {
      return "index.html?news="+index;
    }
    if (src.equals("youm7")) {
      return "NewsComments.asp?NewsID="+index+"&page=1";
    }

    return "";
  }

  static private String newFileName(String src, int index)
  {
    if (src.equals("alriyadh")) {
      return "alriyadh_"+index+".htm";
    }
    if (src.equals("alghad")) {
      return "alghad_"+index+".htm";
    }
    if (src.equals("youm7")) {
      int page = 1;
      return "youm7_"+index+"_p"+page+".htm";
    }

    return "";
  }

  static private String numToStr(int x,int length)
  {
    if (length == 2) { return numToStr_2(x); }
    else if (length == 3) { return numToStr_3(x); }
    else if (length == 4) { return numToStr_4(x); }
    else return "" + x;
  }

  static private String numToStr_2(int x)
  {
    if (x < 10) return "0" + x;
    else return "" + x;
  }

  static private String numToStr_3(int x)
  {
    if (x < 10) return "00" + x;
    else if (x < 100) return "0" + x;
    else return "" + x;
  }

  static private String numToStr_4(int x)
  {
    if (x < 10) return "000" + x;
    else if (x < 100) return "00" + x;
    else if (x < 1000) return "0" + x;
    else return "" + x;
  }


  static private void writeLine(String line, BufferedWriter writer) throws IOException
  {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

  static private String fullPath(String dir, String fileName)
  {
    File dummyFile = new File(dir,fileName);
    return dummyFile.getAbsolutePath();
  }

  static private boolean fileExists(String fileName)
  {
    if (fileName == null) return false;
    File checker = new File(fileName);
    return checker.exists();
  }



  static private void println(Object obj) { System.out.println(obj); }
  static private void print(Object obj) { System.out.print(obj); }

}
