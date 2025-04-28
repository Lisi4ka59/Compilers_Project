package com.lisi4ka;

import org.antlr.v4.runtime.tree.ParseTree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * RISC-V code generator for the course interpreter.
 * Handles string literals, variables, and logical operators.
 */
public class RiscVCompiler {
    private final Set<String> vars = new LinkedHashSet<>();
    private final Map<String, String> strLiterals = new LinkedHashMap<>();
    private final Set<String> stringVars = new HashSet<>();
    private final List<String> lines = new ArrayList<>();
    private int literalCount = 0;
    private int lblCount = 0;

    /**
     * Compile the parse tree and write to 'program.s'.
     */
    public void compile(ParseTree tree) throws IOException {
        new VarCollector().visit(tree);

        // Emit main at address 0
        lines.add("main:");
        new CodeGenVisitor().visit(tree);
        lines.add("ebreak");
        lines.add("");

        // Emit print_int subroutine
        emitPrintIntSubroutine();
        lines.add("");

        // Emit string literal data
        for (Map.Entry<String, String> entry : strLiterals.entrySet()) {
            String lbl = entry.getKey();
            String val = entry.getValue();
            lines.add(lbl + ":");
            for (char c : val.toCharArray()) {
                lines.add("data " + (int) c + " * 1");
            }
            lines.add("data 0 * 1"); // null terminator
            lines.add("");
        }

        // Emit variable storage
        for (String var : vars) {
            lines.add(var + ":");
            lines.add("data 0 * 1");
        }
        if (!vars.isEmpty()) {
            lines.add("");
        }

        // Emit buffer for integer printing
        lines.add("buf:");
        lines.add("data 0 * 12");

        // Write to file
        Files.write(Paths.get("program.s"), lines);
    }

    // Visitor to collect variables and string literals
    private class VarCollector extends MicroJathonBaseVisitor<Void> {
        @Override
        public Void visitStatement(MicroJathonParser.StatementContext ctx) {
            if (ctx.variable() != null && ctx.expr() != null) {
                String var = ctx.variable().getText();
                vars.add(var);
                if (ctx.expr() instanceof MicroJathonParser.StringExprContext) {
                    String raw = ((MicroJathonParser.StringExprContext) ctx.expr()).STRING().getText();
                    String content = raw.substring(1, raw.length() - 1);
                    stringVars.add(var);
                    String lbl = "str" + (literalCount++);
                    strLiterals.put(lbl, content);
                }
            }
            return super.visitChildren(ctx);
        }
    }

    // Visitor to generate code lines
    private class CodeGenVisitor extends MicroJathonBaseVisitor<Void> {
        @Override
        public Void visitProgram(MicroJathonParser.ProgramContext ctx) {
            for (MicroJathonParser.StatementContext stmt : ctx.statement()) {
                visitStatement(stmt);
            }
            return null;
        }

        @Override
        public Void visitStatement(MicroJathonParser.StatementContext ctx) {
            if (ctx.variable() != null && ctx.expr() instanceof MicroJathonParser.StringExprContext) {
                // String assignment
                String var = ctx.variable().getText();
                String lbl = getLiteralLabelFor(var);
                lines.add("li x7, " + lbl);
                lines.add("li x6, " + var);
                lines.add("sw x6, 0, x7");

            } else if (ctx.variable() != null && ctx.expr() != null) {
                // Numeric assignment
                visit(ctx.expr()); // result in x5
                lines.add("li x6, " + ctx.variable().getText());
                lines.add("sw x6, 0, x5");

            } else if (ctx.getChild(0).getText().equals("print")) {
                // Print statement
                visitPrint(ctx);

            } else if (ctx.getChild(0).getText().equals("if")) {
                // If-else
                String L1 = newLabel();
                String L2 = newLabel();
                visit(ctx.expr()); // x5
                lines.add("beq x5, x0, " + L1);
                visit(ctx.block(0));
                lines.add("jal x0, " + L2);
                lines.add(L1 + ":");
                if (ctx.block().size() > 1) {
                    visit(ctx.block(1));
                }
                lines.add(L2 + ":");

            } else if (ctx.getChild(0).getText().equals("while")) {
                // While loop
                String L1 = newLabel();
                String L2 = newLabel();
                lines.add(L1 + ":");
                visit(ctx.expr()); // x5
                lines.add("beq x5, x0, " + L2);
                visit(ctx.block(0));
                lines.add("jal x0, " + L1);
                lines.add(L2 + ":");

            } else {
                // Nested block
                visit(ctx.block(0));
            }
            return null;
        }

        // Handle print logic
        private void visitPrint(MicroJathonParser.StatementContext ctx) {
            MicroJathonParser.ExprContext expr = ctx.expr();
            if (expr instanceof MicroJathonParser.StringExprContext) {
                // Inline string literal
                String raw = ((MicroJathonParser.StringExprContext) expr).STRING().getText();
                String s = raw.substring(1, raw.length() - 1);
                for (char c : s.toCharArray()) {
                    lines.add("li x10, " + (int) c);
                    lines.add("ewrite x10");
                }
            } else if (expr instanceof MicroJathonParser.VarExprContext && stringVars.contains(expr.getText())) {
                // String variable print
                String var = expr.getText();
                lines.add("li x6, " + var);
                lines.add("lw x10, x6, 0");
                String loop = newLabel();
                String end = newLabel();
                lines.add("beq x10, x0, " + end);
                lines.add(loop + ":");
                lines.add("lw x11, x10, 0");
                lines.add("beq x11, x0, " + end);
                lines.add("ewrite x11");
                lines.add("addi x10, x10, 1");
                lines.add("jal x0, " + loop);
                lines.add(end + ":");
            } else {
                // Numeric or logical expression
                visit(expr);        // result in x5
                lines.add("addi x10, x5, 0");
                lines.add("jal x1, print_int");
            }
            lines.add("li x10, 10");
            lines.add("ewrite x10");
        }

        @Override
        public Void visitVarExpr(MicroJathonParser.VarExprContext ctx) {
            String v = ctx.getText();
            lines.add("li x7, " + v);
            if (stringVars.contains(v)) {
                lines.add("lw x10, x7, 0");
            } else {
                lines.add("lw x5, x7, 0");
            }
            return null;
        }

        @Override
        public Void visitIntExpr(MicroJathonParser.IntExprContext ctx) {
            lines.add("li x5, " + ctx.getText());
            return null;
        }

        @Override
        public Void visitAddSubExpr(MicroJathonParser.AddSubExprContext ctx) {
            visit(ctx.expr(0));
            lines.add("addi x6, x5, 0");
            visit(ctx.expr(1));
            lines.add(ctx.op.getText().equals("+") ? "add x5, x6, x5" : "sub x5, x6, x5");
            return null;
        }

        @Override
        public Void visitMulDivExpr(MicroJathonParser.MulDivExprContext ctx) {
            visit(ctx.expr(0));
            lines.add("addi x6, x5, 0");
            visit(ctx.expr(1));
            lines.add(ctx.op.getText().equals("*") ? "mul x5, x6, x5" : "div x5, x6, x5");
            return null;
        }

        @Override
        public Void visitCompareExpr(MicroJathonParser.CompareExprContext ctx) {
            visit(ctx.expr(0));
            lines.add("addi x6, x5, 0");
            visit(ctx.expr(1));
            switch (ctx.op.getText()) {
                case "==": lines.add("seq x5, x6, x5"); break;
                case "!=": lines.add("sne x5, x6, x5"); break;
                case "<":  lines.add("slt x5, x6, x5"); break;
                case ">":  lines.add("slt x5, x5, x6"); break;
                case ">=": lines.add("sge x5, x6, x5"); break;
                case "<=": lines.add("sge x5, x5, x6"); break;
                default: throw new RuntimeException("Unknown cmp: " + ctx.op.getText());
            }
            return null;
        }

        @Override
        public Void visitAndExpr(MicroJathonParser.AndExprContext ctx) {
            visit(ctx.expr(0));                // x5 = left
            lines.add("addi x6, x5, 0");       // x6 = left
            visit(ctx.expr(1));                // x5 = right
            // boolean and: (left!=0)&&(right!=0)
            lines.add("sne x6, x6, x0");      // x6= left!=0
            lines.add("sne x5, x5, x0");      // x5= right!=0
            lines.add("and x5, x6, x5");     // x5= x6 & x5
            return null;
        }

        @Override
        public Void visitOrExpr(MicroJathonParser.OrExprContext ctx) {
            visit(ctx.expr(0));
            lines.add("addi x6, x5, 0");
            visit(ctx.expr(1));
            // boolean or: (left!=0)||(right!=0)
            lines.add("sne x6, x6, x0");
            lines.add("sne x5, x5, x0");
            lines.add("or x5, x6, x5");
            return null;
        }

        @Override
        public Void visitNotExpr(MicroJathonParser.NotExprContext ctx) {
            visit(ctx.expr());                // x5
            // boolean not: x5 == 0 ?1:0
            lines.add("seq x5, x5, x0");
            return null;
        }

        @Override
        public Void visitBlock(MicroJathonParser.BlockContext ctx) {
            for (MicroJathonParser.StatementContext s : ctx.statement()) {
                visit(s);
            }
            return null;
        }

        @Override
        public Void visitFloatExpr(MicroJathonParser.FloatExprContext ctx) { throw new UnsupportedOperationException("Floating-point not supported"); }
        @Override
        public Void visitStringExpr(MicroJathonParser.StringExprContext ctx) { return null; }
        @Override
        public Void visitParenExpr(MicroJathonParser.ParenExprContext ctx) { return visit(ctx.expr()); }
        @Override
        public Void visitRoundExpr(MicroJathonParser.RoundExprContext ctx) { throw new UnsupportedOperationException("round() not supported"); }
    }

    private String getLiteralLabelFor(String var) {
        for (String lbl : strLiterals.keySet()) {
            if (stringVars.contains(var)) {
                return lbl;
            }
        }
        throw new RuntimeException("No literal for var " + var);
    }

    private String newLabel() {
        return "L" + (lblCount++);
    }

    private void emitPrintIntSubroutine() {
        lines.add("print_int:");
        lines.add("beq x10, x0, print_int_zero");
        lines.add("blt x10, x0, print_int_neg");
        lines.add("addi x5, x10, 0");
        lines.add("li x6, 0");
        lines.add("li x7, 10");
        lines.add("print_div_loop:");
        lines.add("div x8, x5, x7");
        lines.add("rem x9, x5, x7");
        lines.add("addi x5, x8, 0");
        lines.add("li x11, buf");
        lines.add("add x11, x11, x6");
        lines.add("sw x11, 0, x9");
        lines.add("addi x6, x6, 1");
        lines.add("bne x5, x0, print_div_loop");
        lines.add("print_print_loop:");
        lines.add("addi x6, x6, -1");
        lines.add("li x11, 48");
        lines.add("li x13, buf");
        lines.add("add x13, x13, x6");
        lines.add("lw x9, x13, 0");
        lines.add("add x11, x11, x9");
        lines.add("ewrite x11");
        lines.add("bne x6, x0, print_print_loop");
        lines.add("jalr x0, x1, 0");
        lines.add("print_int_zero:");
        lines.add("li x11, 48");
        lines.add("ewrite x11");
        lines.add("jalr x0, x1, 0");
        lines.add("print_int_neg:");
        lines.add("li x11, 45");
        lines.add("ewrite x11");
        lines.add("sub x5, x0, x10");
        lines.add("addi x10, x5, 0");
        lines.add("jal x0, print_int");
    }
}