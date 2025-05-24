package com.lisi4ka;

import java.util.HashMap;
import java.util.Map;

public class MicroJathonInterpreter extends MicroJathonBaseVisitor<Object> {
    private final Map<String, Object> memory = new HashMap<>();

    @Override
    public Object visitProgram(MicroJathonParser.ProgramContext ctx) {
        for (MicroJathonParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Object visitStatement(MicroJathonParser.StatementContext ctx) {
        if (ctx.variable() != null && ctx.expr() != null) {
            Object value = visit(ctx.expr());
            memory.put(ctx.variable().getText(), value);
        } else if (ctx.getChild(0).getText().equals("print")) {
            Object value = visit(ctx.expr());
            System.out.println(value);
        } else if (ctx.getChild(0).getText().equals("if")) {
            Object cond = visit(ctx.expr());
            if (toInt(cond) != 0) {
                visit(ctx.block(0));
            } else if (ctx.block().size() > 1) {
                visit(ctx.block(1));
            }
        } else if (ctx.getChild(0).getText().equals("while")) {
            while (toInt(visit(ctx.expr())) != 0) {
                visit(ctx.block(0));
            }
        } else {
            visit(ctx.block(0));
        }
        return null;
    }

    @Override
    public Object visitBlock(MicroJathonParser.BlockContext ctx) {
        for (MicroJathonParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Object visitVarExpr(MicroJathonParser.VarExprContext ctx) {
        return memory.getOrDefault(ctx.getText(), 0);
    }

    @Override
    public Object visitIntExpr(MicroJathonParser.IntExprContext ctx) {
        return Integer.parseInt(ctx.getText());
    }

    @Override
    public Object visitFloatExpr(MicroJathonParser.FloatExprContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    @Override
    public Object visitStringExpr(MicroJathonParser.StringExprContext ctx) {
        return ctx.STRING().getText().substring(1, ctx.STRING().getText().length() - 1);
    }

    @Override
    public Object visitParenExpr(MicroJathonParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Object visitAddSubExpr(MicroJathonParser.AddSubExprContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        if ((left instanceof Number) && (right instanceof Number)) {
            double l = toDouble(left);
            double r = toDouble(right);
            return op.equals("+") ? promote(l + r) : promote(l - r);
        }
        throw new RuntimeException("Unsupported operands for + or -");
    }

    @Override
    public Object visitMulDivExpr(MicroJathonParser.MulDivExprContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        double l = toDouble(left);
        double r = toDouble(right);
        return op.equals("*") ? promote(l * r) : promote(l / r);
    }

    @Override
    public Object visitCompareExpr(MicroJathonParser.CompareExprContext ctx) {
        Object left = visit(ctx.expr(0));
        Object right = visit(ctx.expr(1));
        String op = ctx.op.getText();

        double l = toDouble(left);
        double r = toDouble(right);
        return switch (op) {
            case "==" -> l == r ? 1 : 0;
            case "!=" -> l != r ? 1 : 0;
            case "<" -> l < r ? 1 : 0;
            case ">" -> l > r ? 1 : 0;
            case "<=" -> l <= r ? 1 : 0;
            case ">=" -> l >= r ? 1 : 0;
            default -> throw new RuntimeException("Invalid comparison operator");
        };
    }

    @Override
    public Object visitAndExpr(MicroJathonParser.AndExprContext ctx) {
        Object l = visit(ctx.expr(0));
        Object r = visit(ctx.expr(1));
        int li = toInt(l);
        int ri = toInt(r);
        return (li != 0 && ri != 0) ? 1 : 0;
    }

    @Override
    public Object visitOrExpr(MicroJathonParser.OrExprContext ctx) {
        Object l = visit(ctx.expr(0));
        Object r = visit(ctx.expr(1));
        int li = toInt(l);
        int ri = toInt(r);
        return (li != 0 || ri != 0) ? 1 : 0;
    }

    @Override
    public Object visitNotExpr(MicroJathonParser.NotExprContext ctx) {
        Object v = visit(ctx.expr());
        return toInt(v) == 0 ? 1 : 0;
    }

    private int toInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Double) return (int) Math.round((Double) obj);
        throw new RuntimeException("Cannot convert to int: " + obj);
    }

    private double toDouble(Object obj) {
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof Double) return (Double) obj;
        throw new RuntimeException("Cannot convert to double: " + obj);
    }

    private Object promote(double value) {
        return value == Math.floor(value) ? (int) value : value;
    }
}