package ru.vm5277.plugin.old;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.plugin.J8BLanguage;

public class J8BLexerAdapter extends LexerBase {
    private CharSequence buffer;
    private int startOffset;
    private int endOffset;
    private int currentPosition;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.currentPosition = startOffset;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        if (currentPosition >= endOffset) {
            return null;
        }
        // Возвращаем заглушку - любой токен
        return new IElementType("", J8BLanguage.INSTANCE);
    }

    @Override
    public int getTokenStart() {
        return currentPosition;
    }

    @Override
    public int getTokenEnd() {
        // Просто берём всю строку до конца как один токен
        return endOffset;
    }

    @Override
    public void advance() {
        // Перемещаемся в конец - больше токенов нет
        currentPosition = endOffset;
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}