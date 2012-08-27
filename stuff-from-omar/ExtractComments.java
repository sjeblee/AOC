import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

import java.awt.*;

public class ExtractComments
{
  static final DecimalFormat f2 = new DecimalFormat("###0.00");
  static final DecimalFormat f4 = new DecimalFormat("###0.0000");
  static final HashMap<String,String> monthArabicNameToNumber = new HashMap<String,String>();

  static final Vector<docInfo> data = new Vector<docInfo>();

  static FileOutputStream outStream_empty;
  static OutputStreamWriter outStreamWriter_empty;
  static BufferedWriter outFile_empty;

  static public void main(String[] args) throws Exception
  {
    String dirName = args[0];
    String outputFileName = args[1];

    set_monthArabicNameToNumber("monthNameToNumber.txt");

    outStream_empty = new FileOutputStream("emptyFiles.txt", false);
    outStreamWriter_empty = new OutputStreamWriter(outStream_empty, "utf8");
    outFile_empty = new BufferedWriter(outStreamWriter_empty);

    File df = new File(dirName);
    String[] fileList = df.list();

    int articleCount = 0;

    for (int i = 0; i < fileList.length; ++i) {
      String fileName = fileList[i];
      if (fileName.startsWith("alriyadh_") && fileName.contains("_comm")) { // e.g. alriyadh_520000_comm.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf("_comm"));
        println("alriyadh/" + id);
        extractComments__alriyadh(id,fullPath(dirName,fileName));
        ++articleCount;
      } else if (fileName.startsWith("alghad_")) { // e.g. alghad_500000.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf(".htm"));
        println("alghad/" + id);
        extractComments__alghad(id,fullPath(dirName,fileName));
        ++articleCount;
      } else if (fileName.startsWith("youm7_") && fileName.contains("_comm")) { // e.g. youm7_210000_comm_p1.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf("_comm"));
        String page = fileName.substring(fileName.indexOf("_p")+2,fileName.indexOf(".htm"));
        println("youm7/" + id + "/p" + page);
        extractComments__youm7(id,page,fullPath(dirName,fileName));
        if (page.equals("1")) ++articleCount;
      }
    }

    println("");

    FileOutputStream outStream = new FileOutputStream(outputFileName, false);
    OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
    BufferedWriter outFile = new BufferedWriter(outStreamWriter);

    writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>",outFile);
    writeLine("<ArabicOnlineCommentary>",outFile);

    docInfo.resetCounts();

    println("Creating xml file from " + data.size() + " docInfo objects...");
    int progress = 0;

    for (docInfo doc : data) {
      doc.incrementCounts();
      doc.print(outFile);
      ++progress;
      if (progress % 10000 == 0) { println("#"+(progress/1000)+"k"); }
      else if (progress % 1000 == 0) { print("."); }
    }

    writeLine("</ArabicOnlineCommentary>",outFile);

    if (progress % 10000 >= 1000) { println(""); }
    println("");

    outFile.close();

    outStream_empty.close();

    println("Article count: " + articleCount);
    println("Comment count: " + docInfo.docCount);
    println("Segment count: " + docInfo.segCount);
    println("Word count: " + docInfo.wordCount);
    println("Character count: " + docInfo.charCount);
    println("");

    System.exit(0);

  } // main(String[] args)


  static private void extractComments__youm7(String docid, String docpage, String fileName) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "Cp1256"));

      String line = inFile.readLine();

      int comment = 0;
      docInfo D = null;

      while (line != null) {

        line = line.replaceAll("&quot;","\"");

        while (line.lastIndexOf("<h4>") > line.lastIndexOf("</h4>")) {
          String next = inFile.readLine();
          line += next;
        }
        while (line.lastIndexOf("<p class") > line.lastIndexOf("</p>")) {
          String next = inFile.readLine();
          line += next;
        }

        if (line.contains("class=\"commentBlock\"")) {
          if (comment > 0) data.add(D);
          D = new docInfo(""); // comment number not known yet
          D.artURL = "http://www.youm7.com/News.asp?NewsID="+docid;
        } else if (line.contains("class=\"commentCount\"")) {
          comment = Integer.parseInt((line.substring(line.indexOf(">")+1,line.indexOf("</div>"))).trim());
          D.docid = "youm7_"+docid+"_comment"+numToStr(comment,3);
        } else if (line.contains("<h4>")) {
          D.subtitle = (line.substring(line.indexOf("<h4>")+4,line.indexOf("</h4>"))).trim();
        } else if (line.contains("class=\"commentAuthor\"")) {
          D.author = (line.substring(line.indexOf(":")+1,line.indexOf("</p>"))).trim();
        } else if (line.contains("class=\"commentDate\"")) {
          String infoStr = (line.substring(line.indexOf(":")+1,line.indexOf("</span>"))).trim();
          infoStr = infoStr.substring(infoStr.indexOf(" ")+1); // remove day
          // now infoStr is e.g. "18 sbtmbr  2010 - 10:11"
          //                  or "9 aktwbr  2010 - 22:49"
          String[] A = infoStr.split("\\s+");
          infoStr = A[0] + "/" + monthArabicNameToNumber.get(A[1]) + "/" + A[2];
          if (infoStr.length() == 10) {
            D.date = infoStr;
          } else { // length == 9
            D.date = "0"+infoStr;
          }
          D.time = A[4];
        } else if (line.contains("class=\"commentBody\"")) {
          line = inFile.readLine();
          line = line.replaceAll("&quot;","\"");

          while (!line.contains("</div>")) {
            line = line.trim();
            if (line.startsWith("<p>")) { line = line.substring(3); }
            if (line.endsWith("</p>")) { line = line.substring(0,line.length()-4); }
            String[] A = (line.trim()).split("<br>");
            for (int i = 0; i < A.length; ++i) {
              String sent = A[i].trim();
              if (sent.length() > 0) {
//                D.segments.add(sent.replaceAll("&quot;","\""));
                D.segments.add(sent);
              }
            }
            line = inFile.readLine();
          }
        }

        line = inFile.readLine();

      } // while (line != null)

      // add last docInfo constructed
      if (D != null) {
        data.add(D);
      } else { // no comments were found
        writeLine(fileName,outFile_empty);
      }

      inFile.close();
  }

  static private void extractComments__alghad(String docid, String fileName) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "Cp1256"));

      String line = inFile.readLine();

      Vector<String> commentLines = new Vector<String>();

      while (line != null) {

        if (line.startsWith("<br><span class=\"centerTitles\" style=\"color:#FF0000\">")) {
          // comment section has started

          int comment = 0;

          while (line != null) {

            while (line.lastIndexOf("<strong>") > line.lastIndexOf("</strong>")) {
              String next = inFile.readLine();
              line += next;
            }

            int sti = line.indexOf("<p class=\"centerText\" align=");

            if (sti >= 0) {
              while (sti >= 0) {
                ++comment;
                String first = line.substring(0,sti);
                String second = line.substring(sti+36); // 36: length of "<p class="centerText" align=justify>"

                addLine__alghad(first,commentLines);
                addLine__alghad("||| "+comment+" |||",commentLines);

                line = second;

                sti = line.indexOf("<p class=\"centerText\" align=");
              }
            }


            addLine__alghad(line,commentLines);

            if (line.endsWith("</div></td>")) { // comment section end
              break; // from inner while loop
            }

            line = inFile.readLine();
          }
        } // if (comment section start)

        line = inFile.readLine();

      } // while (line != null)

      inFile.close();

      if (commentLines.size() == 0) {
        writeLine(fileName,outFile_empty);
        return;
      }

      String last = commentLines.elementAt(commentLines.size()-1);
      if (last.endsWith("</td>")) { last = last.substring(0,last.length()-5); }
      if (last.endsWith("</div>")) { last = last.substring(0,last.length()-6); }
      if (last.endsWith("<hr>")) { last = last.substring(0,last.length()-4); }
      if (last.endsWith("</p>")) { last = last.substring(0,last.length()-4); }
      commentLines.setElementAt(last,commentLines.size()-1);

//      println(commentLines.size());
/*
      FileOutputStream outStream = new FileOutputStream("commentLines.txt", false);
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      for (String S : commentLines) {
        writeLine(S,outFile);
      }

      outFile.close();
*/

      int comment = 0;
      boolean aboutToStart = false;
      docInfo D = null;

      for (String S : commentLines) {
        if (S.startsWith("|||")) {
          if (comment > 0) data.add(D);
          ++comment;
          aboutToStart = true;
        } else if (comment > 0) {
          if (aboutToStart) {
            int strong_start_i = S.indexOf("<strong>");
            int strong_end_i = S.indexOf("</strong>");
            int br1_i = S.indexOf("<br>");
            int sup_i = S.indexOf("<sup>");
            int sup_end_i = S.indexOf("</sup>");

            String subtitle = S.substring(strong_start_i+8,strong_end_i); // 8: length of "<strong>"

            String author = S.substring(strong_end_i+9,br1_i); // 9: length of "</strong>"
            author = author.trim();
            author = author.substring(1,author.length()-1); // remove parentheses
            String location = author.split(" - ")[1]; // *** NOTE: the split string is AN ARABIC STRING ***
            author = author.split(" - ")[0]; // *** NOTE: the split string is AN ARABIC STRING ***

            String email = S.substring(br1_i+4 , sup_i-4); // +4 to skip first "<br>",
                                                           // -4 to exclude second "<br>"

            String time = S.substring(sup_i+5,sup_end_i); // +5 to skip "<sup>"
            time = time.substring(1,time.length()-1); // remove parentheses
            String[] T = time.split(" ");
            String postDate = T[0];
            String postTime = time_24h__alghad(T[1],T[2]);

            S = S.substring(sup_end_i+10); // 10: length of "</sup><br>"

            D = new docInfo("alghad_"+docid+"_comment"+numToStr(comment,3));
            D.artURL = "http://www.alghad.com/?news="+docid;
            D.date = postDate;
            D.time = postTime;
            D.subtitle = subtitle;
            D.author = author;
            D.authorEmail = email;
            D.authorLoc = location;

            aboutToStart = false;
          } // if (aboutToStart)

          D.segments.add(S);

        }

      } // for (S)

      // add last docInfo constructed
      if (D != null) {
        data.add(D);
      }

  }

  static private void addLine__alghad(String line, Vector<String> C)
  {
    if (line.endsWith("<br>")) { line = line.substring(0,line.length()-4); }
    if (line.endsWith("<br />")) { line = line.substring(0,line.length()-6); }
    if (line.endsWith("<hr>")) { line = line.substring(0,line.length()-4); }
    if (line.endsWith("</p>")) { line = line.substring(0,line.length()-4); }

    while (line.startsWith("\t")) {
      line = line.substring(1);
    }

    line = line.replaceAll("&nbsp;"," ");
    line = line.replaceAll("\\s+"," ");
    line = line.trim();

    if (line.length() > 0 && !line.startsWith("<table")) {
      C.add(line);
    }
  }

  static private String time_24h__alghad(String timeStr, String AMPM)
  {
    int hr = Integer.parseInt(timeStr.split(":")[0]);

    if (AMPM.equals("AM")) {
      if (hr != 12) {
        return timeStr;
      } else {
        return "00" + timeStr.substring(2);
      }
    } else { // PM
      if (hr == 12) {
        return timeStr;
      } else {
        return (hr+12) + timeStr.substring(2); // note: hr is guaranteed to 2 characters long
      }
    }
  }


  static private void extractComments__alriyadh(String docid, String fileName) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "utf8"));
/*
      File dfm = null;
      dfm = new File(outputDir);
      dfm.mkdir();
*/
      String line = inFile.readLine();

      int comment = 0;
      int commentSection = 0; // 0: waiting for next comment to start
                              // 1: reading comment segments
                              // 2: waiting to see author info
                              // 3: waiting to see date/time info
      docInfo D = null;

      while (line != null) {
        if (line.startsWith("<p class=\"num\">")) {
          if (comment > 0) data.add(D);
          ++comment;
          D = new docInfo("alriyadh_"+docid+"_comment"+numToStr(comment,3));
          D.artURL = "http://www.alriyadh.com/article"+docid+".html";
          commentSection = 1;
        } else if (comment > 0) {

          if (commentSection == 1) {

            if (line.endsWith("</p>")) {
              ++commentSection;
            }

            if (line.startsWith("<p class=\"body\">")) { line = line.substring(16); }
            if (line.endsWith("</p>")) { line = line.substring(0,line.length()-4); }
            if (line.endsWith("<br />")) { line = line.substring(0,line.length()-6); }
            if (line.endsWith("<br>")) { line = line.substring(0,line.length()-4); }

            line = line.replaceAll("&nbsp;"," ");
            line = line.replaceAll("\\s+"," ");
            line = line.trim();

            if (line.length() > 0) {
              D.segments.add(line);
            }

          } else if (commentSection == 2) {

            if (line.length() > 0 && !line.startsWith("<")) {
              if (line.endsWith("</font></p>")) { // visitor
                line = line.substring(0,line.indexOf("<font color="));
              } else if (line.endsWith("</a> </p>")) { // registered user with link
                line = line.substring(0,line.indexOf("</a>"));
              } else if (line.endsWith("</p>")) { // registered(?) user without link
                line = line.substring(0,line.indexOf("</p>"));
              }

              D.author = line.trim();

              ++commentSection;
            }

          } else if (commentSection == 3) {

            if (line.startsWith("<p class=\"time\">")) {
              String infoStr = line.substring(16,line.length()-4);

              String postDate = date_reverse__alriyadh(infoStr.substring(infoStr.length()-10));
              String postTime = infoStr.substring(0,5);
              if (infoStr.length() == 22) { // ends in "masa2an"
                postTime = time_24h__alriyadh(postTime,"PM");
              } else { // has length 23, ends in "saba7an"
                postTime = time_24h__alriyadh(postTime,"AM");
              }

              D.date = postDate;
              D.time = postTime;
            }

          }


        }

        line = inFile.readLine();

      } // while (line != null)

      // add last docInfo constructed
      if (D != null) {
        data.add(D);
      } else { // no comments were found
        writeLine(fileName,outFile_empty);
      }

      inFile.close();
  }

  static private String date_reverse__alriyadh(String dateStr)
  {
    String[] A = dateStr.split("/");
    return A[2]+"/"+A[1]+"/"+A[0];
  }

  static private String time_24h__alriyadh(String timeStr, String AMPM)
  {
    return time_24h__alghad(timeStr, AMPM);
  }





  static void set_monthArabicNameToNumber(String fileName) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "utf8"));

      String line = inFile.readLine();

      while (line != null) {
        String name = line;

        line = inFile.readLine();

        String number = line;

        monthArabicNameToNumber.put(name,number);

        line = inFile.readLine();

      } // while (line != null)

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



  static private void println(Object obj) { System.out.println(obj); }
  static private void print(Object obj) { System.out.print(obj); }

}

class docInfo {

  public String docid;
  public String artURL;
  public String date;
  public String time;
  public String subtitle;
  public String author;
  public String authorEmail;
  public String authorLoc;

  public Vector<String> segments;

  public int timesCounted;
  public static int charCount;
  public static int wordCount;
  public static int segCount;
  public static int docCount;

  static public void resetCounts()
  {
    charCount = 0;
    wordCount = 0;
    segCount = 0;
    docCount = 0;
  }

  public docInfo(String id)
  {
    docid = id;
    artURL = null;
    date = null;
    time = null;
    subtitle = null;
    author = null;
    authorEmail = null;
    authorLoc = null;

    segments = new Vector<String>();

    timesCounted = 0;
  }

  public void incrementCounts()
  {
    docCount += 1;
    segCount += segments.size();

    for (String seg : segments) {
      wordCount += seg.split("\\s+").length;
      charCount += seg.length();
    }

    ++timesCounted;

  }

  public void print(BufferedWriter outFile) throws Exception
  {
    String header = "";
    header += "<doc";
    header += " docid=\"" + docid + "\"";
    if (artURL != null) header += " articleURL=\"" + artURL + "\"";
    if (date != null) header += " date=\"" + date + "\"";
    if (time != null) header += " time=\"" + time + "\"";
    if (subtitle != null) header += " subtitle=\"" + subtitle + "\"";
    if (author != null) header += " author=\"" + author + "\"";
    if (authorEmail != null) header += " authorEmail=\"" + authorEmail + "\"";
    if (authorLoc != null) header += " authorLocation=\"" + authorLoc + "\"";
    header += ">";

    writeLine(header,outFile);

    int i = 0;
    for (String seg : segments) {
      ++i;
      writeLine("<seg id=\""+i+"\"> "+seg.replaceAll("\\s+"," ")+" </seg>",outFile);
    }

    writeLine("</doc>",outFile);

  }

  static private void writeLine(String line, BufferedWriter writer) throws IOException
  {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

}

