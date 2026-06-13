package ru.vm5277.plugin.j8b;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.lsp.server.J8BLanguageServer;
import ru.vm5277.plugin.ASMLanguage;
import ru.vm5277.plugin.J8BLanguage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer extends LexerBase {
	//TODO необходимо загружать из бибилиотеки тулкита, иначе плагин будет привязан к конретной версии тулкита.
    private static  J8BLanguageServer j8bServer = new J8BLanguageServer();
	private Map<TokenType, IElementType> elementTypes	= new HashMap();
    private List<Token> tokens;
    private int currentIndex = 0;
    private int endOffset;
    private CharSequence buffer;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.endOffset = endOffset;
        this.currentIndex = 0;

        String text = buffer.toString();
        try (SourceBuffer sb = new SourceBuffer(text, 4)) {
            this.tokens = j8bServer.tokenize(sb);
        } catch (Exception e) {
        }
    }

    @Override
    public int getState() {
        return currentIndex;
    }

    @Override
    public IElementType getTokenType() {
        if (tokens == null || currentIndex >= tokens.size()) {
            return null;
        }
        Token token = tokens.get(currentIndex);
        if (token.getSP().getPos() >= endOffset) {
            return null;
        }
		IElementType ieet = elementTypes.get(token.getType());
		if(null==ieet) {
			ieet = new IElementType(token.getType().name(), J8BLanguage.INSTANCE);
			elementTypes.put(token.getType(), ieet);
		}
		return ieet;
    }

	@Override
    public int getTokenStart() {
        return tokens.get(currentIndex).getSP().getPos();
    }

    @Override
    public int getTokenEnd() {
        Token token = tokens.get(currentIndex);
        return token.getSP().getPos() + token.getLength();
    }

    @Override
    public void advance() {
        currentIndex++;
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