package com.lisi4ka;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.gui.TreeViewer;

import javax.swing.*;
import java.util.Arrays;

public class ASTVisualizer {

    public static void showTree(ParseTree tree, MicroJathonParser parser) {
        // Создаем окно
        JFrame frame = new JFrame("AST Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Создаем компонент TreeViewer
        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
        viewer.setScale(1.5); // Увеличить масштаб для лучшей читаемости

        // Добавляем скроллинг
        JScrollPane scrollPane = new JScrollPane(viewer);
        frame.add(scrollPane);

        // Размер окна
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null); // Центрировать
        frame.setVisible(true);
    }
}