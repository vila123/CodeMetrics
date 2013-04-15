// CodeMetrics
// TODOD: Support excluding files or folders
// TODOD: Support for defining components/modules
// TODOD: Support for Java, C#, C++, XML

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class CodeMetrics {
  private SourceFiles srcFiles = null;

  CodeMetrics() {
    srcFiles = new SourceFiles();
  }

  public static void main(final String[] args) {
    // Check if we have a parameter
    if (args.length == 0 ) {
      System.out.println("ERROR: You need to give the path as argument!");
      return;
    }

    // Check if the parameter is a directory
    File sDir = new File(args[0]);
    if (!sDir.isDirectory()) {
      System.out.println("ERROR: Parameter one needs to be a directory");
      return;
    }

    CodeMetrics codeMetrics = new CodeMetrics();
    codeMetrics.srcFiles.parseSrcDir(sDir);

    // Calculate Cyclomatic Complexity
    codeMetrics.countComplexity(codeMetrics.srcFiles);

    // Count LOC (Lines of Code)
    codeMetrics.countLines(codeMetrics.srcFiles);

    // Print LOC information
    for (int i = 0; i < codeMetrics.srcFiles.getNrOfFiles(); i++) {
      System.out.println(codeMetrics.srcFiles.getSrcFile(i).getFileName() + "LOC: \t" + codeMetrics.srcFiles.getSrcFile(i).getLinesOfCode() + 
          " stLOC: " + codeMetrics.srcFiles.getSrcFile(i).getLinesOfStatements() + 
          " ccLOC: " + codeMetrics.srcFiles.getSrcFile(i).getLinesOfComments() + 
          " trLOC: " + codeMetrics.srcFiles.getSrcFile(i).getTrivialLines() + 
          " emLOC: " + codeMetrics.srcFiles.getSrcFile(i).getEmptyLines() + 
          " CC: " + codeMetrics.srcFiles.getSrcFile(i).getComplexity());
    }
    System.out.println("Total LOC: \t" + codeMetrics.srcFiles.getLinesOfCode() + 
        " stLOC: " + codeMetrics.srcFiles.getLinesOfStatements() +
        " ccLOC: " + codeMetrics.srcFiles.getLinesOfComments() + 
        " trLOC: " + codeMetrics.srcFiles.getTrivialLines() + 
        " emLOC: " + codeMetrics.srcFiles.getEmptyLines());
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

      try{
        FileInputStream fStream = new FileInputStream(srcFiles.getSrcFile(i).getFilePath());
        DataInputStream dStream = new DataInputStream(fStream);
        BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));
        String strLine;

        //Read File Line By Line
        while ((strLine = bReader.readLine()) != null) {
          strLine = strLine.trim();

          // This is a Line of Code (total)
          srcFiles.getSrcFile(i).lineOfCode();

          // This is an Empty Line (no visible characters)
          if (strLine.length() == 0) {
            srcFiles.getSrcFile(i).emptyLine();
          }
          // Trivial Lines ( ´{´ or ´}´)
          else if (strLine.equals("{") || strLine.equals("}")) {
            srcFiles.getSrcFile(i).trivialLine();
          }
          // Lines of Comments (single line of comment)
          else if (strLine.startsWith("//") || strLine.startsWith("/*") || strLine.startsWith("*")) {
            srcFiles.getSrcFile(i).lineOfComment();
          }
          // Lines of Statements (ending with ';')
          else if (strLine.endsWith(";")) {
            srcFiles.getSrcFile(i).lineOfStatement();
          }
          // Lines of Statements (methods, functions, conditions and statements)
          else {
            srcFiles.getSrcFile(i).lineOfStatement();
            // System.out.println(strLine);
          }
        }
        bReader.close();
        dStream.close();
      }catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
      }
    }
  }
}
