package si.trplan.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Helper script to automate generating all the classes needed for the AST
 */
public class GenerateAst {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output dir>");
            System.exit(64);
        }

        String outputDir = args[0];
        try {
            defineAst(outputDir, "Expr", Arrays.asList(
                    "Assign : Token name, Expr value",
                    "Binary : Expr left, Token operator, Expr right",
                    "Logical : Expr left, Token operator, Expr right",
                    "Grouping : Expr expression",
                    "Call : Expr callee, Token paren, List<Expr> arguments",
                    "Literal : Object value",
                    "Unary : Token operator, Expr right",
                    "Variable : Token name",
                    "Function : List<Token> params, List<Stmt> body"
            ));
            defineAst(outputDir, "Stmt", Arrays.asList(
                    "Expression : Expr expression",
                    "Print : Expr expresion",
                    "Var : Token name, Expr initializer",
                    "Block : List<Stmt> statements",
                    "Function : Token name, List<Token> params, List<Stmt> body",
                    "If: Expr condition, Stmt thenBranch, Stmt elseBranch",
                    "While : Expr condition, Stmt statement",
                    "Break : Token keyword",
                    "Return : Token keyword, Expr value"
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package si.trplan.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields;
            if(type.split(":").length == 1) {
                fields = "";
            } else {
                fields = type.split(":")[1].trim();
            }
            defineType(writer, baseName, className, fields);
        }

        // The base accept() method.
        writer.println();
        writer.println(" abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println(" public static class " + className + " extends " + baseName + " {");

        // Constructor. 
        writer.println(" " + className + "(" + fieldList + ") {");

        // Store parameters in fields. 
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            if(field.isEmpty()) continue;
            String name = field.split(" ")[1];
            writer.println(" this." + name + " = " + name + ";");
        }

        writer.println(" }");

        // Visitor pattern.
        writer.println();
        writer.println(" @Override");
        writer.println(" <R> R accept(Visitor<R> visitor) {");
        writer.println(" return visitor.visit" + className + baseName + "(this);");
        writer.println(" }");

        // Fields. 
        writer.println();

        for (String field : fields) {
            if(field.isEmpty()) continue;
            writer.println(" final " + field + ";");
        }

        writer.println(" }");
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println(" interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println(" R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println(" }");
    }
}
