package se.prolore.metrics;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

//////////////////////////////////////////////////////////////////////////////
// Class: fileInfo
//////////////////////////////////////////////////////////////////////////////

class fileInfo {    
  static final int MAXLINECOUNT = 20000;
  BufferedReader file;    // Reads from a character input stream.
  public int maxLine;     // After input done, Nr lines in file.
  Node symbol[];          // The symtab handle of each line.
  int other[];            // Map of lineNr to lineNr in other file ( -1 means don't-know ).
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
  // Code Churn
  private int adLOC = 0; // Added Lines of Code
  private int chLOC = 0; // Changed Lines of Code
  private int dlLOC = 0; // Deleted Lines of Code

  // block len > any possible real block len
  final int UNREAL=Integer.MAX_VALUE;

  // Keeps track of information about old file and new file
  fileInfo oldFileInfo, newFileInfo;

  //blocklen is the info about found blocks. 
  //It will be set to 0, except at the line#s where blocks start in the old file. 
  //At these places it will be set to the # of lines in the block. 
  //During printout, this # will be reset to -1 if the block is printed as a MOVE block
  //(because the printout phase will encounter the block twice, but must only print it once.)
  //The array declarations are to MAXLINECOUNT+2 so that we can have two extra lines 
  //(pseudolines) at line# 0 and line# MAXLINECOUNT+1 (or less).
  int blocklen[];
  
  // Constructor
  Diff() {
  }
  
  public void countChurn(ComplexityParser oldFile, ComplexityParser newFile) {
      doDiff(oldFile.getSourceFile().getAbsolutePath(), newFile.getSourceFile().getAbsolutePath());
      calculateChurn();
      
      newFile.setAddedLines(adLOC); // Added Lines of Code
      newFile.setChangedLines(chLOC); // Changed Lines of Code
      newFile.setDeletedLines(dlLOC); // Deleted Lines of Code
  }

  // Do one file comparison. Called with both filenames.
  public void doDiff(String oldFile, String newFile) {
    System.out.println( ">>>> Difference of file \"" + oldFile + "\" and file \"" + newFile + "\".\n");
    oldFileInfo = new fileInfo(oldFile);
    newFileInfo = new fileInfo(newFile);
    // we don't process until we know both files really do exist.
    try {
      inputScan( oldFileInfo );
      inputScan( newFileInfo );
    } catch (IOException e) {
      System.err.println("Read error: " + e);
    }

    // Now that we've read all the lines, allocate some arrays.
    blocklen = new int[ (oldFileInfo.maxLine>newFileInfo.maxLine ? oldFileInfo.maxLine : newFileInfo.maxLine) + 2 ];
    oldFileInfo.alloc();
    newFileInfo.alloc();

    // Now do the work, and print the results.
    transform();
  }

  //Reads the file specified by pinfo.file.
  //Places the lines of that file in the symbol table.
  //Sets pinfo.maxLine to the number of lines found.
  void inputScan( fileInfo pinfo ) throws IOException {
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
  void storeLine( String linebuffer, fileInfo pinfo ) {
    int linenum = ++pinfo.maxLine;    // note, no line zero
    if ( linenum > fileInfo.MAXLINECOUNT ) {
      System.err.println( "MAXLINECOUNT exceeded, must stop." );
      System.exit(1);
    }
    pinfo.symbol[ linenum ] = Node.addSymbol( linebuffer, pinfo == oldFileInfo, linenum );
  }

  //Analyzes the file differences and leaves its findings in
  //the global arrays oldinfo.other, newinfo.other, and blocklen.
  //Expects both files in symtab.
  //Expects valid "maxLine" and "symbol" in oldinfo and newinfo.
  void transform() {                                  
    int oldline, newline;
    int oldmax = oldFileInfo.maxLine + 2;  // Count pseudolines at
    int newmax = newFileInfo.maxLine + 2;  // ..front and rear of file

    for (oldline=0; oldline < oldmax; oldline++ ) {
      oldFileInfo.other[oldline]= -1;
    }
    for (newline=0; newline < newmax; newline++ ) {
      newFileInfo.other[newline]= -1;
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
  void scanUnique() {
    int oldline, newline;
    Node psymbol;

    for( newline = 1; newline <= newFileInfo.maxLine; newline++ ) {
      psymbol = newFileInfo.symbol[ newline ];
      if ( psymbol.symbolIsUnique()) {        // 1 use in each file
        oldline = psymbol.linenum;
        newFileInfo.other[ newline ] = oldline;   // record 1-1 map
        oldFileInfo.other[ oldline ] = newline;
      }
    }
    newFileInfo.other[ 0 ] = 0;
    oldFileInfo.other[ 0 ] = 0;
    newFileInfo.other[ newFileInfo.maxLine + 1 ] = oldFileInfo.maxLine + 1;
    oldFileInfo.other[ oldFileInfo.maxLine + 1 ] = newFileInfo.maxLine + 1;
  }

  //Expects both files in symtab, and oldinfo and newinfo valid.
  //Expects the "other" arrays contain positive #s to indicate lines that are unique in both files.
  //For each such pair of places, scans past in each file.
  //Contiguous groups of lines that match non-uniquely are taken to be good-enough matches, and so marked in "other".
  //Assumes each other[0] is 0.
  void scanAfter() {
    int oldline, newline;

    for( newline = 0; newline <= newFileInfo.maxLine; newline++ ) {
      oldline = newFileInfo.other[ newline ];
      if ( oldline >= 0 ) {   // is unique in old & new
        for(;;) {             // scan after there in both files
          if ( ++oldline > oldFileInfo.maxLine   ) {
            break; 
          }
          if ( oldFileInfo.other[ oldline ] >= 0 ) { 
            break;
          }
          if ( ++newline > newFileInfo.maxLine   ) { 
            break; 
          }
          if ( newFileInfo.other[ newline ] >= 0 ) { 
            break;
          }
          // oldline & newline exist, and aren't already matched

          if ( newFileInfo.symbol[ newline ] != oldFileInfo.symbol[ oldline ] ) { 
            break;  // not same
          }

          newFileInfo.other[newline] = oldline; // record a match
          oldFileInfo.other[oldline] = newline;
        }
      }
    }
  }

  //As scanafter, except scans towards file fronts.
  //Assumes the off-end lines have been marked as a match.
  void scanBefore() {
    int oldline, newline;

    for( newline = newFileInfo.maxLine + 1; newline > 0; newline-- ) {
      oldline = newFileInfo.other[ newline ];
      if ( oldline >= 0 ) {   // unique in each
        for(;;) {
          if ( --oldline <= 0 ) {
            break;
          }
          if ( oldFileInfo.other[ oldline ] >= 0 ) { 
            break;
          }
          if ( --newline <= 0 ) { 
            break;
          }
          if ( newFileInfo.other[ newline ] >= 0 ) { 
            break;
          }
          // oldline and newline exist, and aren't marked yet

          if ( newFileInfo.symbol[ newline ] != oldFileInfo.symbol[ oldline ] ) {
            break;  // not same
          }

          newFileInfo.other[newline] = oldline; // record a match
          oldFileInfo.other[oldline] = newline;
        }
      }
    }
  }

  //Finds the beginnings and lengths of blocks of matches.
  //Sets the blocklen array (see definition).
  //Expects oldinfo valid.
  void scanBlocks() {
    int oldline, newline;
    int oldfront = 0;      // line# of front of a block in old, or 0 
    int newlast = -1;      // newline's value during prev. iteration

    for( oldline = 1; oldline <= oldFileInfo.maxLine; oldline++ ) {
      blocklen[ oldline ] = 0;
    }
    blocklen[ oldFileInfo.maxLine + 1 ] = UNREAL; // starts a mythical blk

    for( oldline = 1; oldline <= oldFileInfo.maxLine; oldline++ ) {
      newline = oldFileInfo.other[ oldline ];
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
  // The following part will Calculate the Code Churn
  // 
  //////////////////////////////////////////////////////////////////////////////
  
  public static final int idle = 0, deleted = 1, added = 2, movenew = 3, moveold = 4, same = 5, changed = 6;
  int diffStatus;
  int churnStatus;
  boolean anyprinted;
  int currentLineOldFile, currentLineNewFile;     // line numbers in old & new file


  // Calculate Code Churn
  // Expects all data structures have been filled out.
  void calculateChurn() {
    diffStatus = idle;
    anyprinted = false;
    for( currentLineOldFile = currentLineNewFile = 1; ; ) {
      if ( currentLineOldFile > oldFileInfo.maxLine ) { 
        consumeNew(); 
        break;
      }
      if ( currentLineNewFile > newFileInfo.maxLine ) { 
        consumeOld(); 
        break;
      }
      if ( newFileInfo.other[ currentLineNewFile ] < 0 ) {
        if ( oldFileInfo.other[ currentLineOldFile ] < 0 )
          countChange();
        else
          countInsert();
      }
      else if ( oldFileInfo.other[ currentLineOldFile ] < 0 )
        countDelete();
      else if ( blocklen[ currentLineOldFile ] < 0 )
        skipOldBlock();
      else if ( oldFileInfo.other[ currentLineOldFile ] == currentLineNewFile )
        countSame();
      else
        countMove();
    }
    if ( anyprinted == true ) 
      System.out.println( ">>>> End of differences."  );
    else
      System.out.println( ">>>> Files are identical." );
  }
  
  // Expects currentLineOldFile is at a deletion.
  void countDelete() {
    if ( diffStatus != deleted ) {
      //System.out.println( ">>>> DELETE AT " + currentLineOldFile);
      System.out.println("DELETED");
    }
    diffStatus = deleted;
    oldFileInfo.symbol[ currentLineOldFile ].showSymbol();
    dlLOC++;
    anyprinted = true;
    currentLineOldFile++;
  }

  // Expects currentLineNewFile is at an add.
  void countInsert() {
    if ( diffStatus == changed ) {
      System.out.println( "CHANGED" );
      churnStatus = changed;
    }
    else if ( diffStatus != added ) { 
      System.out.println( "ADDED");
      churnStatus = added;
    }

    diffStatus = added;
    newFileInfo.symbol[ currentLineNewFile ].showSymbol();

    if (churnStatus == changed )
      chLOC++;
    else if (churnStatus == added)
      adLOC++;

    anyprinted = true;
    currentLineNewFile++;
  }

  // Expects currentLineNewFile is ADDED.
  // Expects currentLineOldFile is at a deletion.
  void countChange() {
    diffStatus = changed;
    anyprinted = true;
    currentLineOldFile++;
  }

  // Expects currentLineNewFile and currentLineOldFile at start of two blocks that aren't to be displayed.
  void countSame() {
    int count;
    diffStatus = idle;
    if ( newFileInfo.other[ currentLineNewFile ] != currentLineOldFile ) {
      System.err.println("BUG IN LINE REFERENCING");
      System.exit(1);
    }
    count = blocklen[ currentLineOldFile ];
    currentLineOldFile += count;
    currentLineNewFile += count;
  }

  // Expects currentLineOldFile, currentLineNewFile at start of two different blocks ( a move was done).
  void countMove() {
    int oldblock = blocklen[ currentLineOldFile ];
    int newother = newFileInfo.other[ currentLineNewFile ];
    int newblock = blocklen[ newother ];

    if ( newblock < 0 ) skipNewBlock(); // already printed.
    else if ( oldblock >= newblock ) {  // assume new's blk moved.
      blocklen[newother] = -1;          // stamp block as "printed".
      System.out.println( ">>>> " + newother + " THRU " + (newother + newblock - 1) + " MOVED TO BEFORE " + currentLineOldFile );
      for( ; newblock > 0; newblock--, currentLineNewFile++ )
      {
        newFileInfo.symbol[ currentLineNewFile ].showSymbol();
        chLOC++;
      }
      anyprinted = true;
      diffStatus = idle;

    } else        // assume old's block moved
      skipOldBlock();  // target line# not known, display later
  }
  
  // Skips over the old block.
  // Expects currentLineOldFile at start of an old block that has already been announced as a move.
  void skipOldBlock() {
    diffStatus = idle;
    for(;;) {
      if ( ++currentLineOldFile > oldFileInfo.maxLine )
        break;    // end of file
      if ( oldFileInfo.other[ currentLineOldFile ] < 0 )
        break;    // end of block
      if ( blocklen[ currentLineOldFile ]!=0)
        break;    // start of another
    }
  }

  // Skips over the new block.
  // Expects currentLineNewFile is at start of a new block that has already been announced as a move.
  void skipNewBlock() {
    int oldline;
    diffStatus = idle;
    for(;;) {
      if ( ++currentLineNewFile > newFileInfo.maxLine )
        break;    // end of file
      oldline = newFileInfo.other[ currentLineNewFile ];
      if ( oldline < 0 )
        break;    // end of block
      if ( blocklen[ oldline ] != 0)
        break;    // start of another
    }
  }
  
  // Have run out of old file. 
  // Print the rest of the new file, as inserts and/or moves.
  void consumeNew() {
    for(;;) {
      if ( currentLineNewFile > newFileInfo.maxLine )
        break;        // end of file
      if ( newFileInfo.other[ currentLineNewFile ] < 0 ) 
        countInsert();
      else
        countMove();
    }
  }

  // Have run out of new file.
  // Process the rest of the old file, printing any parts which were deletes or moves.
  void consumeOld() {
    for(;;) {
      if ( currentLineOldFile > oldFileInfo.maxLine )
        break;       // end of file
      currentLineNewFile = oldFileInfo.other[ currentLineOldFile ];
      if ( currentLineNewFile < 0 ) 
        countDelete();
      else if ( blocklen[ currentLineOldFile ] < 0 ) 
        skipOldBlock();
      else 
        countMove();
    }
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
  Node( String pline) {
    pleft = pright = null;
    linestate = freshnode;
    // linenum field is not always valid     
    line = pline;
  }

  // Searches tree for a match to the line.
  // Parameter: a line of text
  // If node's linestate == freshnode, then created the node.
  static Node matchsymbol( String pline ) {
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
  static Node addSymbol( String pline, boolean inoldfile, int linenum ) {
    Node pnode;
    pnode = matchsymbol( pline );  // find the node in the tree
    if ( pnode.linestate == freshnode ) {
      pnode.linestate = inoldfile ? oldonce : newonce;
    } else {
      if (( pnode.linestate == oldonce && !inoldfile ) || ( pnode.linestate == newonce &&  inoldfile )) {
        pnode.linestate = bothonce;
      }
      else {
        pnode.linestate = other;
      }
    }
    if (inoldfile) {
      pnode.linenum = linenum;
    }
    return pnode;
  }

  // Arg is a ptr previously returned by addSymbol.
  // Returns true if the line was added to the symbol table exactly once with inoldfile true, 
  // and exactly once with inoldfile false.
  boolean symbolIsUnique() {
    return (linestate == bothonce );
  }

  // Prints the line to stdout.
  void showSymbol() {
    System.out.println(line);
  }
}




