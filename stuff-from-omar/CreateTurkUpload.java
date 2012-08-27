import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CreateTurkUpload
{
  public static Font myFont = new Font("Times New Roman", Font.PLAIN, 18);
  public static FontRenderContext frc = new FontRenderContext(null, true, true);

  static Random generator = new Random(2010);

  static final DecimalFormat f2 = new DecimalFormat("###0.00");
  static final DecimalFormat f4 = new DecimalFormat("###0.0000");

  static public void main(String[] args) throws Exception
  {

// Usage:
//   java CreateTurkUpload source1comments.xml source1article.xml range1 source2comments.xml source2article.xml range 2 ...
// e.g.:
//   java CreateTurkUpload alghad AOC_alghad_comments.xml AOC_alghad_articles.xml 500000 510000 alriyadh AOC_alriyadh_comments.xml AOC_alriyadh_articles.xml 520000 522500 youm7 AOC_youm7_comments.xml AOC_youm7_articles.xml 210000 213000
//                            0             1                       2                3      4       5                6                         7                8      9
// (for the samples):
//   java CreateTurkUpload alghad AOC_alghad-sample_comments.xml AOC_alghad-sample_articles.xml 500000 510000 alriyadh AOC_alriyadh-sample_comments.xml AOC_alriyadh-sample_articles.xml 520000 522500 youm7 AOC_youm7-sample_comments.xml AOC_youm7-sample_articles.xml 210000 213000

    if (args.length % 5 != 0) {
      println("Wrong usage.");
      System.exit(1);
    }

    int numSources = args.length / 5;

    String[] sourceNames = new String[1+numSources];

//    BufferedReader inFile_comments;
//    BufferedReader inFile_articles;
//    int[] rangeLow = new int[1+numSources];
//    int[] rangeHigh = new int[1+numSources];

    Vector<String> commentSegments = new Vector<String>();
    Vector<int[]> commentSegments_info = new Vector<int[]>(); // stores {s,doc_i,comm_i,seg_i} for each comment segment
    Vector<String> articleSegments = new Vector<String>();
    Vector<int[]> articleSegments_info = new Vector<int[]>(); // stores {s,doc_i,1,seg_i} for each article segment (1 is a dummy value)

    TreeSet<Integer>[] docsWithComments = new TreeSet[1+numSources];

    int tot_articleCount = 0;
    int tot_articleSegmentCount = 0;
    int tot_articleWordCount = 0;
    int tot_commentCount = 0;
    int tot_commentSegmentCount = 0;
    int tot_commentWordCount = 0;

    for (int s = 1; s <= numSources; ++s) {
      sourceNames[s] = args[-5+(5*s)]; // 0,5,...
      BufferedReader inFile_comments = getReader(args[-4+(5*s)]); // 1,6,...
      BufferedReader inFile_articles = getReader(args[-3+(5*s)]); // 2,7,...
      int rangeLow = Integer.parseInt(args[-2+(5*s)]); // 3,8,...
      int rangeHigh = Integer.parseInt(args[-1+(5*s)]); // 4,9,...

      docsWithComments[s] = new TreeSet<Integer>();

      int commentCount = 0;
      int commentSegmentCount = 0;
      int commentWordCount = 0;
      int articleCount = 0;
      int articleSegmentCount = 0;
      int articleWordCount = 0;

      String line;
      boolean match;

      // PROCESS COMMENTS
      line = inFile_comments.readLine();
      match = false;

      while (line != null) {

        if (line.startsWith("<doc")) {
          String docid = docidFromDocInfoLine(line); // e.g. alghad_500019_comment001
          int docNumber = docNumberFromDocID(docid); // e.g. 500019
          int commNumber = commNumberFromDocID(docid); // e.g. 1

          if (docNumber >= rangeLow && docNumber <= rangeHigh) {
            ++commentCount;
            match = true;
            docsWithComments[s].add(docNumber);

            line = inFile_comments.readLine();
            while (!line.equals("</doc>")) {
              if (line.startsWith("<seg")) {
                int segNumber = segNumberFromSegLine(line); // e.g. 1,2,...
                String sentence = sentenceFromSegLine(line);
                if (sentence.length() >= 5) {
                  int[] info = {s , docNumber , commNumber , segNumber};
                  commentSegments.add(sentence);
                  commentSegments_info.add(info);
                  ++commentSegmentCount;
                  commentWordCount += sentence.split("\\s+").length;
                }
              }
              line = inFile_comments.readLine();
            } // while (line != </doc>)

          } // if (docNumber is in range)

        } // if (line starts with <doc)

        if (!match) { line = inFile_comments.readLine(); }
        else { match = false; }

      } // while (line != null)


      // PROCESS ARTICLES
      line = inFile_articles.readLine();
      match = false;

      while (line != null) {

        if (line.startsWith("<doc")) {
          String docid = docidFromDocInfoLine(line); // e.g. alghad_500019_comment001
          int docNumber = docNumberFromDocID(docid); // e.g. 500019

          if (docsWithComments[s].contains(docNumber)) {
            match = true;

            boolean skip = false; // used to skip <STRONG>, <author>, and <newsStoryEditor>
            line = inFile_articles.readLine();
            while (!line.equals("</doc>")) {
              if (line.startsWith("<seg")) {
                if (!skip) {
                  int segNumber = segNumberFromSegLine(line); // e.g. 1,2,...
                  String sentence = sentenceFromSegLine(line);
                  if (sentence.length() >= 5) {
                    int[] info = {s , docNumber , 1 , segNumber}; // the 1 is a dummy value
                    articleSegments.add(sentence);
                    articleSegments_info.add(info);
                    ++articleSegmentCount;
                    articleWordCount += sentence.split("\\s+").length;
                  }
                }
              } else if (line.equals("<STRONG>") || line.equals("<author>") || line.equals("<newsStoryEditor>")) {
                skip = true;
              } else if (line.equals("</STRONG>") || line.equals("</author>") || line.equals("</newsStoryEditor>")) {
                skip = false;
              }
              line = inFile_articles.readLine();
            } // while (line != </doc>)

          } // if (docNumber is for an article with comments)

        } // if (line starts with <doc)

        if (!match) { line = inFile_articles.readLine(); }
        else { match = false; }

      } // while (line != null)

      inFile_comments.close();
      inFile_articles.close();

      articleCount = docsWithComments[s].size();


      println("Source #" + s + " (" + sourceNames[s] + "):");
      println("  # articles:      " + articleCount);
      println("  # article segs:  " + articleSegmentCount);
      println("  # article words: " + articleWordCount);
      println("  # comments:      " + commentCount);
      println("  # comment segs:  " + commentSegmentCount);
      println("  # comment words: " + commentWordCount);
      println("");
      tot_articleCount += articleCount;
      tot_articleSegmentCount += articleSegmentCount;
      tot_articleWordCount += articleWordCount;
      tot_commentCount += commentCount;
      tot_commentSegmentCount += commentSegmentCount;
      tot_commentWordCount += commentWordCount;

    } // for (s)

    println("ALL SOURCES:");
    println("  # articles:      " + tot_articleCount);
    println("  # article segs:  " + tot_articleSegmentCount);
    println("  # article words: " + tot_articleWordCount);
    println("  # comments:      " + tot_commentCount);
    println("  # comment segs:  " + tot_commentSegmentCount);
    println("  # comment words: " + tot_commentWordCount);
    println("");



    int batchNumber = 2;
    int commentSegsPerHIT = 10;
    int articleSegsPerHIT = 2;
    int segsPerHIT = commentSegsPerHIT + articleSegsPerHIT;

    println(  "Creating MTurk upload file; "
            + Math.min(tot_commentSegmentCount/commentSegsPerHIT,tot_articleSegmentCount/articleSegsPerHIT)
            + " HITs expected.");
    println("Start time: " + new Date());

    String[] commentSegments_a = (commentSegments.toArray(new String[0]));
    String[] articleSegments_a = (articleSegments.toArray(new String[0]));

    Vector<Integer> V_c = new Vector<Integer>();
    for (int i = 0; i < tot_commentSegmentCount; ++i) { V_c.add(i); }
    Collections.shuffle(V_c,generator);

    Vector<Integer> V_a = new Vector<Integer>();
    for (int i = 0; i < tot_articleSegmentCount; ++i) { V_a.add(i); }
    Collections.shuffle(V_a,generator);

    createImgDirs(sourceNames);

    int V_c_ndx = 0; // index in V_c of the number that will be used as an index in commentSegments_a
    int V_a_ndx = 0; // index in V_a of the number that will be used as an index in articleSegments_a
    int currHIT = 0; // will eventually be the number of HITs created

    BufferedWriter outFile = getWriter("dialect_batch"+batchNumber+".input");
    String outLine = "";

    outLine += "\t" + "\"HIT_info\"";
    for (int s = 1; s <= segsPerHIT; ++s) {
      outLine += "\t" + "\"src" + s + "\""; // e.g. alghad_comments
      outLine += "\t" + "\"doc" + s + "\""; // e.g. 500062
      outLine += "\t" + "\"prt" + s + "\""; // e.g. 027 -- this will be the comment number of the source type is "comments" and 001 if the source type is "articles"
      outLine += "\t" + "\"seg" + s + "\""; // e.g. 002
    }
    writeLine(outLine.substring(1),outFile);

    while (true) {
      ++currHIT;

      Vector<String> segType = new Vector<String>(); // either "comment" or "article"
      for (int i = 1; i <= commentSegsPerHIT; ++i) segType.add("comments");
      for (int i = 1; i <= articleSegsPerHIT; ++i) segType.add("articles");
      Collections.shuffle(segType,generator);

      outLine = "";

      outLine += "\t" + "\"DialectClassification#batch:"+batchNumber+"#HIT:" + currHIT + "\"";

      for (int seg = 1; seg <= segsPerHIT; ++seg) {
        String segStr;
        int[] segInfo;
        if (segType.elementAt(seg-1).equals("comments")) {
          int rand_ndx = V_c.elementAt(V_c_ndx);
          ++V_c_ndx;
          segStr = commentSegments.elementAt(rand_ndx);
          segInfo = commentSegments_info.elementAt(rand_ndx);
        } else {
          int rand_ndx = V_a.elementAt(V_a_ndx);
          ++V_a_ndx;
          segStr = articleSegments.elementAt(rand_ndx);
          segInfo = articleSegments_info.elementAt(rand_ndx);
        }

        String source = sourceNames[segInfo[0]] + "_" + segType.elementAt(seg-1).charAt(0);
        int docNumber = segInfo[1];
        int partNumber = segInfo[2]; // for comments, the comment number; for articles, this is just 1
        int segNumber = segInfo[3]; // i.e. segNumber within the block of segments in which it appears online

        String imgFileName = fullPath("imgs/"+source,docNumber+"_"+numToStr(partNumber,3)+"_"+numToStr(segNumber,3)+".png");
        if (!fileExists(imgFileName)) {
          str2img(segStr, 445, 1.5f, imgFileName, false);
        }

        outLine += "\t" + "\"" + source + "\"";
        outLine += "\t" + "\"" + docNumber + "\"";
        outLine += "\t" + "\"" + numToStr(partNumber,3) + "\"";
        outLine += "\t" + "\"" + numToStr(segNumber,3) + "\"";

      } // for (seg)

      writeLine(outLine.substring(1),outFile);


      if (currHIT % 1000 == 0) println("+ " + (currHIT/1000) + "k HITs");
      else if (currHIT % 500 == 0) print("+");
      else if (currHIT % 100 == 0) print(".");


      if (V_c_ndx + commentSegsPerHIT > tot_commentSegmentCount) { break; }
      if (V_a_ndx + articleSegsPerHIT > tot_articleSegmentCount) { break; }

    } // while (true)

    println("");
    if (currHIT % 1000 >= 100) println("");

    println("End time:   " + new Date());

    outFile.close();

    System.exit(0);

  } // main(String[] args)

  static private void createImgDirs(String[] sources) throws Exception
  {

      createDir("imgs");

      for (int s = 0; s < sources.length; ++s) {
        if (sources[s] != null && sources[s].length() > 0) {
          String src = sources[s];
          createDir("imgs/"+src+"_c");
          createDir("imgs/"+src+"_a");
        }
      }

  }

  static private void createDir(String dirName) throws Exception
  {
      File dfm = new File(dirName);
      dfm.mkdir();
  }

  static private String docidFromDocInfoLine(String docInfoLine)
  {
    int docidf_i = docInfoLine.indexOf("docid");
    int docid_i1 = docInfoLine.indexOf("\"",docidf_i);
    int docid_i2 = docInfoLine.indexOf("\"",docid_i1+1);
    return docInfoLine.substring(docid_i1+1,docid_i2).trim(); // e.g. alghad_500019_comment001
  }

  static private int docNumberFromDocID(String docid)
  {
    int docNumber_i1 = docid.indexOf("_");
    int docNumber_i2 = docid.indexOf("_",docNumber_i1+1);
    String docNumber = docid.substring(docNumber_i1+1,docNumber_i2);
    docNumber = docNumber.replaceAll("net",""); // for alriyadh.com "Net" articles
    return Integer.parseInt(docNumber); // e.g. 500019
  }

  static private int commNumberFromDocID(String docid)
  {
    int c_i = docid.indexOf("comment");
    String comment = docid.substring(c_i+7);
    return Integer.parseInt(comment);
  }

  static private int segNumberFromSegLine(String segLine)
  {
    int id_i1 = segLine.indexOf("id=");
    int id_i2 = segLine.indexOf(">");
    String segN = segLine.substring(id_i1+3,id_i2);
    if (segN.startsWith("\"") && segN.endsWith("\"")) {
      segN = segN.substring(1,segN.length()-1);
    }
    return Integer.parseInt(segN);
  }

  static private String sentenceFromSegLine(String segLine)
  {
    String sentence = segLine.substring(segLine.indexOf(">")+1,segLine.indexOf("</seg>"));
    sentence = sentence.trim();
    return sentence;
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

  static private void str2img(String str, int desiredWidth, float lineSpacing, String outFileName, boolean leftAlign)
  {
    try {

      Vector<String> A = null;
      if (leftAlign) {
        A = splitStr(str,desiredWidth);
      } else {
        A = splitStr(str,desiredWidth-11); // -11: left-most letter sometimes crosses border(!)
      }

//      Rectangle2D bounds = myFont.getStringBounds(str, frc);
//      println(bounds.getY()); // the 16.42; depends on font type and size
      int w = desiredWidth;
      int h = (int)(((A.size() + 0.67)*16.42f)*lineSpacing);

      BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
      Graphics2D g = image.createGraphics();
      g.setColor(Color.WHITE); // background color
//      g.setColor(new Color(240,240,255)); // background color - very light blue

      g.fillRect(0, 0, w, h);
      g.setColor(Color.BLACK); // text color
//      g.setColor(new Color(220,20,60)); // text color - crimson
//      g.setColor(new Color(0,128,0)); // text color - green
//      g.setColor(new Color(0,0,128)); // text color - navy
      g.setFont(myFont);

      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
      FontMetrics fm = g.getFontMetrics(myFont);

      for (int i = 1; i <= A.size(); ++i) {
        String s = A.elementAt(i-1);

        if (leftAlign) {
          g.drawString(s, 0.0f, lineSpacing*i*16.42f); // x = 0
        } else {
          Rectangle2D bounds = fm.getStringBounds(s, g);
          int textWidth  = (int)(bounds.getWidth());
          g.drawString(s, desiredWidth - textWidth, lineSpacing*i*16.42f); // x > 0, especially for last line
        }
      }

      g.dispose();

      File file = new File(outFileName);
      ImageIO.write(image,"png",file);

    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in str2img(...): " + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in str2img(...): " + e.getMessage());
      System.exit(99902);
    }
  }

  static private Vector<String> splitStr(String str, int width)
  {
    Vector<String> subStrs = new Vector<String>();
    String[] words = str.split(" ");    

    int i = 0; // number of words processed

    while (i < words.length) {
      String nextStr = words[i];
      String nextStrCand = words[i];
      ++i;
      int currWidth = (int)myFont.getStringBounds(nextStr,frc).getWidth();

      while (i < words.length) {
        nextStrCand += " " + words[i];
        int candWidth = (int)myFont.getStringBounds(nextStrCand,frc).getWidth();
        if (candWidth <= width) {
          nextStr = nextStrCand;
          currWidth = candWidth;
          ++i;
        } else {
          break;
        }
      }

      subStrs.add(nextStr);

    }

    return subStrs;
  }



  static private int randInt(int strt, int fnsh)
  {
    int retVal = generator.nextInt((fnsh-strt) + 1); // gives value in [0,fnsh-strt]
    return retVal + strt; // returns value in [strt,fnsh]
  }


  static private BufferedReader getReader(String inFileName) { return getReader(inFileName,"utf8"); }

  static private BufferedReader getReader(String inFileName, String format)
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

  static private BufferedWriter getWriter(String outFileName) { return getWriter(outFileName,false,"utf8"); }
  static private BufferedWriter getWriter(String outFileName, boolean append) { return getWriter(outFileName,append,"utf8"); }
  static private BufferedWriter getWriter(String outFileName, String format) { return getWriter(outFileName,false,format); }

  static private BufferedWriter getWriter(String outFileName, boolean append, String format)
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

  static private int countLines(String fileName)
  {
    int count = 0;

    try {
      BufferedReader inFile = new BufferedReader(new FileReader(fileName));

      String line;
      do {
        line = inFile.readLine();
        if (line != null) ++count;
      }  while (line != null);

      inFile.close();
    } catch (IOException e) {
      System.err.println("IOException in countLines(String): " + e.getMessage());
      System.exit(99902);
    }

    return count;
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




  static private void writeLine(String line, BufferedWriter writer)
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
