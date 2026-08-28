// J8BParser.java
package ru.vm5277.plugin.j8b;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class J8BParser implements PsiParser {
	@NotNull
	@Override
	public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
		builder.setDebugMode(true);
		PsiBuilder.Marker rootMarker = builder.mark();

		while (!builder.eof()) {
			builder.advanceLexer();
		}

		rootMarker.done(root);
		return builder.getTreeBuilt();
	}
}