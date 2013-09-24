package se.prolore.metrics;

import java.io.File;
import java.util.ArrayList;

public class SourceFiles {
	ArrayList<ComplexityParser> srcFiles = null;

	// Cyclomatic Complexity
	private int CC = 0;

	// Constructor
	SourceFiles() {
		srcFiles = new ArrayList<ComplexityParser>();
	}

	// Parse a file path and find all source files
	public void parseSrcDir(final File sDir) {
		File[] faFiles = new File(sDir.getAbsolutePath()).listFiles();

		for (File file : faFiles) {
			if (file.isFile()) {
				// System.out.println(file.getName());

				addSrcFile(file);
			}
			if (file.isDirectory()) {
				parseSrcDir(file);
			}
		}
	}

	// Only work with one source file
	public void addSrcFile(final File sFile) {
		if (sFile.getName().endsWith("java") && sFile.isFile()) {
			srcFiles.add(new ComplexityParser(sFile));
		} else {
			System.out
					.println("ERROR: File type is not yet supported. Only support for java files.");
			System.exit(1);
		}
	}

	// Return the total number of source code files
	public int getNrOfFiles() {
		return srcFiles.size();
	}

	public ComplexityParser getParser(final int index) {
		return srcFiles.get(index);
	}

	// LOC Metrics
	public int sumLinesOfCode() {
		int LOC = 0; // Lines of Code (total)
		for (ComplexityParser srcFile : srcFiles) {
			LOC += srcFile.getLinesOfCode();
		}
		return LOC;
	}

	public int sumLinesOfStatements() {
		int stLOC = 0; // Lines of Statements (ending with ';')
		for (ComplexityParser srcFile : srcFiles) {
			stLOC += srcFile.getLinesOfStatements();
		}
		return stLOC;
	}

	public int sumLinesOfComments() {
		int ccLOC = 0; // Lines of Comments (single line of comment
		for (ComplexityParser srcFile : srcFiles) {
			ccLOC += srcFile.getLinesOfComments();
		}
		return ccLOC;
	}

	public int sumTrivialLines() {
		int trLOC = 0; // Trivial Lines ( ´{´ or ´}´)
		for (ComplexityParser srcFile : srcFiles) {
			trLOC += srcFile.getTrivialLines();
		}
		return trLOC;
	}

	public int sumEmptyLines() {
		int emLOC = 0; // Empty Lines (no visible characters)
		for (ComplexityParser srcFile : srcFiles) {
			emLOC += srcFile.getEmptyLines();
		}
		return emLOC;
	}

	// Complexity mertics
	public float sumComplexity() {
		for (ComplexityParser srcFile : srcFiles) {
			CC += srcFile.getComplexity();
		}
		return (float) CC;
	}

	public float sumAvgComplexity() {
		if (CC != 0) {
			return (float) CC / srcFiles.size();
		}
		return (float) 0;
	}

	// Code Churn Metrics
	public int sumAddedLines() {
		int adLOC = 0; // Added Lines of Code
		for (ComplexityParser srcFile : srcFiles) {
			adLOC += srcFile.getAddedLines();
		}
		return adLOC;
	}

	public int sumChangedLines() {
		int chLOC = 0; // Changed Lines of Code
		for (ComplexityParser srcFile : srcFiles) {
			chLOC += srcFile.getChangedLines();
		}
		return chLOC;
	}

	public int sumDeletedLines() {
		int dlLOC = 0; // Deleted Lines of Code
		for (ComplexityParser srcFile : srcFiles) {
			dlLOC += srcFile.getDeletedLines();
		}
		return dlLOC;
	}

	public int sumCodeChurn() {
		int codeChurn = 0;
		for (ComplexityParser srcFile : srcFiles) {
			codeChurn += srcFile.getCodeChurn();
		}
		return codeChurn;
	}
}
