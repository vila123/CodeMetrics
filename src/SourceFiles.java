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

  // Code Churn
  private final int adLOC = 0; // Added Lines of Code
  private final int chLOC = 0; // Changed Lines of Code
  private final int dlLOC = 0; // Deleted Lines of Code

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

  // Code Churn Metrics

}