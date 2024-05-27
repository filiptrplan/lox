package si.trplan.lox;

import java.util.List;

abstract class Stmt {
 interface Visitor<R> {
 R visitExpressionStmt(Expression stmt);
 R visitPrintStmt(Print stmt);
 R visitVarStmt(Var stmt);
 R visitBlockStmt(Block stmt);
 R visitIfStmt(If stmt);
 R visitWhileStmt(While stmt);
 R visitBreakStmt(Break stmt);
 }
 public static class Expression extends Stmt {
 Expression(Expr expression) {
 this.expression = expression;
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitExpressionStmt(this);
 }

 final Expr expression;
 }
 public static class Print extends Stmt {
 Print(Expr expresion) {
 this.expresion = expresion;
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitPrintStmt(this);
 }

 final Expr expresion;
 }
 public static class Var extends Stmt {
 Var(Token name, Expr initializer) {
 this.name = name;
 this.initializer = initializer;
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitVarStmt(this);
 }

 final Token name;
 final Expr initializer;
 }
 public static class Block extends Stmt {
 Block(List<Stmt> statements) {
 this.statements = statements;
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitBlockStmt(this);
 }

 final List<Stmt> statements;
 }
 public static class If extends Stmt {
 If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
 this.condition = condition;
 this.thenBranch = thenBranch;
 this.elseBranch = elseBranch;
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitIfStmt(this);
 }

 final Expr condition;
 final Stmt thenBranch;
 final Stmt elseBranch;
 }
 public static class While extends Stmt {
 While(Expr condition, Stmt statement) {
 this.condition = condition;
 this.statement = statement;
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitWhileStmt(this);
 }

 final Expr condition;
 final Stmt statement;
 }
 public static class Break extends Stmt {
 Break() {
 }

 @Override
 <R> R accept(Visitor<R> visitor) {
 return visitor.visitBreakStmt(this);
 }

 }

 abstract <R> R accept(Visitor<R> visitor);
}
