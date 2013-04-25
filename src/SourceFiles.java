import java.io.File;
import java.util.ArrayList;

public class SourceFiles {
  ArrayList<SourceFile> srcList = null;

  // LOC Metric
  private int LOC = 0;   // Lines of Code (total)
  private int ccLOC = 0; // Lines of Comments (single line of comment
  private int trLOC = 0; // Trivial Lines ( ´{´ or ´}´)
  private int emLOC = 0; // Empty Lines (no visible characters)
  private int stLOC = 0; // Lines of Statements (ending with ';')

  // Cyclomatic Complexity
  private int CC = 0;

  // Code Churn
  private int adLOC = 0; // Added Lines of Code
  private int chLOC = 0; // Changed Lines of Code
  private int dlLOC = 0; // Deleted Lines of Code

  // Constructor
  SourceFiles() {
    srcList = new ArrayList<SourceFile>();
  }

  // Parse a file path and find all source files
  public void parseSrcDir(final File sDir) {
    File[] faFiles = new File(sDir.getAbsolutePath()).listFiles();

    for (File file : faFiles) {
      if (file.getName().endsWith("java") && file.isFile()) {
        // System.out.println(file.getName());

        SourceFile javaSource = new SourceFile(file.getName(), file.getAbsolutePath());
        srcList.add(javaSource);
      }
      if (file.isDirectory()) {
        parseSrcDir(file);
      }
    }
  }
  
  // Only work with one source file 
  public void addSrcFile(final File sFile) {
    if (sFile.getName().endsWith("java") && sFile.isFile()) {
      SourceFile javaSource = new SourceFile(sFile.getName(), sFile.getAbsolutePath());
      srcList.add(javaSource);
    }
    else {
      System.out.println("ERROR: File type is not yet supported. Only support for java files.");
      System.exit(1);
    }
  }

  // Return the total number of source code files
  public int getNrOfFiles() {
    return srcList.size();
  }

  public SourceFile getSrcFile(final int index) {
    return srcList.get(index);
  }

  // LOC Metrics
  public int getLinesOfCode() {
    for (SourceFile srcFile : srcList) {
      LOC += srcFile.getLinesOfCode();
    }
    return LOC;
  }

  public int getLinesOfStatements() {
    for (SourceFile srcFile : srcList) {
      stLOC += srcFile.getLinesOfStatements();
    }
    return stLOC;
  }

  public int getLinesOfComments() {
    for (SourceFile srcFile : srcList) {
      ccLOC += srcFile.getLinesOfComments();
    }
    return ccLOC;
  }

  public int getTrivialLines() {
    for (SourceFile srcFile : srcList) {
      trLOC += srcFile.getTrivialLines();
    }
    return trLOC;
  }

  public int getEmptyLines() {
    for (SourceFile srcFile : srcList) {
      emLOC += srcFile.getEmptyLines();
    }
    return emLOC;
  }

  // Complexity mertics
  public float getComplexity() {
    for (SourceFile srcFile : srcList) {
      CC += srcFile.getComplexity();
    }
    return (float)CC;
  }
  
  public float getAvgComplexity() {
    if (CC != 0) {
      return (float)CC/srcList.size();
    }
    return (float) 0;
}
  
  // Code Churn Metrics

}
