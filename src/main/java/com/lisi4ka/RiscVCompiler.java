package com.lisi4ka;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * A simple RISC-V code generator for MicroJathon programs.
 *
 * Usage:
 *   RiscVCompiler compiler = new RiscVCompiler();
 *   String asm = compiler.compile(tree);
 *   Files.writeString(Paths.get("program.s"), asm);
 */
public class RiscVCompiler extends MicroJathonBaseVisitor<String> {
    private StringBuilder data = new StringBuilder();
    private StringBuilder text = new StringBuilder();
    private Set<String> vars = new HashSet<>();
    private int labelCount = 0;

    /**
     * Compile the ANTLR parse tree into a RISC-V assembly string.
     */
    public String compile(ParseTree tree) {
        // Data section with newline definition
        data.append(".data\n");
        data.append("newline: .asciiz \"\\n\"\n");

        // Text section and entry point
        text.append(".text\n.globl main\nmain:\n");

        // Generate code
        visit(tree);

        // Exit syscall
        text.append("li a7, 93\n");
        text.append("ecall\n");

        return data.toString() + "\n" + text.toString();
    }

    // Ensure a variable has space allocated in the .data section
    private void ensureVar(String name) {
        if (vars.add(name)) {
            data.append(name).append(": .word 0\n");
        }
    }

    @Override
    public String visit(org.antlr.v4.runtime.tree.ParseTree tree) {
        // Delegate to program rule if applicable
        return super.visit(tree);
    }

    @Override
    public String visitProgram(MicroJathonParser.ProgramContext ctx) {
        for (MicroJathonParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }
    @Override
    public String visitStatement(MicroJathonParser.StatementContext ctx) {
        // Variable assignment
        if (ctx.variable() != null && ctx.expr() != null) {
            String name = ctx.variable().getText();
            ensureVar(name);
            String reg = visit(ctx.expr());
            text.append("sw ").append(reg).append(", ").append(name).append("\n");

            // Print statement
        } else if (ctx.getChild(0).getText().equals("print")) {
            // String literal print
            if (ctx.expr() instanceof MicroJathonParser.StringExprContext) {
                String raw = ((MicroJathonParser.StringExprContext) ctx.expr()).STRING().getText();
                String lbl = "str" + (labelCount++);
                data.append(lbl).append(": .asciiz ").append(raw).append("\n");
                text.append("la a0, ").append(lbl).append("\n");
                text.append("li a7, 4\necall\n");
            } else {
                // Numeric print
                String reg = visit(ctx.expr());
                text.append("mv a0, ").append(reg).append("\n");
                text.append("li a7, 1\necall\n");
            }
            // Newline
            text.append("la a0, newline\nli a7, 4\necall\n");

            // If-else
        } else if (ctx.getChild(0).getText().equals("if")) {
            String cond = visit(ctx.expr());
            String elseLbl = "L" + (labelCount++);
            String endLbl  = "L" + (labelCount++);
            text.append("beqz ").append(cond).append(", ").append(elseLbl).append("\n");
            visit(ctx.block(0));
            text.append("j ").append(endLbl).append("\n");
            text.append(elseLbl).append(":\n");
            if (ctx.block().size() > 1) visit(ctx.block(1));
            text.append(endLbl).append(":\n");

            // While loop
        } else if (ctx.getChild(0).getText().equals("while")) {
            String startLbl = "L" + (labelCount++);
            String endLbl   = "L" + (labelCount++);
            text.append(startLbl).append(":\n");
            String cond = visit(ctx.expr());
            text.append("beqz ").append(cond).append(", ").append(endLbl).append("\n");
            visit(ctx.block(0));
            text.append("j ").append(startLbl).append("\n");
            text.append(endLbl).append(":\n");

            // Block alone
        } else {
            visit(ctx.block(0));
        }
        return null;
    }

    // Expression visitors return the register (always t0) containing the result

    @Override
    public String visitVarExpr(MicroJathonParser.VarExprContext ctx) {
        String name = ctx.getText();
        ensureVar(name);
        text.append("lw t0, ").append(name).append("\n");
        return "t0";
    }

    @Override
    public String visitIntExpr(MicroJathonParser.IntExprContext ctx) {
        text.append("li t0, ").append(ctx.getText()).append("\n");
        return "t0";
    }

    @Override
    public String visitFloatExpr(MicroJathonParser.FloatExprContext ctx) {
        throw new UnsupportedOperationException("Floating-point not supported");
    }

    @Override
    public String visitStringExpr(MicroJathonParser.StringExprContext ctx) {
        throw new UnsupportedOperationException("StringExpr only in print");
    }

    @Override
    public String visitParenExpr(MicroJathonParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    // Binary ops: push left, eval right, pop left, combine into t0

    @Override
    public String visitAddSubExpr(MicroJathonParser.AddSubExprContext ctx) {
        String left = visit(ctx.expr(0));
        text.append("addi sp, sp, -4\n");
        text.append("sw ").append(left).append(", 0(sp)\n");
        String right = visit(ctx.expr(1));
        text.append("lw t1, 0(sp)\naddi sp, sp, 4\n");
        if (ctx.op.getText().equals("+")) text.append("add t0, t1, ").append(right).append("\n");
        else text.append("sub t0, t1, ").append(right).append("\n");
        return "t0";
    }

    @Override
    public String visitMulDivExpr(MicroJathonParser.MulDivExprContext ctx) {
        String left = visit(ctx.expr(0));
        text.append("addi sp, sp, -4\n");
        text.append("sw ").append(left).append(", 0(sp)\n");
        String right = visit(ctx.expr(1));
        text.append("lw t1, 0(sp)\naddi sp, sp, 4\n");
        if (ctx.op.getText().equals("*")) text.append("mul t0, t1, ").append(right).append("\n");
        else text.append("div t0, t1, ").append(right).append("\n");
        return "t0";
    }

    @Override
    public String visitCompareExpr(MicroJathonParser.CompareExprContext ctx) {
        String left = visit(ctx.expr(0));
        text.append("addi sp, sp, -4\n");
        text.append("sw ").append(left).append(", 0(sp)\n");
        String right = visit(ctx.expr(1));
        text.append("lw t1, 0(sp)\naddi sp, sp, 4\n");
        String op = ctx.op.getText();
        switch (op) {
            case "==":
                text.append("sub t0, t1, ").append(right).append("\nseqz t0, t0\n");
                break;
            case "!=":
                text.append("sub t0, t1, ").append(right).append("\nsnez t0, t0\n");
                break;
            case "<":
                text.append("slt t0, t1, ").append(right).append("\n");
                break;
            case "<=":
                text.append("sgt t0, t1, ").append(right).append("\nseqz t0, t0\n");
                break;
            case ">":
                text.append("sgt t0, t1, ").append(right).append("\n");
                break;
            case ">=":
                text.append("slt t0, t1, ").append(right).append("\nseqz t0, t0\n");
                break;
            default:
                throw new RuntimeException("Unknown compare: " + op);
        }
        return "t0";
    }

    @Override
    public String visitRoundExpr(MicroJathonParser.RoundExprContext ctx) {
        throw new UnsupportedOperationException("round() not supported");
    }
}
