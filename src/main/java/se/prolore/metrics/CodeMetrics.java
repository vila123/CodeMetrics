package se.prolore.metrics;

// CodeMetrics
// TODO: Support excluding files or folders. This is useful if parts of your source code was generated by a tool.
// -e PATH1_TO_EXCLUDE;PATH2_TO_EXCLUDE
// TODO: Support for defining components/modules
// -m module1=PATH1;PATH2 module2=PATH3
// TODO: Support for Java, C#, C++, XML
// TODO: Output to .csv file

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class CodeMetrics {
  private boolean calculateCodeChurn = false;
  private SourceFiles oldFiles = null;
  private SourceFiles newFiles = null;
  private File oldFile = null;
  private File newFile = null;
  
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
    CodeMetrics codeMetrics = new CodeMetrics();

    codeMetrics.parseCommandLine(args);

    // Calculate Code Churn
    if (codeMetrics.calculateCodeChurn) {
      codeMetrics.countChurn(codeMetrics.oldFiles, codeMetrics.newFiles);
    }

    // Calculate Cyclomatic Complexity
    codeMetrics.countComplexity(codeMetrics.newFiles);

    // Count LOC (Lines of Code)
    codeMetrics.countLines(codeMetrics.newFiles);

    codeMetrics.printReport();
    codeMetrics.writeReport();
  }

  private void writeReport() {
    BufferedWriter out = null;
    try {
      FileWriter fstream = new FileWriter("out.csv");
      out = new BufferedWriter(fstream);
      out.write("File Name;");
      out.write("Total Lines of Code;"); 
      out.write("Executable Lines;");
      out.write("Lines of Comments;"); 
      out.write("Trivial Lines;"); 
      out.write("Empty Lines;"); 
      out.write("Code Complexity;");
      out.write("Number of Methods;");
      out.write("Average Method Complexity;");
      out.write("Comment Percentage;");
      if (calculateCodeChurn) {
        out.write("Added Lines of Code;");
        out.write("Changed Lines of Code;");
        out.write("Deleted Lines of Code;");
        out.write("Code Churn;");
      }
      out.write(System.getProperty("line.separator"));
      
      for (int i = 0; i < newFiles.getNrOfFiles(); i++) {
        out.write(newFiles.getParser(i).getSourceFile().getAbsolutePath() + ";"); 
        out.write(String.valueOf(newFiles.getParser(i).getLinesOfCode()) + ";"); 
        out.write(String.valueOf(newFiles.getParser(i).getLinesOfStatements()) + ";");
        out.write(String.valueOf(newFiles.getParser(i).getLinesOfComments()) + ";"); 
        out.write(String.valueOf(newFiles.getParser(i).getTrivialLines()) + ";");
        out.write(String.valueOf(newFiles.getParser(i).getEmptyLines()) + ";");
        out.write(String.valueOf(newFiles.getParser(i).getComplexity()) + ";");
        out.write(String.valueOf(newFiles.getParser(i).getNrOfMethods()) + ";");
        out.write(String.valueOf(newFiles.getParser(i).getAvgComplexity()) + ";");
        out.write(String.valueOf((100 * newFiles.getParser(i).getLinesOfComments()) / newFiles.getParser(i).getLinesOfCode() + "%") + ";");
        if (calculateCodeChurn) {
          out.write(String.valueOf(newFiles.getParser(i).getAddedLines()) + ";");
          out.write(String.valueOf(newFiles.getParser(i).getChangedLines()) + ";");
          out.write(String.valueOf(newFiles.getParser(i).getDeletedLines()) + ";");
          out.write(String.valueOf(newFiles.getParser(i).getCodeChurn()) + ";");
        }
        out.write(System.getProperty("line.separator"));
      }
      
      //Close the output stream
      out.close();
    }
    catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      return;
    }
  }

  private void printReport() {
    for (int i = 0; i < newFiles.getNrOfFiles(); i++) {
      System.out.println(newFiles.getParser(i).getSourceFile().getAbsolutePath());
      System.out.println("\t Total Lines of Code:       " + newFiles.getParser(i).getLinesOfCode()); 
      System.out.println("\t Executable Lines:          " + newFiles.getParser(i).getLinesOfStatements());
      System.out.println("\t Lines of Comments:         " + newFiles.getParser(i).getLinesOfComments()); 
      System.out.println("\t Trivial Lines:             " + newFiles.getParser(i).getTrivialLines()); 
      System.out.println("\t Empty Lines:               " + newFiles.getParser(i).getEmptyLines()); 
      System.out.println("\t Code Complexity:           " + newFiles.getParser(i).getComplexity());
      System.out.println("\t Number of Methods:         " + newFiles.getParser(i).getNrOfMethods());
      System.out.println("\t Average Method Complexity: " + newFiles.getParser(i).getAvgComplexity());
      System.out.println("\t Comment Percentage:        " + (100 * newFiles.getParser(i).getLinesOfComments()) / newFiles.getParser(i).getLinesOfCode() + "%");
      // Recommendations: Code where the percentage of comment is lower than 20% should be more commented. 
      // However overly commented code (>40%) is more difficult to read.
      if (calculateCodeChurn) {
        System.out.println("\t Added Lines of Code:       " + newFiles.getParser(i).getAddedLines());
        System.out.println("\t Changed Lines of Code:     " + newFiles.getParser(i).getChangedLines());
        System.out.println("\t Deleted Lines of Code:     " + newFiles.getParser(i).getDeletedLines());
        System.out.println("\t Code Churn:                " + newFiles.getParser(i).getCodeChurn());
      }
    }
    System.out.println("Total (Aggregated Metrics)");
    System.out.println("\t Total Lines of Code:     " + newFiles.sumLinesOfCode()); 
    System.out.println("\t Executable Lines:        " + newFiles.sumLinesOfStatements());
    System.out.println("\t Lines of Comments:       " + newFiles.sumLinesOfComments()); 
    System.out.println("\t Trivial Lines:           " + newFiles.sumTrivialLines()); 
    System.out.println("\t Empty Lines:             " + newFiles.sumEmptyLines()); 
    System.out.println("\t Code Complexity:         " + newFiles.sumComplexity());
    System.out.println("\t Number of Files:         " + newFiles.getNrOfFiles());
    System.out.println("\t Average File Complexity: " + newFiles.sumAvgComplexity());
    System.out.println("\t Comment Percentage:      " + (100 * newFiles.sumLinesOfComments()) / newFiles.sumLinesOfCode() + "%");
    // Recommendations: Code where the percentage of comment is lower than 20% should be more commented. 
    // However overly commented code (>40%) is more difficult to read.  
    if (calculateCodeChurn) {
      System.out.println("\t Added Lines of Code:     " + newFiles.sumAddedLines());
      System.out.println("\t Changed Lines of Code:   " + newFiles.sumChangedLines());
      System.out.println("\t Deleted Lines of Code:   " + newFiles.sumDeletedLines());
      System.out.println("\t Code Churn:              " + newFiles.sumCodeChurn());
    }
  }

  private void parseCommandLine(final String[] args) {
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
        newFiles.parseSrcDir(newFile);
      }
      else if (newFile.isFile()) {
        // One file
        newFiles.addSrcFile(newFile);        
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
        oldFiles.addSrcFile(oldFile);
        newFiles.addSrcFile(newFile);
      }
      else {
        System.out.println(usage);
        System.exit(1);
      }
    }
  }

  public void countChurn(final SourceFiles oldFiles, final SourceFiles newFiles) {
    for (int i = 0; i < newFiles.getNrOfFiles(); i++) {
      Diff d = new Diff();
      d.countChurn(oldFiles.getParser(i), newFiles.getParser(i));
    }
  }
  
  public void countComplexity(final SourceFiles srcFiles) {
    for (int i = 0; i < srcFiles.getNrOfFiles(); i++) {
      srcFiles.getParser(i).parseMethods();
      srcFiles.getParser(i).countComplexity();
    }
  }

  public void countLines(final SourceFiles srcFiles) {
    for (int i = 0; i < srcFiles.getNrOfFiles(); i++) {
      srcFiles.getParser(i).countLines();
    }
  }
}



//public class XDirDiff {
//  static void Main(string[] args)
//  {
//
//    // Create two identical or different temporary folders  
//    // on a local drive and change these file paths. 
//    string pathA = @"C:\TestDir";
//    string pathB = @"C:\TestDir2";
//
//    System.IO.DirectoryInfo dir1 = new System.IO.DirectoryInfo(pathA);
//    System.IO.DirectoryInfo dir2 = new System.IO.DirectoryInfo(pathB);
//
//    // Take a snapshot of the file system.
//    IEnumerable<System.IO.FileInfo> list1 = dir1.GetFiles("*.*", System.IO.SearchOption.AllDirectories);
//    IEnumerable<System.IO.FileInfo> list2 = dir2.GetFiles("*.*", System.IO.SearchOption.AllDirectories);
//
//    //A custom file comparer defined below
//    FileCompare myFileCompare = new FileCompare();
//
//    // This query determines whether the two folders contain 
//    // identical file lists, based on the custom file comparer 
//    // that is defined in the FileCompare class. 
//    // The query executes immediately because it returns a bool. 
//    bool areIdentical = list1.SequenceEqual(list2, myFileCompare);
//
//    if (areIdentical == true)
//    {
//      Console.WriteLine("the two folders are the same");
//    }
//    else
//    {
//      Console.WriteLine("The two folders are not the same");
//    }
//
//    // Find the common files. It produces a sequence and doesn't  
//    // execute until the foreach statement. 
//    var queryCommonFiles = list1.Intersect(list2, myFileCompare);
//
//    if (queryCommonFiles.Count() > 0)
//    {
//      Console.WriteLine("The following files are in both folders:");
//      foreach (var v in queryCommonFiles)
//      {
//        Console.WriteLine(v.FullName); //shows which items end up in result list
//      }
//    }
//    else
//    {
//      Console.WriteLine("There are no common files in the two folders.");
//    }
//
//    // Find the set difference between the two folders. 
//    // For this example we only check one way. 
//    var queryList1Only = (from file in list1
//        select file).Except(list2, myFileCompare);
//
//    Console.WriteLine("The following files are in list1 but not list2:");
//    foreach (var v in queryList1Only)
//    {
//      Console.WriteLine(v.FullName);
//    }
//
//    // Keep the console window open in debug mode.
//    Console.WriteLine("Press any key to exit.");
//    Console.ReadKey();
//  }
//}
//
//// This implementation defines a very simple comparison 
//// between two FileInfo objects. It only compares the name 
//// of the files being compared and their length in bytes. 
//class FileCompare : System.Collections.Generic.IEqualityComparer<System.IO.FileInfo>
//{
//  public FileCompare() { }
//
//  public bool Equals(System.IO.FileInfo f1, System.IO.FileInfo f2)
//  {
//    return (f1.Name == f2.Name &&
//        f1.Length == f2.Length);
//  }
//
//  // Return a hash that reflects the comparison criteria. According to the  
//  // rules for IEqualityComparer<T>, if Equals is true, then the hash codes must 
//  // also be equal. Because equality as defined here is a simple value equality, not 
//  // reference identity, it is possible that two or more objects will produce the same 
//  // hash code. 
//  public int GetHashCode(System.IO.FileInfo fi)
//  {
//    string s = String.Format("{0}{1}", fi.Name, fi.Length);
//    return s.GetHashCode();
//  }
//}
//}
