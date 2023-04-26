package com.microsoft.java.bs.core.contrib.javac;

// import java.io.File;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import ch.epfl.scala.bsp4j.Diagnostic;
// import ch.epfl.scala.bsp4j.Position;
// import ch.epfl.scala.bsp4j.Range;
// import ch.epfl.scala.bsp4j.TextDocumentIdentifier;

/**
 * Parses the output from javac.
 */
public class JavacOutputParser {
  // public Map<TextDocumentIdentifier, List<Diagnostic>> parse(String output) {
  //   //TODO: improve it https://github.com/JetBrains/android/blob/master/build-common/src/com/android/tools/idea/gradle/output/parser/javac/JavacOutputParser.java
  //   Map<TextDocumentIdentifier, List<Diagnostic>> map = new HashMap<>();
  //   String[] lines = output.split("\n");
  //   int column = 0;
  //   int i = 0;
  //   while (i + 2 < lines.length) {
  //     String line = lines[i];
  //     int idx = line.indexOf(":");
  //     if (idx == -1) { // no more error messages
  //       break;
  //     }
  //     if (idx == 1) { // drive letter (Windows)
  //       idx = line.indexOf(":", idx + 1);
  //     }
  //     String filePath = line.substring(0, idx);
  //     File file = new File(filePath);
  //     TextDocumentIdentifier identifier = new TextDocumentIdentifier(file.toURI().toString());

  //     line = line.substring(idx + 1);
  //     idx = line.indexOf(":");
  //     int lineNum = Integer.parseInt(line.substring(0, idx));

  //     line = line.substring(idx + 1);
  //     String msg = line;

  //     int j = 2;
  //     String endMessage = lines[i + j];
  //     while (!endMessage.trim().equals("^")) {
  //       j++;
  //     }
  //     column = endMessage.indexOf('^');

  //     Diagnostic diagnostic = new Diagnostic(new Range(
  //       new Position(lineNum - 1, column - 1),
  //       new Position(lineNum - 1, column)),
  //       msg.trim()
  //     );

  //     List<Diagnostic> diagnostics = map.getOrDefault(identifier, new ArrayList<>());
  //     diagnostics.add(diagnostic);
  //     map.put(identifier, diagnostics);
  //     column = 0;
  //     i += j;
  //     i++;

  //     line = lines[i];
  //   }
  //   return map;
  // }
}
