package se.prolore.metrics;


public class SourceFile {
  private String fileName = "";
  private String filePath = "";
  private ComplexityParser parser;

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
	  setComplexityParser(new ComplexityParser(this));
  }

  SourceFile(final String name, final String path) {
    fileName = name;
    filePath = path;
    setComplexityParser(new ComplexityParser(this));
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

public ComplexityParser getComplexityParser() {
	return parser;
}

public void setComplexityParser(ComplexityParser parser) {
	this.parser = parser;
}

public int getLinesOfCode() {
	return parser.getLinesOfCode();
}

public int getLinesOfStatements() {
	return parser.getLinesOfStatements();
}

public int getLinesOfComments() {
	 return parser.getLinesOfComments();
}

public int getTrivialLines() {
	return parser.getTrivialLines();
}

public int getEmptyLines() {
	return parser.getEmptyLines();
}

public float getComplexity() {
	return parser.getComplexity();
}

public int getAddedLines() {
	return parser.getAddedLines();
}

public int getChangedLines() {
	return parser.getChangedLines();
}

public int getDeletedLines() {
	return parser.getDeletedLines();
}

public int getCodeChurn() {
	return parser.getCodeChurn();
}

public int getNrOfMethods() {
	return parser.getNrOfMethods();
}

public float getAvgComplexity() {
	return parser.getAvgComplexity();
}

public void getMethods() {
	parser.getMethods();
}

public void countComplexity() {
	parser.countComplexity();
}

public void countLines() {
	parser.countLines();
}


  
 
}