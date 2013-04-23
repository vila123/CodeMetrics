// CodeMetrics
// TODOD: Support excluding files or folders
// -e PATH1_TO_EXCLUDE;PATH2_TO_EXCLUDE
// TODOD: Support for defining components/modules
// -m module1=PATH1;PATH2 module2=PATH3
// TODOD: Support for Java, C#, C++, XML

import java.io.File;

public class CodeMetrics {
  private SourceFiles oldFiles = null;
  private SourceFiles newFiles = null;
  static private String usage =
      "Usage: CodeMetrics file [-options]\n" +
      "  (to calculate metrics for a single source file)\n" +
      "or  CodeMetrics path [-options]\n" +
      "  (to traverse a directory and calculate metrics for all source files)\n" +
      "or  CodeMetrics oldfile newfile [-options]\n" +
      "  (to calculate metrics including code churn)\n" +
      "\n" +
      "where options include:\n" +
      "  -ignoremove   Igmore moved code. Default behaviour is to count moved code as changed.\n" +
      "  -verbose      Output more detailed metrics\n";

  CodeMetrics() {
    oldFiles = new SourceFiles();
    newFiles = new SourceFiles();
  }

  public static void main(final String[] args) {
    CodeMetrics codeMetrics = new CodeMetrics();
    File oldFile = null;
    File newFile = null;
    
    // Check if we have a parameter
    if (args.length == 0) {  
      System.out.println(usage);
      System.exit(1);
    }
    // One parameter (Run Code Metrics without Code Churn)
    else if (args.length == 1) {
      newFile = new File(args[0]);
      
      if (newFile.isDirectory()) {
        // One directory
        codeMetrics.newFiles.parseSrcDir(newFile);
      }
      else if (newFile.isFile()) {
        // One file
        codeMetrics.newFiles.addSrcFile(newFile);        
      }
      else {
        System.out.println(usage);
        System.exit(1);
      }
    }
    // Two parameters calculate all Code Metrics
    else if (args.length == 2) {
      oldFile = new File(args[0]);
      newFile = new File(args[1]);

      if (oldFile.isDirectory() && newFile.isDirectory()) {
        // Two directories
      }
      else if (oldFile.isFile() && newFile.isFile()) {
        // Two files
        codeMetrics.newFiles.addSrcFile(oldFile);
        codeMetrics.newFiles.addSrcFile(newFile);
      }
      else {
        System.out.println(usage);
        System.exit(1);
      }
    }
    
    // Calculate Code Churn
    if (oldFile != null && newFile != null) {
      codeMetrics.countChurn(codeMetrics.oldFiles, codeMetrics.newFiles);
    }

    // Calculate Cyclomatic Complexity
    codeMetrics.countComplexity(codeMetrics.newFiles);

    // Count LOC (Lines of Code)
    codeMetrics.countLines(codeMetrics.newFiles);

    // Print LOC information
    for (int i = 0; i < codeMetrics.newFiles.getNrOfFiles(); i++) {
      System.out.println(codeMetrics.newFiles.getSrcFile(i).getFileName() + "LOC: \t" + codeMetrics.newFiles.getSrcFile(i).getLinesOfCode() + 
          " stLOC: " + codeMetrics.newFiles.getSrcFile(i).getLinesOfStatements() + 
          " ccLOC: " + codeMetrics.newFiles.getSrcFile(i).getLinesOfComments() + 
          " trLOC: " + codeMetrics.newFiles.getSrcFile(i).getTrivialLines() + 
          " emLOC: " + codeMetrics.newFiles.getSrcFile(i).getEmptyLines() + 
          " CC: " + codeMetrics.newFiles.getSrcFile(i).getComplexity());
    }
    System.out.println("Total LOC: \t" + codeMetrics.newFiles.getLinesOfCode() + 
        " stLOC: " + codeMetrics.newFiles.getLinesOfStatements() +
        " ccLOC: " + codeMetrics.newFiles.getLinesOfComments() + 
        " trLOC: " + codeMetrics.newFiles.getTrivialLines() + 
        " emLOC: " + codeMetrics.newFiles.getEmptyLines());
    
    
    // Calculate Code Churn
//    Diff d = new Diff();
//    d.doDiff(args[0], args[1]);
//    d.countChurn();
//
//    // Print Code Churn information
//    d.cChurn = d.adLOC+d.chLOC+d.dlLOC;
//    System.out.println("Added Lines of Code: " + d.adLOC);
//    System.out.println("Changed Lines of Code:" + d.chLOC);
//    System.out.println("Deleted Lines of Code: " + d.dlLOC);
//    System.out.println("Total Code Churn: " + d.cChurn);
  }

  public void countChurn(final SourceFiles oldFiles, final SourceFiles newFiles) {
    for (int i = 0; i < newFiles.getNrOfFiles(); i++) {
      // oldFiles.getSrcFile(i).getFilePath();
      // newFiles.getSrcFile(i)getFilePath();
      
      Diff d = new Diff();
      d.doDiff(oldFiles.getSrcFile(i).getFilePath(), newFiles.getSrcFile(i).getFilePath());
      d.countChurn();
    }
  }
  
  public void countComplexity(final SourceFiles srcFiles) {
    for (int i = 0; i < srcFiles.getNrOfFiles(); i++) {
      //System.out.println(srcFiles.getSrcFile(i).getFilePath());

      srcFiles.getSrcFile(i).getMethods();
      srcFiles.getSrcFile(i).countComplexity();
    }
  }

  public void countLines(final SourceFiles srcFiles) {
    for (int i = 0; i < srcFiles.getNrOfFiles(); i++) {
      //System.out.println(srcFiles.getSrcFile(i).getFilePath());

      srcFiles.getSrcFile(i).countLines();
    }
  }
}