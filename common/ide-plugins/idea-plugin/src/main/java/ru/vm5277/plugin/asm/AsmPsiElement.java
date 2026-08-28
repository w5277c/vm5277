package ru.vm5277.plugin.asm;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class AsmPsiElement extends ASTWrapperPsiElement {
	public AsmPsiElement(@NotNull ASTNode node) {
		super(node);
	}
}