// Diff         Text file difference utility.
// ----         Copyright 1987, 1989 by Donald C. Lindsay,
//              School of Computer Science,  Carnegie Mellon University.
//              Copyright 1982 by Symbionics.
//              Use without fee is permitted when not for direct commercial
//              advantage, and when credit to the source is given. Other uses
//              require specific permission.
// 
// Converted from C to Java by Ian F. Darwin, http://www.darwinsys.com/, January, 1997.
// Copyright 1997, Ian F. Darwin.
// 
// Conversion is NOT FULLY TESTED.
// 
// USAGE:      diff oldfile newfile
// 
// This program assumes that "oldfile" and "newfile" are text files.
// The program writes to stdout a description of the changes which would
// transform "oldfile" into "newfile".
// 
// The printout is in the form of commands, each followed by a block of
// text. The text is delimited by the commands, which are:
// 
//    DELETE AT n
//         ..deleted lines
// 
//    INSERT BEFORE n
//         ..inserted lines
// 
//    n MOVED TO BEFORE n
//         ..moved lines
// 
//    n CHANGED FROM
//         ..old lines
//    CHANGED TO
//         ..newer lines
// 
// The line numbers all refer to the lines of the oldfile, as they are
//    numbered before any commands are applied.
// The text lines are printed as-is, without indentation or prefixing. The
//    commands are printed in upper case, with a prefix of ">>>>", so that
//    they will stand out. Other schemes may be preferred.
// Files which contain more than MAXLINECOUNT lines cannot be processed.
//    This can be fixed by changing "symbol" to a Vector.
// The algorithm is taken from Communications of the ACM, Apr78 (21, 4, 264-),
//    "A Technique for Isolating Differences Between Files."
//    Ignoring I/O, and ignoring the symbol table, it should take O(N) time.
//    This implementation takes fixed space, plus O(U) space for the symbol
//    table (where U is the number of unique lines). Methods exist to change
//    the fixed space to O(N) space.
// Note that this is not the only interesting file-difference algorithm. In
//    general, different algorithms draw different conclusions about the
//    changes that have been made to the oldfile. This algorithm is sometimes
//    "more right", particularly since it does not consider a block move to be 
//    an insertion and a (separate) deletion. However, on some files it will be
//    "less right". This is a consequence of the fact that files may contain
//    many identical lines (particularly if they are program source). Each
//    algorithm resolves the ambiguity in its own way, and the resolution
//    is never guaranteed to be "right". However, it is often excellent.
// This program is intended to be pedagogic.  Specifically, this program was
//    the basis of the Literate Programming column which appeared in the
//    Communications of the ACM (CACM), in the June 1989 issue (32, 6,
//    740-755).
// By "pedagogic", I do not mean that the program is gracefully worded, or
//    that it showcases language features or its algorithm. I also do not mean
//    that it is highly accessible to beginners, or that it is intended to be
//    read in full, or in a particular order. Rather, this program is an
//    example of one professional's style of keeping things organized and
//    maintainable.
// The program would be better if the "print" variables were wrapped into
//    a struct. In general, grouping related variables in this way improves
//    documentation, and adds the ability to pass the group in argument lists.
// This program is a de-engineered version of a program which uses less
//    memory and less time.  The article points out that the "symbol" arrays
//    can be implemented as arrays of pointers to arrays, with dynamic
//    allocation of the subarrays.  (In C, macros are very useful for hiding 
//    the two-level accesses.) In Java, a Vector would be used. This allows an
//    extremely large value for MAXLINECOUNT, without dedicating fixed arrays.
//    (The "other" array can be allocated after the input phase, when the exact
//    sizes are known.) The only slow piece of code is the "strcmp" in the tree
//    descent: it can be speeded up by keeping a hash in the tree node, and
//    only using "strcmp" when two hashes happen to be equal.



import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//////////////////////////////////////////////////////////////////////////////
// Class: fileInfo
//////////////////////////////////////////////////////////////////////////////

class fileInfo {    
  static final int MAXLINECOUNT = 20000;
  BufferedReader file;   // File handle that is open for read.
  public int maxLine;     // After input done, # lines in file.
  Node symbol[];          // The symtab handle of each line.
  int other[];            // Map of line# to line# in other file
                          // ( -1 means don't-know ).
                          // Allocated AFTER the lines are read.

  // Normal constructor with one filename; file is opened and saved.
  fileInfo( String filename ) {
    symbol = new Node [ MAXLINECOUNT+2 ];
    other  = null;    // allocated later!
    try {
      FileInputStream fStream = new FileInputStream(filename);
      DataInputStream dStream = new DataInputStream(fStream);
      file = new BufferedReader(new InputStreamReader(dStream));
    } catch (IOException e) {
      System.err.println("Diff can't read file " + filename );
      System.err.println("Error Exception was:" + e );
      System.exit(1);
    }
  }

  // This is done late, to be same size as # lines in input file.
  void alloc() {
    other  = new int[symbol.length + 2];
  }
};


//////////////////////////////////////////////////////////////////////////////
// Class: Diff
//
// The info is kept here per-file.
//////////////////////////////////////////////////////////////////////////////

public class Diff {

  // block len > any possible real block len
  final int UNREAL=Integer.MAX_VALUE;

  // Keeps track of information about file1 and file2
  fileInfo oldinfo, newinfo;

  //blocklen is the info about found blocks. 
  //It will be set to 0, except at the line#s where blocks start in the old file. 
  //At these places it will be set to the # of lines in the block. 
  //During printout, this # will be reset to -1 if the block is printed as a MOVE block
  //(because the printout phase will encounter the block twice, but must only print it once.)
  //The array declarations are to MAXLINECOUNT+2 so that we can have two extra lines 
  //(pseudolines) at line# 0 and line# MAXLINECOUNT+1 (or less).
  int blocklen[];

  public static void main(String argstrings[])
  {
    if ( argstrings.length != 2 ) {
      System.err.println("Usage: diff oldfile newfile" );
      System.exit(1);
    }
    Diff d = new Diff();
    d.doDiff(argstrings[0], argstrings[1]);
    return;
  }

  // Constructor
  Diff() {
  }

  // Do one file comparison. Called with both filenames.
  public void doDiff(String oldFile, String newFile) {
    System.out.println( ">>>> Difference of file \"" + oldFile + "\" and file \"" + newFile + "\".\n");
    oldinfo = new fileInfo(oldFile);
    newinfo = new fileInfo(newFile);
    // we don't process until we know both files really do exist.
    try {
      inputScan( oldinfo );
      inputScan( newinfo );
    } catch (IOException e) {
      System.err.println("Read error: " + e);
    }

    // Now that we've read all the lines, allocate some arrays.
    blocklen = new int[ (oldinfo.maxLine>newinfo.maxLine ? oldinfo.maxLine : newinfo.maxLine) + 2 ];
    oldinfo.alloc();
    newinfo.alloc();

    // Now do the work, and print the results.
    transform();
    printout();
  }

  //Reads the file specified by pinfo.file.
  //Places the lines of that file in the symbol table.
  //Sets pinfo.maxLine to the number of lines found.
  void inputScan( fileInfo pinfo ) throws IOException
  {
    String linebuffer;
    pinfo.maxLine = 0;
    while ((linebuffer = pinfo.file.readLine()) != null) {
      storeLine( linebuffer, pinfo );
    }
  }

  //Places line into symbol table.
  //Expects pinfo.maxLine initted: increments.
  //Places symbol table handle in pinfo.ymbol.
  //Expects pinfo is either oldinfo or newinfo.
  void storeLine( String linebuffer, fileInfo pinfo )
  {
    int linenum = ++pinfo.maxLine;    // note, no line zero
    if ( linenum > fileInfo.MAXLINECOUNT ) {
      System.err.println( "MAXLINECOUNT exceeded, must stop." );
      System.exit(1);
    }
    pinfo.symbol[ linenum ] = Node.addSymbol( linebuffer, pinfo == oldinfo, linenum );
  }


  //Analyzes the file differences and leaves its findings in
  //the global arrays oldinfo.other, newinfo.other, and blocklen.
  //Expects both files in symtab.
  //Expects valid "maxLine" and "symbol" in oldinfo and newinfo.
  void transform()
  {                                  
    int oldline, newline;
    int oldmax = oldinfo.maxLine + 2;  // Count pseudolines at
    int newmax = newinfo.maxLine + 2;  // ..front and rear of file

    for (oldline=0; oldline < oldmax; oldline++ ) {
      oldinfo.other[oldline]= -1;
    }
    for (newline=0; newline < newmax; newline++ ) {
      newinfo.other[newline]= -1;
    }

    scanUnique();  // scan for lines used once in both files
    scanAfter();   // scan past sure-matches for non-unique blocks 
    scanBefore();  // scan backwards from sure-matches
    scanBlocks();  // find the fronts and lengths of blocks
  }


  //Scans for lines which are used exactly once in each file.
  //Expects both files in symtab, and oldinfo and newinfo valid.
  //The appropriate "other" array entries are set to the line# in the other file.
  //Claims pseudo-lines at 0 and XXXinfo.maxLine+1 are unique.
  void scanUnique()
  {
    int oldline, newline;
    Node psymbol;

    for( newline = 1; newline <= newinfo.maxLine; newline++ ) {
      psymbol = newinfo.symbol[ newline ];
      if ( psymbol.symbolIsUnique()) {        // 1 use in each file
        oldline = psymbol.linenum;
        newinfo.other[ newline ] = oldline;   // record 1-1 map
        oldinfo.other[ oldline ] = newline;
      }
    }
    newinfo.other[ 0 ] = 0;
    oldinfo.other[ 0 ] = 0;
    newinfo.other[ newinfo.maxLine + 1 ] = oldinfo.maxLine + 1;
    oldinfo.other[ oldinfo.maxLine + 1 ] = newinfo.maxLine + 1;
  }


  //Expects both files in symtab, and oldinfo and newinfo valid.
  //Expects the "other" arrays contain positive #s to indicate lines that are unique in both files.
  //For each such pair of places, scans past in each file.
  //Contiguous groups of lines that match non-uniquely are taken to be good-enough matches, and so marked in "other".
  //Assumes each other[0] is 0.
  void scanAfter()
  {
    int oldline, newline;

    for( newline = 0; newline <= newinfo.maxLine; newline++ ) {
      oldline = newinfo.other[ newline ];
      if ( oldline >= 0 ) {   // is unique in old & new
        for(;;) {             // scan after there in both files
          if ( ++oldline > oldinfo.maxLine   ) {
            break; 
          }
          if ( oldinfo.other[ oldline ] >= 0 ) { 
            break;
          }
          if ( ++newline > newinfo.maxLine   ) { 
            break; 
          }
          if ( newinfo.other[ newline ] >= 0 ) { 
            break;
          }
          // oldline & newline exist, and aren't already matched

          if ( newinfo.symbol[ newline ] != oldinfo.symbol[ oldline ] ) { 
            break;  // not same
          }

          newinfo.other[newline] = oldline; // record a match
          oldinfo.other[oldline] = newline;
        }
      }
    }
  }


  //As scanafter, except scans towards file fronts.
  //Assumes the off-end lines have been marked as a match.
  void scanBefore()
  {
    int oldline, newline;

    for( newline = newinfo.maxLine + 1; newline > 0; newline-- ) {
      oldline = newinfo.other[ newline ];
      if ( oldline >= 0 ) {   // unique in each
        for(;;) {
          if ( --oldline <= 0 ) {
            break;
          }
          if ( oldinfo.other[ oldline ] >= 0 ) { 
            break;
          }
          if ( --newline <= 0 ) { 
            break;
          }
          if ( newinfo.other[ newline ] >= 0 ) { 
            break;
          }
          // oldline and newline exist, and aren't marked yet

          if ( newinfo.symbol[ newline ] != oldinfo.symbol[ oldline ] ) {
            break;  // not same
          }

          newinfo.other[newline] = oldline; // record a match
          oldinfo.other[oldline] = newline;
        }
      }
    }
  }


  //Finds the beginnings and lengths of blocks of matches.
  //Sets the blocklen array (see definition).
  //Expects oldinfo valid.
  void scanBlocks()
  {
    int oldline, newline;
    int oldfront = 0;      // line# of front of a block in old, or 0 
    int newlast = -1;      // newline's value during prev. iteration

    for( oldline = 1; oldline <= oldinfo.maxLine; oldline++ ) {
      blocklen[ oldline ] = 0;
    }
    blocklen[ oldinfo.maxLine + 1 ] = UNREAL; // starts a mythical blk

    for( oldline = 1; oldline <= oldinfo.maxLine; oldline++ ) {
      newline = oldinfo.other[ oldline ];
      if ( newline < 0 ) {
        oldfront = 0;  // no match: not in block
      }
      else {  // match.
        if ( oldfront == 0 ) { 
          oldfront = oldline;
        }
        if ( newline != (newlast+1)) {
          oldfront = oldline;
        }
        ++blocklen[ oldfront ];            
      }
      newlast = newline;
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // The following are global to printout's subsidiary routines
  // enum { idle, delete, insert, movenew, moveold, same, change } printstatus;
  //////////////////////////////////////////////////////////////////////////////
  
  public static final int idle = 0, delete = 1, insert = 2, movenew = 3, moveold = 4, same = 5, change = 6;
  int printstatus;
  boolean anyprinted;
  int printoldline, printnewline;     // line numbers in old & new file

  //Prints summary to stdout.
  //Expects all data structures have been filled out.
  void printout()
  {
    printstatus = idle;
    anyprinted = false;
    for( printoldline = printnewline = 1; ; ) {
      if ( printoldline > oldinfo.maxLine ) { 
        newconsume(); 
        break;
      }
      if ( printnewline > newinfo.maxLine ) { 
        oldconsume(); 
        break;
      }
      if ( newinfo.other[ printnewline ] < 0 ) {
        if ( oldinfo.other[ printoldline ] < 0 )
          showchange();
        else
          showinsert();
      }
      else if ( oldinfo.other[ printoldline ] < 0 )
        showdelete();
      else if ( blocklen[ printoldline ] < 0 )
        skipold();
      else if ( oldinfo.other[ printoldline ] == printnewline )
        showsame();
      else
        showmove();
    }
    if ( anyprinted == true ) 
      System.out.println( ">>>> End of differences."  );
    else
      System.out.println( ">>>> Files are identical." );
  }

  //Part of printout. Have run out of old file. 
  //Print the rest of the new file, as inserts and/or moves.
  void newconsume()
  {
    for(;;) {
      if ( printnewline > newinfo.maxLine )
        break;        // end of file
      if ( newinfo.other[ printnewline ] < 0 ) 
        showinsert();
      else
        showmove();
    }
  }

  //Part of printout. Have run out of new file.
  //Process the rest of the old file, printing any parts which were deletes or moves.
  void oldconsume()
  {
    for(;;) {
      if ( printoldline > oldinfo.maxLine )
        break;       // end of file
      printnewline = oldinfo.other[ printoldline ];
      if ( printnewline < 0 ) 
        showdelete();
      else if ( blocklen[ printoldline ] < 0 ) 
        skipold();
      else 
        showmove();
    }
  }

  //Part of printout.
  //Expects printoldline is at a deletion.
  void showdelete()
  {
    if ( printstatus != delete )
      System.out.println( ">>>> DELETE AT " + printoldline);
    printstatus = delete;
    oldinfo.symbol[ printoldline ].showSymbol();
    anyprinted = true;
    printoldline++;
  }

  //Part of printout.
  //Expects printnewline is at an insertion.
  void showinsert()
  {
    if ( printstatus == change ) 
      System.out.println( ">>>>     CHANGED TO" );
    else if ( printstatus != insert ) 
      System.out.println( ">>>> INSERT BEFORE " + printoldline );
    printstatus = insert;
    newinfo.symbol[ printnewline ].showSymbol();
    anyprinted = true;
    printnewline++;
  }

  //Part of printout.
  //Expects printnewline is an insertion.
  //Expects printoldline is a deletion.
  void showchange()
  {
    if ( printstatus != change ) 
      System.out.println( ">>>> " + printoldline + " CHANGED FROM");
    printstatus = change;
    oldinfo.symbol[ printoldline ].showSymbol();
    anyprinted = true;
    printoldline++;
  }

  //Part of printout.
  //Expects printoldline at start of an old block that has already been announced as a move.
  //Skips over the old block.
  void skipold()
  {
    printstatus = idle;
    for(;;) {
      if ( ++printoldline > oldinfo.maxLine )
        break;    // end of file
      if ( oldinfo.other[ printoldline ] < 0 )
        break;    // end of block
      if ( blocklen[ printoldline ]!=0)
        break;    // start of another
    }
  }

  //Part of printout.
  //Expects printnewline is at start of a new block that has
  //already been announced as a move.
  //Skips over the new block.
  void skipnew()
  {
    int oldline;
    printstatus = idle;
    for(;;) {
      if ( ++printnewline > newinfo.maxLine )
        break;    // end of file
      oldline = newinfo.other[ printnewline ];
      if ( oldline < 0 )
        break;    // end of block
      if ( blocklen[ oldline ] != 0)
        break;    // start of another
    }
  }

  //Part of printout.
  //Expects printnewline and printoldline at start of
  //two blocks that aren't to be displayed.
  void showsame()
  {
    int count;
    printstatus = idle;
    if ( newinfo.other[ printnewline ] != printoldline ) {
      System.err.println("BUG IN LINE REFERENCING");
      System.exit(1);
    }
    count = blocklen[ printoldline ];
    printoldline += count;
    printnewline += count;
  }

  //Part of printout.
  //Expects printoldline, printnewline at start of
  //two different blocks ( a move was done).
  void showmove()
  {
    int oldblock = blocklen[ printoldline ];
    int newother = newinfo.other[ printnewline ];
    int newblock = blocklen[ newother ];

    if ( newblock < 0 ) skipnew();         // already printed.
    else if ( oldblock >= newblock ) {     // assume new's blk moved.
      blocklen[newother] = -1;         // stamp block as "printed".
      System.out.println( ">>>> " + newother + " THRU " + (newother + newblock - 1) + " MOVED TO BEFORE " + printoldline );
      for( ; newblock > 0; newblock--, printnewline++ )
        newinfo.symbol[ printnewline ].showSymbol();
      anyprinted = true;
      printstatus = idle;

    } else        // assume old's block moved
      skipold();  // target line# not known, display later
  }
};        



//////////////////////////////////////////////////////////////////////////////
// Class: Node
// The symbol table routines in this class all understand the symbol table format,
// which is a binary tree.
// The methods are: addSymbol, symbolIsUnique, showSymbol.
//////////////////////////////////////////////////////////////////////////////

class Node {            // the tree is made up of these nodes
  Node pleft, pright;
  int linenum;

  static final int freshnode = 0,
      oldonce = 1, newonce = 2, bothonce = 3, other = 4;

  int /* enum linestates */ linestate;
  String line;

  static Node panchor = null;     // symtab is a tree hung from this

  // Construct a new symbol table node and fill in its fields.
  // Parameter:  A line of the text file
  Node( String pline)
  {
    pleft = pright = null;
    linestate = freshnode;
    // linenum field is not always valid     
    line = pline;
  }

  // Searches tree for a match to the line.
  // Parameter: a line of text
  // If node's linestate == freshnode, then created the node.
  static Node matchsymbol( String pline )
  {
    int comparison;
    Node pnode = panchor;
    if ( panchor == null ) return panchor = new Node( pline);
    for(;;) {
      comparison = pnode.line.compareTo(pline);
      if ( comparison == 0 ) return pnode;          // found

      if ( comparison < 0 ) {
        if ( pnode.pleft == null ) {
          pnode.pleft = new Node( pline);
          return pnode.pleft;
        }
        pnode = pnode.pleft;
      }
      if ( comparison > 0 ) {
        if ( pnode.pright == null ) {
          pnode.pright = new Node( pline);
          return pnode.pright;
        }
        pnode = pnode.pright;
      }
    }
    // NOTE: There are return stmts, so control does not get here.
  }

  // Saves line into the symbol table.
  // Returns a handle to the symtab entry for that unique line.
  // If inoldfile nonzero, then linenum is remembered.
  static Node addSymbol( String pline, boolean inoldfile, int linenum )
  {
    Node pnode;
    pnode = matchsymbol( pline );  // find the node in the tree
    if ( pnode.linestate == freshnode ) {
      pnode.linestate = inoldfile ? oldonce : newonce;
    } else {
      if (( pnode.linestate == oldonce && !inoldfile ) ||
          ( pnode.linestate == newonce &&  inoldfile )) 
        pnode.linestate = bothonce;
      else pnode.linestate = other;
    }
    if (inoldfile) pnode.linenum = linenum;
    return pnode;
  }

  // Arg is a ptr previously returned by addSymbol.
  // Returns true if the line was added to the symbol table exactly once with inoldfile true, 
  // and exactly once with inoldfile false.
  boolean symbolIsUnique()
  {
    return (linestate == bothonce );
  }


  // Prints the line to stdout.
  void showSymbol()
  {
    System.out.println(line);
  }
}




