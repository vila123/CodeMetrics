import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SourceFile {
  private String fileName = "";
  private String filePath = "";

  private ArrayList<Method> methodList = null;

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

  // Total LOC
  // is the number of physical lines in the files comprising the new version
  // of a binary.

  // Churned LOC
  // is the sum of the added and changed lines of code between a baseline
  // version and a new version of the
  // files comprising a binary.

  // Deleted LOC
  // is the number of lines of code deleted between the baseline version and
  // the new version of a binary.
  // The churned LOC and the deleted LOC are computed by the version control
  // systems using a file comparison utility like diff.

  // Constructor
  SourceFile() {
    methodList = new ArrayList<Method>();
  }

  SourceFile(final String name, final String path) {
    methodList = new ArrayList<Method>();
    fileName = name;
    filePath = path;
  }

  // Implement smarter!
  public void print() {
    for (int j =0; j < methodList.size(); j++) {
      System.out.println("Method: " + methodList.get(j).getMethodName() + "\t Complexity: " +
      methodList.get(j).getComplexity());
    }
  }
  
  // Count Lines Of Code
  public void countLines() {
    try{
      FileInputStream fStream = new FileInputStream(getFilePath());
      DataInputStream dStream = new DataInputStream(fStream);
      BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));
      String strLine;

      //Read File Line By Line
      while ((strLine = bReader.readLine()) != null) {
        strLine = strLine.trim();

        // This is a Line of Code (total)
        LOC++;

        // This is an Empty Line (no visible characters)
        if (strLine.length() == 0) {
          emLOC++;
        }
        // Trivial Lines ( ´{´ or ´}´)
        else if (strLine.equals("{") || strLine.equals("}")) {
          trLOC++;
        }
        // Lines of Comments (single line of comment)
        else if (strLine.startsWith("//") || strLine.startsWith("/*") || strLine.startsWith("*")) {
          ccLOC++;
        }
        // Lines of Statements (ending with ';')
        else if (strLine.endsWith(";")) {
          stLOC++;
        }
        // Lines of Statements (methods, functions, conditions and statements)
        else {
          stLOC++;
        }
      }
      bReader.close();
      dStream.close();
    }catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }
  
  // Calculate the complexity for each method in the private ArrayList<Method>
  // (Need to populate the private ArrayList by using getMethods() first)
  public void countComplexity() {
    if (methodList.size() > 0) {
      for (Method method : methodList) {

        try{
          FileInputStream fStream = new FileInputStream(getFilePath());
          DataInputStream dStream = new DataInputStream(fStream);
          BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));

          // Go to the first line of this method
          for(int i = 1; i < method.getLineNr(); ++i) {
            bReader.readLine();
          }

          int complexity = complexityParser(bReader, method.getLineNr(), method.getLastLineNr());
          method.setComplexity(complexity);
          
          //Increase the total complexity
          CC = CC + complexity;
          
          bReader.close();
          dStream.close();
        }catch (Exception e) {
          System.err.println("Error: " + e.getMessage());
        }
      }
    }
  }

  // Start with a count of one for the method.
  // Add one for each of the following flow-related elements that are found in the method.

  // Methods    Each return that isn't the last statement of a method.

  // Selection  if, else, case, default.
  // Loops      for, while, do-while, break, and continue.
  // Operators  &&, ||, ?, and :
  // Exceptions try, catch, finally, throw, or throws clause.
  // Threads    start() call on a thread. Of course, this is a ridiculous underestimate!
  private int complexityParser(final BufferedReader bReader, final int firstLine, final int lastLine) throws IOException {
    int complexity = 1;
    String currentLine = "";

    // Read each line of this Method
    for (int i = firstLine; i < lastLine; i++) {
      currentLine = bReader.readLine();

      currentLine = currentLine.trim(); // Trim leading and trailing whitespace

      // Ignore: This is an Empty Line (no visible characters)
      if (currentLine.length() == 0) {
        // Do nothing, empty line
      }
      // Ignore: This is a Trivial Line ( ´{´ or ´}´)
      else if (currentLine.equals("{") || currentLine.equals("}")) {
	// Do nothing, trivial line
      }
      // Ignore: This is a Line of Comments (single line of comment)
      else if (currentLine.startsWith("//") || currentLine.startsWith("/*") || currentLine.startsWith("*")) {
	// Do nothing, comment line
      }
      else {
        currentLine = trimComments(currentLine); // Trim tailing comments

        complexity = complexity + countSelection(currentLine);
        complexity = complexity + countLoops(currentLine);
        complexity = complexity + countOperators(currentLine);
        complexity = complexity + countExceptions(currentLine);
        complexity = complexity + countThreads(currentLine);
      }
    }

    return complexity;
  }

  // Selection if, else, case, default.
  private int countSelection(final String currentLine) {
    int count = 0;

    count = count + countOccurences(currentLine, "if");
    count = count + countOccurences(currentLine, "else");
    count = count + countOccurences(currentLine, "case");
    count = count + countOccurences(currentLine, "default");
    return count;
  }

  // Loops for, while, do-while, break, and continue.
  private int countLoops(final String currentLine) {
    int count = 0;

    count = count + countOccurences(currentLine, "for");
    count = count + countOccurences(currentLine, "while");
    count = count + countOccurences(currentLine, "do");
    count = count + countOccurences(currentLine, "break");
    count = count + countOccurences(currentLine, "continue");
    return count;
  }

  // Operators &&, ||, ?, and :
  private int countOperators(final String currentLine) {
    int count = 0;

    count = count + countOccurences(currentLine, "&&");
    count = count + countOccurences(currentLine, "||");
    count = count + countOccurences(currentLine, "?");
    count = count + countOccurences(currentLine, ":");
    return count;
  }

  // Exceptions try, catch, finally, throw, or throws clause.
  private int countExceptions(final String currentLine) {
    int count = 0;

    count = count + countOccurences(currentLine, "try");
    count = count + countOccurences(currentLine, "catch");
    count = count + countOccurences(currentLine, "finally");
    count = count + countOccurences(currentLine, "throw");
    count = count + countOccurences(currentLine, "throws");
    return count;
  }

  // Threads start() call on a thread. Of course, this is a ridiculous underestimate!
  private int countThreads(final String currentLine) {
    int count = 0;

    // TODO: Implement later
    return count;
  }

  private int countOccurences(final String currentLine, final String findStr) {
    int count = 0;
    int lastIndex = 0;

    while (lastIndex != -1) {
      lastIndex = currentLine.indexOf(findStr, lastIndex);

      if (lastIndex != -1) {
        // Make sure the position not is within quotation marks
        if (!inQuotation(currentLine, lastIndex)) {
          count++;
        }
        lastIndex += findStr.length();
      }
    }
    return count;
  }

  // Count the number of lines in this method
  private int getLastMethodLine(String firstLine, final BufferedReader bReader) {
    String strLine = firstLine;
    boolean endOfMethod = false;
    int functionDepth = 0;
    int linesOfCode = 0;

    while (!endOfMethod) {
        functionDepth = functionDepth + getIndentationDepth(strLine);

        if (functionDepth == 0 && linesOfCode > 1) {
          // We have reached the end of the method
          endOfMethod = true;
          return linesOfCode;
        }
        try {
	    strLine = bReader.readLine();
	    linesOfCode++;
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    return linesOfCode;
  }

  // Increase or decrease the indentation level
  private int getIndentationDepth(final String strLine) {
    int depth = 0;
    for (int i = 0; i < strLine.length(); i++) {
      char c = strLine.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
    }
    return depth;
  }

  // Populate the private ArrayList methods
  public void getMethods() {
    int firstLine = 0;
    try {
      FileInputStream fStream = new FileInputStream(getFilePath());
      DataInputStream dStream = new DataInputStream(fStream);
      BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));
      String strLine;
      
      // Read File Line By Line
      while ((strLine = bReader.readLine()) != null) {
        firstLine++;
        strLine = trimComments(strLine); // Trim tailing comments
        strLine = strLine.trim(); // Trim leading and trailing whitespace

        if (strLine.length() != 0 && !strLine.endsWith(";") && Character.isLetter(strLine.charAt(0))) {
          if (isMethod(strLine)) {
            int lastLine = firstLine + getLastMethodLine(strLine, bReader);
            
            // Add it to the private ArrayList method
            Method method = new Method(firstLine, lastLine, strLine);
            methodList.add(method);
            firstLine = lastLine;
          }
        }
      }
      bReader.close();
      dStream.close();
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }

  // Is this a method declaration?
  private boolean isMethod(final String strLine) {
    if (!isSelections(strLine) && !isLoops(strLine) && !isExceptions(strLine) && !strLine.contains("class ")) {
      return verifyMethod(strLine);
    }
    return false;
  }
  
  // Is this really a method declaration?
  private boolean verifyMethod (final String strLine) {
      if (!strLine.contains("(")) {
	  return false;
      }
      return true;
  }

  // Does line start with if, else, case, default?
  private boolean isSelections(final String strLine) {
    if (strLine.startsWith("if") || strLine.startsWith("else") || strLine.startsWith("case") || strLine.startsWith("default")) {
      return true;
    }
    return false;
  }

  // Does line start with for, while, do, break, continue?
  private boolean isLoops(final String strLine) {
    if (strLine.startsWith("for") || strLine.startsWith("while") || strLine.startsWith("do") || strLine.startsWith("break")
        || strLine.startsWith("continue")) {
      return true;
    }
    return false;
  }

  // Douse line start with catch, finally, throw, throws?
  private boolean isExceptions(final String strLine) {
    if (strLine.startsWith("try") || strLine.startsWith("catch") || strLine.startsWith("finally") || strLine.startsWith("throw")
        || strLine.startsWith("throws")) {
      return true;
    }
    return false;
  }

  // Remove tailing comment if there is any
  private String trimComments(final String strLine) {

    int index = strLine.indexOf("//");

    // No comments in this line, return the line
    if (index == -1) { // We have to ignore "//" (not a comment)
      return strLine;
    }

    while (inQuotation(strLine, index) && index != -1) {
      // System.out.println(index + strLine);
      index = strLine.indexOf("//", index + 2);
    }

    if (index == -1) {
      // No comments
      return strLine;
    }
    // End of line comment
    return strLine.substring(0, index);
  }

  // Check if the position is within quotation marks
  private boolean inQuotation(final String strLine, final int pos) {
    int quotes = 0;
    // Count the number of quotation marks to the left of this position
    for (int i = pos; 0 <= i; i--) {
      if (strLine.charAt(i) == '"') {
        quotes++;
      }
    }

    // If the number is uneven, we within quotation
    if (quotes % 2 == 1) {
      return true;
    }
    return false;
  }

  // Source file attributes

  public void setFileName(final String name) {
    fileName = name;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFilePath(final String path) {
    filePath = path;
  }

  public String getFilePath() {
    return filePath;
  }

  // Return the total number of methods
  public int getNrOfMethods() {
    return methodList.size();
  }

  public Method getMethod(final int index) {
    return methodList.get(index);
  }
  
  // LOC Metrics
  public int getLinesOfCode() {
    return LOC;
  }

  public int getLinesOfStatements() {
    return stLOC;
  }

  public int getLinesOfComments() {
    return ccLOC;
  }

  public int getTrivialLines() {
    return trLOC;
  }

  public int getEmptyLines() {
    return emLOC;
  }
  
  // Complexity mertics
  public float getComplexity() {
      return (float)CC;
  }
  
  public float getAvgComplexity() {
    if (CC != 0) {
      return (float)CC/methodList.size();
    }
    return (float) 0;
}

  // Code Churn Metrics
  public int getAddedLines() {
    return adLOC;
  }

  public int getChangedLines() {
    return chLOC;
  }

  public int getDeletedLines() {
    return dlLOC;
  }

  public int getCodeChurn() {
    int codeChurn = adLOC + chLOC + dlLOC;
    return codeChurn;
  }

  public void setAddedLines(int value) {
    adLOC = value;
  }

  public void setChangedLines(int value) {
    chLOC = value;
  }

  public void setDeletedLines(int value) {
    dlLOC = value;
  }
}