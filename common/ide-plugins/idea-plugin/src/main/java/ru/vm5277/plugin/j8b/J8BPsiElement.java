// J8BPsiElement.java
package ru.vm5277.plugin.j8b;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class J8BPsiElement extends ASTWrapperPsiElement {
	public J8BPsiElement(@NotNull ASTNode node) {
		super(node);
	}
}