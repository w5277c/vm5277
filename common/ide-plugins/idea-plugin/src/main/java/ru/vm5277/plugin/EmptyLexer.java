package ru.vm5277.plugin;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyLexer extends LexerBase {
	private CharSequence buffer;
	private int startOffset;
	private int endOffset;
	private boolean firstToken;

	@Override
	public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.firstToken = true;
	}

	@Override
	public int getState() {
		return 0;
	}

	@Nullable
	@Override
	public IElementType getTokenType() {
		// TokenType.BAD_CHARACTER говорит IDE, что это обычный текст
		if (firstToken && startOffset < endOffset) {
			return com.intellij.psi.TokenType.BAD_CHARACTER;
		}
		return null;
	}

	@Override
	public int getTokenStart() {
		return startOffset;
	}

	@Override
	public int getTokenEnd() {
		return endOffset;
	}

	@Override
	public void advance() {
		firstToken = false;
	}

	@NotNull
	@Override
	public CharSequence getBufferSequence() {
		return buffer != null ? buffer : "";
	}

	@Override
	public int getBufferEnd() {
		return endOffset;
	}
}