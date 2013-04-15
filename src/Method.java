
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
  private int cComplexity = 0;

  // Constructor
  Method(final String name) {
    methodName = name;
  }

  Method(final int firstLine, final int lastLine, final String name) {
    lineNr = firstLine;
    lastLineNr = lastLine;
    methodName = name;
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
    cComplexity = complexity;
  }

  public int getComplexity() {
    return cComplexity;
  }
}
