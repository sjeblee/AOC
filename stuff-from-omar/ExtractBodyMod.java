import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

import java.awt.*;

public class ExtractBody
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

    int fileCount = 0;

    for (int i = 0; i < fileList.length; ++i) {
      String fileName = fileList[i];
      if (fileName.startsWith("alriyadh_") && fileName.contains(".html") && fileName.contains("net_art")) { // e.g. alriyadh_520116net_art.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf("net_art"));
        println("alriyadh/" + id + " (net)");
        extractBody__alriyadh(id,fullPath(dirName,fileName),true); // true: isNetArticle
        ++fileCount;
      } else if (fileName.startsWith("alriyadh_") && fileName.contains(".html") && fileName.contains("_art")) { // e.g. alriyadh_520000_art.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf("_art"));
        println("alriyadh/" + id);
        extractBody__alriyadh(id,fullPath(dirName,fileName),false); // false: isNetArticle
        ++fileCount;
      } else if (fileName.startsWith("alghad_") && fileName.contains(".htm")) { // e.g. alghad_500000.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf(".htm"));
        println("alghad/" + id);
        extractBody__alghad(id,fullPath(dirName,fileName));
        ++fileCount;
      } else if (fileName.startsWith("youm7_") && fileName.contains(".htm") && fileName.contains("_art")) { // e.g. youm7_210000_art.htm
        String id = fileName.substring(fileName.indexOf("_")+1,fileName.indexOf("_art"));
        println("youm7/" + id);
        extractBody__youm7(id,fullPath(dirName,fileName));
        ++fileCount;
      }
    }

    println("");

    FileOutputStream outStream = new FileOutputStream(outputFileName, false);
    OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
    BufferedWriter outFile = new BufferedWriter(outStreamWriter);

    writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>",outFile);
    writeLine("<ArabicOnlineNews>",outFile);

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

    writeLine("</ArabicOnlineNews>",outFile);

    if (progress % 10000 >= 1000) { println(""); }
    println("");

    outFile.close();

    outStream_empty.close();

    println("File count: " + fileCount);
    println("Article count (excludes empty files): " + docInfo.artCount);
    println("Segment count: " + docInfo.segCount);
    println("Word count: " + docInfo.wordCount);
    println("Character count: " + docInfo.charCount);
    println("");

    System.exit(0);

  } // main(String[] args)


  static private void extractBody__alghad(String docid, String fileName) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "Cp1256"));

      String line = inFile.readLine();

      docInfo D = new docInfo("alghad_"+docid+"_article");
      D.artURL = "http://www.alghad.com/?news="+docid;

      String articleStartStr = "<!-- ***body -->";
      String articleEndStr = "<div class=\"clear\"></div>";

      while (line != null && !line.contains(articleStartStr)) {
        if (line.contains("<title>")) {
          int i1 = line.indexOf("<title>");
          int i2 = line.indexOf("</title>");
          line = line.substring(i1+7,i2);
          line = line.trim();
          line = line.replaceAll("&nbsp;"," ");
          line = line.replaceAll("\\s+"," ");
          line = line.trim();
          D.htmlTitle = line;
        } else if (line.contains("GMT+2")) {
          int g = line.indexOf("GMT+2");
          int s1 = line.lastIndexOf("<sup>",g);
          int s2 = line.indexOf("</sup>",g);
          line = line.substring(s1,s2+6);
          line = line.replaceAll("&nbsp;"," ");

          // line:
          //   <sup>[d]d/[m]m/yyyy alsa3t .<span...">GMT+2</span> ) hh:mm x.m ) </sup>
          // e.g.: (500000)
          //   <sup>22/4/2010 alsa3t .<span sty...">GMT+2</span> ) 00:54 a.m ) </sup>
          // e.g.: (530001)
          //   <sup>13/9/2010 alsa3t .<span sty...">GMT+2</span> ) 23:07 p.m ) </sup>
          // e.g.: (510001)
          //   <sup>7/6/2010 alsa3t .<span sty...">GMT+2</span> ) 00:12 a.m ) </sup>

          String[] A = line.split("\\s+");
          String dateStr = A[0].substring(A[0].indexOf(">")+1);
          dateStr = fix_dateStr__alghad(dateStr);
          D.date = dateStr;
          D.time = A[5];
        }

        line = inFile.readLine();
      }

      while (line != null) {

        if (line.contains(articleEndStr)) break;

        line = line.trim();

        line = line.replaceAll("<P align=justify>","<p>");
        line = line.replaceAll("<P align=left>","<p>");
        line = line.replaceAll("<P align=right>","<p>");
        line = line.replaceAll("<P dir=rtl align=justify>","<p>");
        line = line.replaceAll("<P>","<p>");
        line = line.replaceAll("</P>","</p>");

        if (line.contains("<p>")) {

          while (!line.contains("</p>")) {
            line += " " + inFile.readLine();
            line = line.replaceAll("<P align=justify>","<p>");
            line = line.replaceAll("<P align=left>","<p>");
            line = line.replaceAll("<P align=right>","<p>");
            line = line.replaceAll("<P dir=rtl align=justify>","<p>");
            line = line.replaceAll("<P>","<p>");
            line = line.replaceAll("</P>","</p>");
          }

          line = line.replaceAll("&nbsp;"," ");
          line = line.replaceAll("\\s+"," ");

          int i1 = line.indexOf("<p>");
          int i2 = line.indexOf("</p>");
          line = line.substring(i1+3,i2);

          while (line.contains("<STRONG>")) {
            int s1 = line.indexOf("<STRONG>");
            int s2 = line.indexOf("</STRONG>",s1);
            int s1p = line.lastIndexOf("<STRONG>",s2);
            if (s1p > s1) s1 = s1p;
            if (s2 < s1) s2 = line.length();

            String strongStr = line.substring(s1+8,s2).trim();
            strongStr = cleanLine__alghad(strongStr);
            if (strongStr.length() > 0) {
              D.segments.add(strongStr);
              D.formatting.add("<STRONG>|||</STRONG>");
            }

            line = line.substring(0,s1) + line.substring(Math.min(line.length(),s2+9));
          }

          line = cleanLine__alghad(line);

          if (line.length() > 0) {
            D.segments.add(line);
            D.formatting.add("");
          }

        }

        line = inFile.readLine();

      }

      if (D.segments.size() > 0) {
        data.add(D);
      } else { // no comments were found
        writeLine(fileName,outFile_empty);
      }

  }

  static String fix_dateStr__alghad(String dateStr)
  {
    String[] A = dateStr.split("/");

    String d = A[0];
    String m = A[1];
    String y = A[2];

    if (d.length() == 1) d = "0" + d;
    if (m.length() == 1) m = "0" + m;

    return d + "/" + m + "/" + y;
  }

  static private String cleanLine__alghad(String line) throws Exception
  {
    String S = line;

    S = S.replaceAll("<A ","<a ");
    S = S.replaceAll(" HREF="," href=");
    S = S.replaceAll("</A>","</a>");
    while (S.contains("<a href")) {
      int a1 = S.indexOf("<a href");
      int a2 = S.indexOf(">",a1);
      S = S.substring(0,a1) + S.substring(a2+1);
    }
    S = S.replaceAll("</a>"," ");

    S = S.replaceAll("<FONT>","");
    while (S.contains("<FONT ")) {
      int f1 = S.indexOf("<FONT ");
      int f2 = S.indexOf(">",f1);
      S = S.substring(0,f1) + S.substring(f2+1);
    }
    S = S.replaceAll("</FONT>","");

    S = S.replaceAll("<SPAN>","");
    while (S.contains("<SPAN ")) {
      int s1 = S.indexOf("<SPAN ");
      int s2 = S.indexOf(">",s1);
      S = S.substring(0,s1) + S.substring(s2+1);
    }
    S = S.replaceAll("</SPAN>","");

    while (S.contains("<IMG ")) {
      int im1 = S.indexOf("<IMG ");
      int im2 = S.indexOf(">",im1);
      S = S.substring(0,im1) + S.substring(im2+1);
    }

    while (S.contains("<div ")){
	int im1 = S.indexOf("<div ");
      int im2 = S.indexOf(">",im1);
      S = S.substring(0,im1) + S.substring(im2+1);
	}

    S = S.replaceAll("<p>"," ");
    S = S.replaceAll("</p>"," ");
    S = S.replaceAll("<P>"," ");
    S = S.replaceAll("</P>"," ");
    S = S.replaceAll("<br>"," ");
    S = S.replaceAll("<br/>"," ");
    S = S.replaceAll("</br>"," ");
    S = S.replaceAll("<BR>"," ");
    S = S.replaceAll("<BR/>"," ");
    S = S.replaceAll("</BR>"," ");

    S = S.replaceAll("<U>","");
    S = S.replaceAll("</U>","");

    S = S.replaceAll("</STRONG>","");
    S = S.replaceAll("\\s+"," ");
    S = S.trim();
    return S;
  }

  static private void extractBody__youm7(String docid, String fileName) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "Cp1256"));

      String line = inFile.readLine();

      docInfo D = new docInfo("youm7_"+docid+"_article");
      D.artURL = "http://www.youm7.com/News.asp?NewsID="+docid;

      String articleStartStr = "<!-- end sport advertise -->";
      String articleEndStr = "<!-- sport ads website -->";

      while (line != null && !line.contains(articleStartStr)) {

        line = line.trim();

        if (line.contains("<title>")) {

          // title is split over more than one line
          while (!line.contains("</title>")) {
            line += " " + inFile.readLine();
          }
          int i1 = line.indexOf("<title>");
          int i2 = line.indexOf("</title>");
          if (i1 < i2) line = line.substring(i1+7,i2);
          line = line.replaceAll("&nbsp;"," ");
          line = line.replaceAll("\\s+"," ");
          line = line.trim();
          D.htmlTitle = line;

        } else if (line.startsWith("<h3>") || line.startsWith("<h2>")) {

          char headlineSize = line.charAt(2);
          // deal with cases when headline is split over more than one line
          while (!line.contains("</h"+headlineSize+">")) {
            line += " " + inFile.readLine();
          }

          line = line.substring(4,line.indexOf("</h"+headlineSize+">")); // remove <hx> and </hx>
          line = line.trim();
          if (line.length() > 0) {
            D.segments.add(line);
            D.formatting.add("<h" + headlineSize + ">|||</h" + headlineSize + ">");
          }

        } else if (line.contains("<p class=\"newsStoryDate\">")) {

          int i1 = line.indexOf("StoryDate\">");
          int i2 = line.indexOf("</p>");
          line = line.substring(i1+11,i2);
          line = line.trim();

          // line:
          //   weekDay, day monthName  year - hh:mm
          // e.g.: (250000)
          //   alETnyn, 5 yolyo  2010 - 15:50

          String[] A = line.split("\\s+");
          String dateStr = A[1] + "/" + monthArabicNameToNumber.get(A[2]) + "/" + A[3];
          if (dateStr.length() == 10) {
            D.date = dateStr;
          } else { // length == 9
            D.date = "0"+dateStr;
          }

          D.time = A[5];

        } else if (line.contains("<p class=\"newsStoryEditor\">")) {

          // deal with (rare) cases when <p> tag is split over more than one line
          while (!line.contains("</p>")) {
            line += " " + inFile.readLine();
          }

          int i1 = line.indexOf("StoryEditor\">");
          int i2 = line.indexOf("</p>");
          line = line.substring(i1+13,i2);
          line = line.trim();
          if (line.length() > 0) {
            D.segments.add(line);
            D.formatting.add("<newsStoryEditor>|||</newsStoryEditor>");
          }

        }

        line = inFile.readLine();

      }

      if (D.date.endsWith("1899")) D.segments.clear();

      while (line != null) {

        if (line.contains(articleEndStr)) break;

        line = line.trim();

        if (line.startsWith("<p>")) {

          while (!line.contains("</p>")) {
            line += " " + inFile.readLine();
          }

          line = line.replaceAll("&#1642;","%"); // Arabic %
          line = cleanLine__youm7(line); // remove junk like <big> and <hr/>
          line = line.replaceAll("&nbsp;"," ");
          line = line.replaceAll("\\s+"," ");

          int i1 = line.indexOf("<p>");
          int i2 = line.indexOf("</p>");
          line = line.substring(i1+3,i2);
          line = line.replaceAll("<SPAN CLASS='NEWSSUBTITLETEXT'>","<span Class='NewsSubTitleText'>");
          line = line.replaceAll("</SPAN>","</span>");
          line = line.replaceAll("</span>","</span><br>"); // to force splitting after </span>
          String[] A = line.split("<br>");

          boolean subtitle_on = false, f_u = false, f_it = false, f_b = false;

          for (int i = 0; i < A.length; ++i) {
            String par = A[i];
            par = par.trim();
            String formattingStr = "";

            if (!subtitle_on && par.contains("<span Class='NewsSubTitleText'>")) {
              String extras = "";

              int s1 = par.indexOf("<span Class='NewsSubTitleText'>");
              int s2 = par.indexOf("</span>");
              if (s1 < s2) {
                par = par.substring(s1+31,s2);
              } else { // spans multiple lines
                par = par.substring(s1+31);
                subtitle_on = true;
              }

              if (par.contains("<u>")) {
                int u1 = par.indexOf("<u>"); int u2 = par.indexOf("</u>"); extras += "_u";
                if (u1 < u2 ) {
                  par = par.substring(u1+3,u2) + par.substring(u2+4);
                } else { // spans multiple lines
                  par = par.substring(u1+3);
                  f_u = true;
                }
              }

              if (par.contains("<i>")) {
                int it1 = par.indexOf("<i>"); int it2 = par.indexOf("</i>"); extras += "_i";
                if (it1 < it2 ) {
                  par = par.substring(it1+3,it2) + par.substring(it2+4);
                } else { // spans multiple lines
                  par = par.substring(it1+3);
                  f_it = true;
                }
              }

              if (par.contains("<b>")) {
                int b1 = par.indexOf("<b>"); int b2 = par.indexOf("</b>"); extras += "_b";
                if (b1 < b2 ) {
                  par = par.substring(b1+3,b2) + par.substring(b2+4);
                } else { // spans multiple lines
                  par = par.substring(b1+3);
                  f_b = true;
                }
              }

              formattingStr = "<NewsSubTitleText"+extras+">|||</NewsSubTitleText"+extras+">";

            } else if (subtitle_on) {
              String extras = "";
              if (f_u) extras += "_u";
              if (f_it) extras += "_i";
              if (f_b) extras += "_b";

              if (par.contains("</span>")) {
                int s2 = par.indexOf("</span>");
                par = par.substring(0,s2);
                subtitle_on = false;
              }

              if (par.contains("</u>")) {
                int u2 = par.indexOf("</u>");
                par = par.substring(0,u2) + par.substring(u2+4);
                f_u = false;
              }

              if (par.contains("</i>")) {
                int it2 = par.indexOf("</i>");
                par = par.substring(0,it2) + par.substring(it2+4);
                f_it = false;
              }

              if (par.contains("</b>")) {
                int b2 = par.indexOf("</b>");
                par = par.substring(0,b2) + par.substring(b2+4);
                f_b = false;
              }

              formattingStr = "<NewsSubTitleText"+extras+">|||</NewsSubTitleText"+extras+">";
            }

            par = par.trim();

            if (par.contains("<img src=")) {
              int im1 = par.indexOf("<img");
              int im2 = par.indexOf(">");
              if (im1 < im2) {
                par = par.substring(0,im1) + par.substring(im2+1);
              } else {
                par = par.substring(0,im1);
              }
            }

            par = par.trim();

            if (par.length() > 0) {
              D.segments.add(par);
              D.formatting.add(formattingStr);
            }
          } // for (i)
        }

        line = inFile.readLine();
      }

      if (D.segments.size() > 0) {
        data.add(D);
      } else { // no comments were found
        writeLine(fileName,outFile_empty);
      }

  }

  static private String cleanLine__youm7(String line) throws Exception
  {
    String S = line;

    while (S.contains("<a href=")) {
      int a1 = S.indexOf("<a href=");
      int a2 = S.indexOf("</a>");
      if (a1 < a2) {
        S = S.substring(0,a1) + S.substring(a2+4);
      } else {
        a2 = S.indexOf(">",a1);
        if (a1 < a2) {
          S = S.substring(0,a1) + S.substring(a2+1);
        }
      }
    }

    while (S.contains("<object")) {
      int o1 = S.indexOf("<object");
      int o2 = S.indexOf("</object>");
      S = S.substring(0,o1) + S.substring(o2+9);
    }

    while (S.contains("<embed")) {
      int e1 = S.indexOf("<embed");
      int e2 = S.indexOf("</embed>");
      if (e1 < e2) {
        S = S.substring(0,e1) + S.substring(e2+8);
      } else {
        e2 = S.indexOf("</embed");
        if (e1 < e2) {
          S = S.substring(0,e1) + S.substring(e2+7);
        } else {
          e2 = S.indexOf(">",e1);
          if (e1 < e2) {
            S = S.substring(0,e1) + S.substring(e2+1);
          }
        }
      }
    }

    while (S.contains("<iframe")) {
      int if1 = S.indexOf("<iframe");
      int if2 = S.indexOf("</iframe>");
      S = S.substring(0,if1) + S.substring(if2+9);
    }

    S = S.replaceAll("<big>"," ");
    S = S.replaceAll("</big>"," ");
    S = S.replaceAll("<BIG>"," ");
    S = S.replaceAll("</BIG>"," ");
    S = S.replaceAll("<body>"," ");
    S = S.replaceAll("</body>"," ");
    S = S.replaceAll("<center>"," ");
    S = S.replaceAll("</center>"," ");
    S = S.replaceAll("<CENTER>"," ");
    S = S.replaceAll("</CENTER>"," ");
    S = S.replaceAll("<strong>","");
    S = S.replaceAll("</strong>","");
    S = S.replaceAll("<STRONG>","");
    S = S.replaceAll("</STRONG>","");

    S = S.replaceAll("<hr/>"," ");
    S = S.replaceAll("<HR/>"," ");
    S = S.replaceAll("<br/>"," ");
    S = S.replaceAll("<BR/>"," ");
    S = S.replaceAll("</a>"," ");

    S = S.replaceAll("&#9668;"," ");
    S = S.replaceAll("&#9668; "," ");
    S = S.replaceAll(" &#9668;"," ");

    S = S.replaceAll("<span Class='NewsSubTitleText'></span>"," ");
    S = S.replaceAll("<span Class='NewsSubTitleText'> </span>"," ");

    while (S.contains("<span Class='NewsSubTitleText'><span Class='NewsSubTitleText'>")) {
      S = S.replaceAll("<span Class='NewsSubTitleText'><span Class='NewsSubTitleText'>","<span Class='NewsSubTitleText'>");
    }
    while (S.contains("<span Class='NewsSubTitleText'> <span Class='NewsSubTitleText'>")) {
      S = S.replaceAll("<span Class='NewsSubTitleText'> <span Class='NewsSubTitleText'>","<span Class='NewsSubTitleText'>");
    }
    while (S.contains("</span></span>") || S.contains("</span> </span>")) {
      S = S.replaceAll("</span></span>","</span>");
      S = S.replaceAll("</span> </span>","</span>");
    }

    S = S.replaceAll("\\s+"," ");
    S = S.trim();

    return S;
  }

  static private void extractBody__alriyadh(String docid, String fileName, boolean isNetArticle) throws Exception
  {
      InputStream inStream = new FileInputStream(new File(fileName));
      BufferedReader inFile = new BufferedReader(new InputStreamReader(inStream, "utf8"));

      String line = inFile.readLine();

      docInfo D = new docInfo("alriyadh_"+docid+(isNetArticle? "net" : "")+"_article");
      D.artURL = "http://www.alriyadh.com/article"+docid+".html";
      if (isNetArticle) D.artURL = "http://www.alriyadh.com/net/article/"+docid;

      String articleStartStr = "<div id=\"article-view\">";
      if (isNetArticle) articleStartStr= "<div class=\"box1 \">";

      String articleEndStr = "<!-- article-view -->";
      if (isNetArticle) articleEndStr= "<div class=\"left_icon\">";

      while (line != null && !line.equals(articleStartStr)) {

        if (line.contains("<title>")) {

          // deal with cases when title is split over more than one line
          while (!line.contains("</title>")) {
            line += " " + inFile.readLine();
          }

          int i1 = line.indexOf("<title>");
          int i2 = line.indexOf("</title>");
          if (i1 < i2) line = line.substring(i1+7,i2);
          line = line.replaceAll("&nbsp;"," ");
          line = line.replaceAll("\\s+"," ");
          line = line.trim();
          D.htmlTitle = line;

        } else if (line.startsWith("<div id=\"date\"")) { // for non-net articles

          line = inFile.readLine();
          while (line.trim().length() == 0) { // usually just once
            line = inFile.readLine();
          }

          // line:
          //   weekDay dayH monthH yearH h_ - dayM monthM yearMm - al3dd xxxxx
          // e.g.: (540000)
          //   aljm3x 20 rjb 1431 h_ - 2 yolyo 2010m - al3dd 15348

          // NOTES: (*) monthH could be two words
          //        (*) sometimes there is no space(!!) between (dayH,monthH) or (dayM,monthM) or (monthM,yearM)

          line = line.substring(line.indexOf("-")+1); // remove Hijri date (and first dash)

          // extract date
          String dateStr = line.substring(0,line.indexOf("-")).trim();
          dateStr = fix_dateStr__alriyadh(dateStr);

          String[] A = dateStr.split("\\s+");
          if (A[0].length() == 1) { A[0] = "0" + A[0]; } // dayM
          D.date = A[0] + "/" + monthArabicNameToNumber.get(A[1]) + "/" + A[2].substring(0,4);

          // extract issue
          String issueStr = line.substring(line.indexOf("-")+1).trim();
          issueStr = fix_issueStr__alriyadh(issueStr);
          D.issue = issueStr.split("\\s+")[1];

        }

        line = inFile.readLine();

      }

      while (line != null) {

        if (line.equals("<br clear=\"all\" />")) break;
        if (line.contains(articleEndStr)) break;

        line = line.replaceAll("&nbsp;"," ");
        line = line.replaceAll("\\s+"," ");
        line = line.trim();

        if (line.startsWith("<h2>") || line.startsWith("<h1>")) {

          char headlineSize = line.charAt(2);
          // deal with cases when headline is split over more than one line
          while (!line.contains("</h"+headlineSize+">")) {
            line += " " + inFile.readLine();
          }

          line = line.substring(4,line.indexOf("</h"+headlineSize+">")); // remove <hx> and </hx>
          line = line.trim();
          if (line.length() > 0) {
            D.segments.add(line);
            D.formatting.add("<h" + headlineSize + ">|||</h" + headlineSize + ">");
          }

        } else if (line.contains("<div class=\"author\">")) { // for net articles

          int i1 = line.indexOf("<div class=\"author\">");
          int i2 = line.indexOf("</div>");
          line = line.substring(i1+20,i2).trim();
          if (line.length() > 0) {
            D.segments.add(line);
            D.formatting.add("<author>|||</author>");
          }

        } else if (line.startsWith("<div class=\"date\"")) { // for net articles

          line = line.substring(line.indexOf(":"),line.indexOf("</div>"));

          // line:
          //   : yyyy-mm-dd hh:mm:ss
          // e.g.: (526276)
          //   : 2010-05-16 09:58:13

          // extract time
          String timeStr = line.split("\\s+")[2];
          D.time = timeStr;

          // extract date
          String dateStr = line.split("\\s+")[1];
          String[] A = dateStr.split("-");
          D.date = A[2] + "/" + A[1] + "/" + A[0];

        } else if (line.contains("<p")) {

          // deal with weirdness that involves an EOL character somehow in the middle of the line
          while (!line.contains("</p>")) {
            line += inFile.readLine();
          }

          line = line.substring(line.indexOf("<p"),line.indexOf("</p>")+4);

          // remove quotes
          if (line.contains("<span class=quote>")) {
            // deal with (rare) cases when <span> tag is split over more than one line
            while (!line.contains("</span>")) {
              line += " " + inFile.readLine();
            }
            int i1 = line.indexOf("<span class=quote>");
            int i2 = line.lastIndexOf("</span>");
            line = line.substring(0,i1) + line.substring(i2+7);
          }

          // remove images
          if (line.contains("<div class=\"img_title\">")) {
            // deal with (rare) cases when <img> tag is split over more than one line
            while (!line.contains("</div>")) {
              line += " " + inFile.readLine();
            }
            int i1 = line.indexOf("<div class=\"img_title\">");
            int i2 = line.lastIndexOf("</div>");
            line = line.substring(0,i1) + line.substring(i2+6);
          }

          // remove images
          if (line.contains("<div align=\"center\"><img src=")) {
/*
// I haven't observed this yet, but might be needed
            // deal with (rare) cases when <img> tag is split over more than one line
            while (!line.contains("</div>")) {
              line += " " + inFile.readLine();
            }
*/
            int i1 = line.indexOf("<div align=\"center\"><img src=");
            int i2 = line.lastIndexOf("</div>");
            line = line.substring(0,i1) + line.substring(i2+6);
          }

          // remove objects (happens in net articles with YouTube links)
          if (line.contains("<object")) {
            while (!line.contains("</object>")) {
              line += " " + inFile.readLine();
            }
            int i1 = line.indexOf("<object");
            int i2 = line.lastIndexOf("</object>");
            line = line.substring(0,i1) + line.substring(i2+9);
          }

          // these are rare, but happen
          line = line.replaceAll("<pr>","");
          line = line.replaceAll("<br>","");
          line = line.replaceAll("<BR>","");
          line = line.replaceAll("<div align=\"center\">","");
          line = line.replaceAll("</div>","");
          line = line.trim();

          String formattingStr = "";
          if (line.equals("<p>")) {
            line = "";
            formattingStr = "";
          } else if (line.startsWith("<p>")) {
            line = line.substring(3,line.length()-4).trim();
            formattingStr = "";
          } else if (line.startsWith("<p class=\"author\">")) { // for non-net articles
            line = line.substring(18,line.length()-4).trim();
            formattingStr = "<author>|||</author>";
          }

          if (line.length() > 0) {
            D.segments.add(line);
            D.formatting.add(formattingStr);
          }

        }

        line = inFile.readLine();

      }

      if (D.segments.size() > 0) {
        data.add(D);
      } else { // no comments were found
        writeLine(fileName,outFile_empty);
      }

  }

  static String fix_dateStr__alriyadh(String dateStr)
  {
    String[] A = dateStr.split("\\s+");
    if (A.length == 3) return dateStr;
    else {
      String d,m,y;
      if (A[1].length() == 5) {
        // year is OK, day and month are connected
        int firstAlpha_i = 0;
        for (int i = 0; i < A[0].length(); ++i) {
          char c = A[0].charAt(i);
          if (c < '0' || c > '9'){ firstAlpha_i = i; break;}
        }
        d = A[0].substring(0,firstAlpha_i);
        m = A[0].substring(firstAlpha_i);
        y = A[1];
      } else {
        // day is OK, month and year are connected
        d = A[0];
        int mLen = A[1].length() - 5;
        m = A[1].substring(0,mLen);
        y = A[1].substring(mLen);
      }

      return d + " " + m + " " + y;
    }

  }

  static String fix_issueStr__alriyadh(String issueStr)
  {
    if (issueStr.split("\\s+").length == 2) return issueStr;
    else {
      String w,n;
      int firstNumeric_i = 0;
      for (int i = 0; i < issueStr.length(); ++i) {
        char c = issueStr.charAt(i);
        if (c >= '0' && c <= '9'){ firstNumeric_i = i; break;}
      }
      w = issueStr.substring(0,firstNumeric_i);
      n = issueStr.substring(firstNumeric_i);
      return w + " " + n;
    }
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
  public String htmlTitle;
  public String date;
  public String issue;
  public String time;

  public Vector<String> segments;
  public Vector<String> formatting;

  public int timesCounted;
  public static int charCount;
  public static int wordCount;
  public static int segCount;
  public static int artCount;

  static public void resetCounts()
  {
    charCount = 0;
    wordCount = 0;
    segCount = 0;
    artCount = 0;
  }

  public docInfo(String id)
  {
    docid = id;
    artURL = null;
    htmlTitle = null;
    date = null;
    time = null;
    issue = null;

    segments = new Vector<String>();
    formatting = new Vector<String>();

    timesCounted = 0;
  }

  public void incrementCounts()
  {
    artCount += 1;
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
    if (htmlTitle != null) header += " htmlTitle=\"" + htmlTitle + "\"";
    if (date != null) header += " date=\"" + date + "\"";
    if (time != null) header += " time=\"" + time + "\"";
    if (issue != null) header += " issue=\"" + issue + "\"";
    header += ">";

    writeLine(header,outFile);

    int i = 0;
    for (String seg : segments) {
      ++i; // 1-based
      String formattingStr = formatting.elementAt(i-1);
      if (!formattingStr.equals("")) {
        String prev_formattingStr = "";
        if (i > 1) prev_formattingStr = formatting.elementAt(i-2);
        if (!formattingStr.equals(prev_formattingStr)) {
          String[] A = formattingStr.split("\\|\\|\\|");
          writeLine(A[0],outFile);
        }
      }

      writeLine("<seg id=\""+i+"\"> "+seg.replaceAll("\\s+"," ")+" </seg>",outFile);

      if (!formattingStr.equals("")) {
        String next_formattingStr = "";
        if (i < formatting.size()) next_formattingStr = formatting.elementAt(i);
        if (!formattingStr.equals(next_formattingStr)) {
          String[] A = formattingStr.split("\\|\\|\\|");
          writeLine(A[1],outFile);
        }
      }
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

