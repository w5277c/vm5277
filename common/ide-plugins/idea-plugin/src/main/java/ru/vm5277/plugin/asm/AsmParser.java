package ru.vm5277.plugin.asm;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.lexer.TokenType;

public class AsmParser implements PsiParser {
	@NotNull
	@Override
	public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
		PsiBuilder.Marker rootMarker = builder.mark();

		while (!builder.eof()) {
			IElementType tokenType = builder.getTokenType();

			if (tokenType != null) {
				// ПРОВЕРЯЕМ: является ли токен пробелом/комментарием/строкой?
				if (isSkippedToken(tokenType)) {
					// Пропускаем без создания элемента
					builder.advanceLexer();
				} else {
					// Создаём элемент для значимых токенов
					PsiBuilder.Marker tokenMarker = builder.mark();
					builder.advanceLexer();
					tokenMarker.done(tokenType);
				}
			} else {
				builder.advanceLexer();
			}
		}

		rootMarker.done(root);
		return builder.getTreeBuilt();
	}

	private boolean isSkippedToken(IElementType tokenType) {
		// Проверяем, является ли токен пробелом, комментарием или строкой
		IElementType whitespace = AsmLexer.getIElementType(TokenType.WHITESPACE);
		IElementType comment = AsmLexer.getIElementType(TokenType.COMMENT);
		IElementType string = AsmLexer.getIElementType(TokenType.STRING);

		return tokenType == whitespace || tokenType == comment || tokenType == string;
	}
}