package se.prolore.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class Method {
  // McCabe proposed a way to measuring flow complexity of a method which basically counts
  // one for each place where the flow changes from a linear flow.
  // His algorithm, translated, at least approximately, into Java terms is as follows.
  // His measurement was designed before exceptions and threads were used in programming languages,
  // so what I've added I believe reflects some of the original intent.
  //
  // Start with a count of one for the method.
  // Add one for each of the following flow-related elements that are found in the method.

  // Methods    Each return that isn't the last statement of a method.
  // Selection  if, else, case, default.
  // Loops      for, while, do-while, break, and continue.
  // Operators  &&, ||, ?, and :
  // Exceptions try, catch, finally, throw, or throws clause.
  // Threads    start() call on a thread. Of course, this is a ridiculous underestimate!

  private String methodName = "";
  private int lineNr = 0;
  private int lastLineNr = 0;
  
  // Cyclomatic Complexity
  private int CC = 0;
  private JavaFileType filetype;


  // Constructor
  Method(final String name) {
    methodName = name;
  }

  Method(final int firstLine, final int lastLine, final String name, final JavaFileType fileType) {
    lineNr = firstLine;
    lastLineNr = lastLine;
    methodName = name;
	this.filetype = fileType;
  }

  public void setMethodName(final String name) {
    methodName = name;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setLineNr(final int nr) {
    lineNr = nr;
  }

  public int getLineNr() {
    return lineNr;
  }
  
  public int getLastLineNr() {
      return lastLineNr;
  }

  public void setComplexity(final int complexity) {
    CC = complexity;
  }

  public int getComplexity() {
    return CC;
  }
  
  // Start with a count of one for the method.
  // Add one for each of the following flow-related elements that are found in the method.

  // Methods    Each return that isn't the last statement of a method.

  // Selection  if, else, case, default.
  // Loops      for, while, do-while, break, and continue.
  // Operators  &&, ||, ?, and :
  // Exceptions try, catch, finally, throw, or throws clause.
  // Threads    start() call on a thread. Of course, this is a ridiculous underestimate!
  public int calculateMethodComplexity(final BufferedReader bReader, final int firstLine, final int lastLine) throws IOException {
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
      else if (filetype.lineIsTrivial(currentLine)) {
	// Do nothing, trivial line
      }
      // Ignore: This is a Line of Comments (single line of comment)
      else if (filetype.isLineComment(currentLine)) {
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
    List<String> selectionKeyWords = filetype.getSelectionKeyWords();
    for (String keyword : selectionKeyWords) {
		count = count + countOccurences(currentLine, keyword);
	}
    return count;
  }

  // Loops for, while, do-while, break, and continue.
  private int countLoops(final String currentLine) {
    int count = 0;
    
    List<String> loopKeyWords = filetype.getLoopKeyWords();
    for (String keyword : loopKeyWords) {
		count = count + countOccurences(currentLine, keyword);
	}
    return count;
  }

  // Operators &&, ||, ?, and :
  private int countOperators(final String currentLine) {
    int count = 0;
    List<String> operators = filetype.getOperators();
    for (String operator : operators) {
		count = count + countOccurences(currentLine, operator);
	}
    return count;
  }

  // Exceptions try, catch, finally, throw, or throws clause.
  private int countExceptions(final String currentLine) {
    int count = 0;

    List<String> exceptions = filetype.getExceptions();
    for (String keyword : exceptions) {
		count =  count + countOccurences(currentLine, keyword);
	}
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
  
  // Remove tailing comment if there is any
  private String trimComments(final String strLine) {

    int index = strLine.indexOf(filetype.getLineComment());

    // No comments in this line, return the line
    if (index == -1) { // We have to ignore "//" (not a comment)
      return strLine;
    }

    while (inQuotation(strLine, index) && index != -1) {
      // System.out.println(index + strLine);
      index = strLine.indexOf(filetype.getLineComment(), index + 2);
    }

    if (index == -1) {
      // No comments
      return strLine;
    }
    // End of line comment
    return strLine.substring(0, index);
  }	 
}
