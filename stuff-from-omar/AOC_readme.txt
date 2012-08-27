--------------------------------------------------------
  README for the Arabic Online Commentary Dataset v1.1
--------------------------------------------------------

1. Introduction
-----------------

The AOC dataset was created by crawling the websites of three Arabic newspapers,
and extracting online articles and readers' comments.  The readers' comments are
arguably more "interesting", which is why we call this the *commentary* dataset,
but the articles themselves are also included.


2. Sources
------------

The extraction crawled webpages corresponding to a roughly-6-month period,
covering early April 2010 to early October 2010.  The three newspapers are:

  1) Al-Ghad (الغد), a Jordanian newspaper (www.alghad.com)
       Crawled URLs:         http://www.alghad.com/?news=500000
                     through http://www.alghad.com/?news=534000
                     (each page contains both the article and the corresponding comments)

  2) Al-Riyadh (الرياض), a Saudi newspaper (www.alriyadh.com)
       Crawled URLs:         http://www.alriyadh.com/article520000.html
                     through http://www.alriyadh.com/article565000.html for the articles,
                         and http://www.alriyadh.com/newspaper/comments/520000/1
                     through http://www.alriyadh.com/newspaper/comments/565000/1 for the corresponding comments

  3) Al-Youm Al-Sabe' (اليوم السابع), an Egyptian newspaper (www.youm7.com)
       Crawled URLs:         http://www.youm7.com/News.asp?NewsID=210000
                     through http://www.youm7.com/News.asp?NewsID=285000 for the articles,
                         and http://www.youm7.com/Includes/NewsComments.asp?NewsID=210000&page=[1|2|...]
                     through http://www.youm7.com/Includes/NewsComments.asp?NewsID=210000&page=[1|2|...] for the corresponding comments

For alriyadh.com, around 7% of the article URLs redirect to the "Net" edition of
Al-Riyadh.  For example

  http://www.alriyadh.com/article520116.html

...redirects to:

  http://www.alriyadh.com/net/article/520116

Those articles seem to be only published online, and not in the paper edition,
as they do not have an issue number (which the other articles do), and they have
a timestamp (which the other articles do not).


2. Organization
----------------

The extracted comments were split into segments based on hard returns entered
by the author, as indicated by the HTML <br> tag.

The extracted articles were split into segments based on hard returns indicated
by <br> and paragraph breaks indicates by <p> (or <p ... >).

No further punctuation-based segmentation was performed, though it is perfectly
reasonable for you to do so.

The dataset contains six XML files, two per newspaper (one for the comments and
one for the articles).  The XML files contain the segments themselves,
in addition to some other relevant information for each comment or article,
stored as XML fields explained in the next section.


3. XML Fields
--------------

In the comment XML files (AOC_*_comments.xml), there are 4 fields that apply
to every comment:

  (*) articleURL: The URL of the newspaper article (NOT the comments page).
  (*) date: The date on which the comment was posted, formatted dd/mm/yyyy.
  (*) time: The time at which the comment was posted, formatted hh:mm (or hh:mm:ss for alghad.com), following a 24-hour format (i.e. hh is between 00 and 23).
  (*) author: The author "ID" associated with that comment, as entered by the author.

...and there are 3 fields that apply to some but not all comments:

  (*) subtitle: a header entered by the author for their comment.  (Only for alghad.com and youm7.com comments.)
  (*) authorEmail: an e-mail address entered by the author.  (Only for alghad.com comments.)
  (*) authorLocation: a location entered by the author.  (Only for alghad.com comments.)


In the article XML files (AOC_*_articles.xml), there are 3 fields that apply
to every article:

  (*) articleURL: Same as above.
  (*) date: The date the article was published, formatted dd/mm/yyyy.
  (*) htmlTitle: The string encapsulated within the HTML <title></title> span.

...and there are 2 fields that apply to some but not all articles:

  (*) time: The time at which the article was published, formatted hh:mm (or hh:mm:ss for the alriyadh.com "Net" articles),
      following a 24-hour format (i.e. hh is between 00 and 23).  (Only for alghad.com, youm7.com, and the alriyadh.com "Net" articles.)
  (*) issue: The issue number where the article was published.  (Only for the alriyadh.com non-"Net" articles.)


4. The Datasets
----------------

The webpages were downloaded by supplying a URL list to the wget command.  Not
every URL has an article on it (though 97% of them do), and not every article
has reader comments.  Here is a breakdown of those quantities:

-----------------------------------------------------------------------------------
      Source        |   Al-Ghad      Al-Riyadh   Al-Youm Al-Sabe'      ALL
-----------------------------------------------------------------------------------
  # URLs crawled    |   34,001        45,001          75,001         154.0K files
  # URLs w/article  |   32,223        43,506          73,798         149.5K files
  # URLs w/comment  |    6,299        34,163          45,667          86.1K files
-----------------------------------------------------------------------------------
  % URLs w/article  |   94.8%         96.7%           98.4%           97.1%
  % arts w/comment  |   19.5%         78.5%           61.9%           57.6%
-----------------------------------------------------------------------------------

Note that the release does not include the downloaded HTML files themselves,
because the total size of these files is quite large (about 1 GB compressed).
But if for some reason you are interested in the raw files, let me know.


The commentary data consists of 3.1M segments, corresponding to 52.1M words
(word: longest sequence of non-space characters).  Here is the breakdown of the
86.1K articles across the three sources:

------------------------------------------------------------------------------------------------
      Source        |   Al-Ghad      Al-Riyadh   Al-Youm Al-Sabe'     TOTAL
------------------------------------------------------------------------------------------------
  # arts w/comment  |      6,299         34,163         45,667        86.1K articles
  # comments        |     26,648        804,968        564,853         1.4M comments
  # segments        |     63,304      1,685,533      1,383,952         3.1M segments
  # words           |  1,235,300     18,782,395     32,132,157        52.1M words
  # characters      |  6,878,512    104,231,502    177,604,767       288.7M characters
  XML file size     |     19.7 MB       340.0 MB       446.3 MB      806.0 MB (195.0 MB zipped)
------------------------------------------------------------------------------------------------
  comments/article  |      4.23          23.56          12.37         16.21
  segments/comment  |      2.38           2.09           2.45          2.24
  words/segment     |     19.51          11.14          23.22         16.65
  characters/word   |      5.57           5.55           5.53          5.54
------------------------------------------------------------------------------------------------


The article data consists of 1.4M segments, corresponding to 42.5M words.  Note
that a "segment" here is often a full paragraph, and further punctuation-based
segmentation would result in a higher number of sentences (or true "segments" if
you prefer to call it that).  Here is the breakdown of the 149.5K articles
across the three sources:

------------------------------------------------------------------------------------------------
      Source        |   Al-Ghad      Al-Riyadh   Al-Youm Al-Sabe'     TOTAL
------------------------------------------------------------------------------------------------
  # articles        |     32,223        43,506         73,798        149.5K articles
  # segments        |    367,324       383,651        615,168          1.4M segments
  # words           | 11,424,287    12,676,090     18,413,850         42.5M words
  # characters      | 68,393,539    75,503,507    109,007,044        252.9M characters
  XML file size     |    134.1 MB      150.9 MB       223.6 MB       508.6 MB (130.2 MB zipped)
------------------------------------------------------------------------------------------------
  segments/article  |     11.40          8.82           8.34           9.14
  words/segment     |     31.10         33.04          29.93          31.12
  characters/word   |      5.99          5.96           5.92           5.95
------------------------------------------------------------------------------------------------

Keep in mind that many of the articles do not have any comments associated with
them, since no reader comments were found when they were crawled.


5. History
-----------

v1.1 (Nov. 29, 2010)
  added article data
  expanded readme
  updated sample XML file
  (no change at all to commentary data)

v1.0 (Nov. 1, 2010)
  initial release of commentary data alone


6. Contact Me
--------------

If you have any questions about the data, please contact me: ozaidan@cs.jhu.edu

--O.Z.


