package se.prolore.metrics;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ComplexityParser {
	
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
	  private File sourceFile;
	  private JavaFileType filetype;
	  
	  private ArrayList<Method> methodList = null;
	  
	  public ComplexityParser(File sourceFile){
		this.sourceFile = sourceFile;
		methodList = new ArrayList<Method>();
		filetype = new JavaFileType();
		  
	  }
	  
	// Count Lines Of Code
	  public void countLines() {
	    try{
	      FileInputStream fStream = new FileInputStream(sourceFile.getAbsolutePath());
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
	        else if (filetype.lineIsTrivial(strLine)) {
	          trLOC++;
	        }
	        // Lines of Comments (single line of comment)
	        else if (filetype.isLineComment(strLine)) {
	          ccLOC++;
	        }
	        // Lines of Statements (ending with ';')
	        else if (filetype.isLineStatement(strLine)) {
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
	    if (getNrOfMethods() > 0) {
	      for (int i=0; i < getNrOfMethods(); i++) {
	    	  Method method = getMethod(i);
	        try{
	          FileInputStream fStream = new FileInputStream(sourceFile.getAbsolutePath());
	          DataInputStream dStream = new DataInputStream(fStream);
	          BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));

	          // Go to the first line of this method
	          for(int j = 1; j < method.getLineNr(); ++j) {
	            bReader.readLine();
	          }

	          int complexity = method.calculateMethodComplexity(bReader, method.getLineNr(), method.getLastLineNr());
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
	      return (float)CC/getNrOfMethods();
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
	  
	  // Populate the private ArrayList methods
	  public void parseMethods() {
	    int firstLine = 0;
	    try {
	      FileInputStream fStream = new FileInputStream(sourceFile.getAbsolutePath());
	      DataInputStream dStream = new DataInputStream(fStream);
	      BufferedReader bReader = new BufferedReader(new InputStreamReader(dStream));
	      String strLine;
	      
	      // Read File Line By Line
	      while ((strLine = bReader.readLine()) != null) {
	        firstLine++;
	        strLine = trimComments(strLine); // Trim tailing comments
	        strLine = strLine.trim(); // Trim leading and trailing whitespace

	        if (strLine.length() != 0 && !filetype.isLineStatement(strLine) && Character.isLetter(strLine.charAt(0))) {
	          if (filetype.isMethod(strLine)) {
	            int lastLine = firstLine + getLastMethodLine(strLine, bReader);
	            
	            // Add it to the private ArrayList method
	            Method method = new Method(firstLine, lastLine, strLine, filetype);
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
	  
	  // Return the total number of methods
	  public int getNrOfMethods() {
	    return methodList.size();
	  }

	  public Method getMethod(final int index) {
	    return methodList.get(index);
	  }
	  // Implement smarter!
	  public void print() {
	    for (int j =0; j < methodList.size(); j++) {
	      System.out.println("Method: " + methodList.get(j).getMethodName() + "\t Complexity: " +
	      methodList.get(j).getComplexity());
	    }
	  }

	public File getSourceFile() {
		return sourceFile;
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

}
