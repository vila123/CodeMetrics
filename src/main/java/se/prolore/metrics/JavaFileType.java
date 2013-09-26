package se.prolore.metrics;

import java.util.ArrayList;
import java.util.List;

public class JavaFileType {

	private List<String> selectionKeyWords;
	private List<String> loopKeyWords;
	private List<String> operators;
	private List<String> exceptions;
	
	
	public JavaFileType() {
		selectionKeyWords = new ArrayList<String>();
		selectionKeyWords.add("if");
		selectionKeyWords.add("else");
		selectionKeyWords.add("case");
		selectionKeyWords.add("default");
		
		loopKeyWords = new ArrayList<String>();
		loopKeyWords.add("for");
		loopKeyWords.add("while");
		loopKeyWords.add("do");
		loopKeyWords.add("break");
		loopKeyWords.add("continue");
		
		operators = new ArrayList<String>();
		operators.add("&&");
		operators.add("||");
		operators.add("?");
		operators.add(":");
		
		exceptions = new ArrayList<String>();
		exceptions.add("catch");
		exceptions.add("try");
		exceptions.add("finally");
		exceptions.add("throw");
		exceptions.add("throws");		
	}

	public boolean isLineStatement(String strLine) {
		return strLine.endsWith(";");
	}

	public boolean isLineComment(String strLine) {
		return strLine.startsWith("//") || strLine.startsWith("/*") || strLine.startsWith("*");
	}

	public boolean lineIsTrivial(String strLine) {
		return strLine.equals("{") || strLine.equals("}");
	}
	
	public List<String> getSelectionKeyWords(){
		return selectionKeyWords;
	}
	
	public List<String> getLoopKeyWords(){
		return loopKeyWords;
	}
	
	public List<String> getOperators(){
		return operators;
	}
	
	public List<String> getExceptions(){
		return exceptions;
	}
	
	public boolean isSelectionKeyWord(String keyword) {
		return selectionKeyWords.contains(keyword);
	}
	
	  // Douse line start with catch, finally, throw, throws?
	  private boolean isExceptions(final String strLine) {
	    if (exceptions.contains(strLine)) {
	      return true;
	    }
	    return false;
	  }	
	
	  // Does line start with if, else, case, default?
	  private boolean isSelections(final String strLine) {
	    if (selectionKeyWords.contains(strLine)) {
	      return true;
	    }
	    return false;
	  }
	  
	  // Does line start with for, while, do, break, continue?
	  private boolean isLoops(final String strLine) {
	    if (loopKeyWords.contains(strLine)) {
	      return true;
	    }
	    return false;
	  }
	  
	  // Is this a method declaration?
	  public boolean isMethod(final String strLine) {
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
	  
	  public String getLineComment() {
		  return "//";
	  }
}
