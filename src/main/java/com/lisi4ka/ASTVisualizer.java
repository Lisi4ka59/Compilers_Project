package com.lisi4ka;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.gui.TreeViewer;

import javax.swing.*;
import java.util.Arrays;

public abstract class ASTVisualizer {

    public static void showTree(ParseTree tree, MicroJathonParser parser) {
        JFrame frame = new JFrame("AST Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
        viewer.setScale(1.5);
        JScrollPane scrollPane = new JScrollPane(viewer);
        frame.add(scrollPane);
        frame.setSize(1920, 1080);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}