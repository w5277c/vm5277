package ru.vm5277.plugin.old;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class J8BElement extends ASTWrapperPsiElement {

    public J8BElement(@NotNull ASTNode node) {
        super(node);
    }
}