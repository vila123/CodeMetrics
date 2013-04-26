// CodeMetrics
// TODO: Support excluding files or folders. This is useful if parts of your source code was generated by a tool.
// -e PATH1_TO_EXCLUDE;PATH2_TO_EXCLUDE
// TODO: Support for defining components/modules
// -m module1=PATH1;PATH2 module2=PATH3
// TODO: Support for Java, C#, C++, XML
// TODO: Output to .csv file

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
      boolean calculateCodeChurn = false;
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
	  calculateCodeChurn = true;

	  if (oldFile.isDirectory() && newFile.isDirectory()) {
	      // Two directories
	  }
	  else if (oldFile.isFile() && newFile.isFile()) {
	      // Two files
	      codeMetrics.oldFiles.addSrcFile(oldFile);
	      codeMetrics.newFiles.addSrcFile(newFile);
	  }
	  else {
	      System.out.println(usage);
	      System.exit(1);
	  }
      }

      // Calculate Code Churn
      if (calculateCodeChurn) {
	  codeMetrics.countChurn(codeMetrics.oldFiles, codeMetrics.newFiles);
      }

      // Calculate Cyclomatic Complexity
      codeMetrics.countComplexity(codeMetrics.newFiles);

      // Count LOC (Lines of Code)
      codeMetrics.countLines(codeMetrics.newFiles);

      // Print LOC information
      for (int i = 0; i < codeMetrics.newFiles.getNrOfFiles(); i++) {
	  System.out.println(codeMetrics.newFiles.getSrcFile(i).getFileName());
	  System.out.println("\t Total Lines of Code: " + codeMetrics.newFiles.getSrcFile(i).getLinesOfCode()); 
	  System.out.println("\t Executable Lines: " + codeMetrics.newFiles.getSrcFile(i).getLinesOfStatements());
	  System.out.println("\t Lines of Comments: " + codeMetrics.newFiles.getSrcFile(i).getLinesOfComments()); 
	  System.out.println("\t Trivial Lines: " + codeMetrics.newFiles.getSrcFile(i).getTrivialLines()); 
	  System.out.println("\t Empty Lines: " + codeMetrics.newFiles.getSrcFile(i).getEmptyLines()); 
	  System.out.println("\t Code Complexity: " + codeMetrics.newFiles.getSrcFile(i).getComplexity());
	  System.out.println("\t Number of Methods: " + codeMetrics.newFiles.getSrcFile(i).getNrOfMethods());
	  System.out.println("\t Average Method Complexity: " + codeMetrics.newFiles.getSrcFile(i).getAvgComplexity());
	  System.out.println("\t Comment Percentage: " + (100 * codeMetrics.newFiles.getSrcFile(i).getLinesOfComments()) / codeMetrics.newFiles.getSrcFile(i).getLinesOfCode() + "%");
	  // Recommendations: Code where the percentage of comment is lower than 20% should be more commented. 
	  // However overly commented code (>40%) is more difficult to read.  
	  if (calculateCodeChurn) {
	      System.out.println("\t Added Lines of Code: " + codeMetrics.newFiles.getSrcFile(i).getAddedLines());
	      System.out.println("\t Changed Lines of Code: " + codeMetrics.newFiles.getSrcFile(i).getChangedLines());
	      System.out.println("\t Deleted Lines of Code: " + codeMetrics.newFiles.getSrcFile(i).getDeletedLines());
	      System.out.println("\t Code Churn: " + codeMetrics.newFiles.getSrcFile(i).getCodeChurn());
	  }

      }
      System.out.println("Total (Aggregated Metrics)");
      System.out.println("\t Total Lines of Code: " + codeMetrics.newFiles.getLinesOfCode()); 
      System.out.println("\t Executable Lines: " + codeMetrics.newFiles.getLinesOfStatements());
      System.out.println("\t Lines of Comments: " + codeMetrics.newFiles.getLinesOfComments()); 
      System.out.println("\t Trivial Lines: " + codeMetrics.newFiles.getTrivialLines()); 
      System.out.println("\t Empty Lines: " + codeMetrics.newFiles.getEmptyLines()); 
      System.out.println("\t Code Complexity: " + codeMetrics.newFiles.getComplexity());
      System.out.println("\t Number of Files: " + codeMetrics.newFiles.getNrOfFiles());
      System.out.println("\t Average File Complexity: " + codeMetrics.newFiles.getAvgComplexity());
      System.out.println("\t Comment Percentage: " + (100 * codeMetrics.newFiles.getLinesOfComments()) / codeMetrics.newFiles.getLinesOfCode() + "%");
      // Recommendations: Code where the percentage of comment is lower than 20% should be more commented. 
      // However overly commented code (>40%) is more difficult to read.  
	  if (calculateCodeChurn) {
	      System.out.println("\t Added Lines of Code: " + codeMetrics.newFiles.getAddedLines());
	      System.out.println("\t Changed Lines of Code: " + codeMetrics.newFiles.getChangedLines());
	      System.out.println("\t Deleted Lines of Code: " + codeMetrics.newFiles.getDeletedLines());
	      System.out.println("\t Code Churn: " + codeMetrics.newFiles.getCodeChurn());
	  }
  }

  public void countChurn(final SourceFiles oldFiles, final SourceFiles newFiles) {
    for (int i = 0; i < newFiles.getNrOfFiles(); i++) {
      Diff d = new Diff();
      d.countChurn(oldFiles.getSrcFile(i), newFiles.getSrcFile(i));

//      d.doDiff(oldFiles.getSrcFile(i).getFilePath(), newFiles.getSrcFile(i).getFilePath());
//      d.calculateChurn();
    }
  }
  
  public void countComplexity(final SourceFiles srcFiles) {
    for (int i = 0; i < srcFiles.getNrOfFiles(); i++) {
      //System.out.println(srcFiles.getSrcFile(i).getFilePath());

      srcFiles.getSrcFile(i).getMethods();
      srcFiles.getSrcFile(i).countComplexity();
      //srcFiles.getSrcFile(i).print();
    }
  }

  public void countLines(final SourceFiles srcFiles) {
    for (int i = 0; i < srcFiles.getNrOfFiles(); i++) {
      //System.out.println(srcFiles.getSrcFile(i).getFilePath());

      srcFiles.getSrcFile(i).countLines();
    }
  }
}