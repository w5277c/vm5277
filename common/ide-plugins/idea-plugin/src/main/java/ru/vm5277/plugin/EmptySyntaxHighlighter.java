package ru.vm5277.plugin;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerBase;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;

public class EmptySyntaxHighlighter extends SyntaxHighlighterBase {
	@Override
	public Lexer getHighlightingLexer() {
		return new EmptyLexer();
	}

	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		return EMPTY;
	}
}

