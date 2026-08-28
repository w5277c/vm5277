package ru.vm5277.plugin.asm;

import com.intellij.application.options.CodeStyle;
import com.intellij.lexer.LexerBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.lsp.server.ASMLanguageServer;
import ru.vm5277.plugin.ASMLanguage;
import ru.vm5277.plugin.j8b.FileType;

import java.util.*;

public class AsmLexer extends LexerBase {
    private	final int tabSize;
	private final ASMLanguageServer	lspServer;
	private final static Map<TokenType, IElementType> ELEMENT_TYPES = new HashMap<>();
	private	List<Token> tokens;
    private	int currentIndex 	= 0;
	private	int startOffset;
	private	int endOffset;
    private	CharSequence buffer;

	static {
		// Инициализируем все возможные типы токенов заранее
		for (TokenType type : TokenType.values()) {
			ELEMENT_TYPES.put(type, new IElementType(type.name(), ASMLanguage.INSTANCE));
		}
	}

    public AsmLexer(Project project) {
		CodeStyleSettings settings = CodeStyle.getSettings(project);
		tabSize = settings.getTabSize(FileType.INSTANCE);
		lspServer = AsmSyntaxHighlighterFactory.getLspSever(project);
    }

	@Override
	public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.currentIndex = 0;

		if(null==lspServer || buffer.isEmpty()) {
			this.tokens = null;
			return;
		}

		String text = buffer.toString();
//		System.out.println("=== LEXER START DEBUG ===");
//		System.out.println("Buffer length: " + buffer.length());
//		System.out.println("startOffset: " + startOffset);
//		System.out.println("endOffset: " + endOffset);
//		System.out.println("Text: '" + buffer.toString() + "'");

		try (SourceBuffer sb = new SourceBuffer(text, tabSize)) {
			this.tokens = lspServer.tokenize(sb);
//			for(Token token : tokens) {
//				System.out.println("Token: " + token.toString());
//			}

			// Пропускаем токены до startOffset
			while(currentIndex<tokens.size() && tokens.get(currentIndex).getSP().getPos() +	tokens.get(currentIndex).getLength() <= startOffset) {

				currentIndex++;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			this.tokens = Collections.emptyList();
		}
	}
	@Override
    public int getState() {
        return currentIndex;
    }

	@Override
	public IElementType getTokenType() {
		if(null==lspServer || null==tokens || currentIndex>=tokens.size()) {
			return null;
		}

		Token token = tokens.get(currentIndex);
		int tokenStart = token.getSP().getPos();
		int tokenEnd = tokenStart + token.getLength();

		// Токен полностью до начала - не показываем
		if(tokenEnd<=startOffset) {
			return null;
		}

		// Токен начинается после конца - не показываем
		if(tokenStart>=endOffset) {
			return null;
		}

		return ELEMENT_TYPES.get(token.getType());
	}
	
    @Override
    public int getTokenStart() {
        return null==lspServer ? 0 : tokens.get(currentIndex).getSP().getPos();
    }

    @Override
    public int getTokenEnd() {
		if(null==lspServer || null==tokens || currentIndex>=tokens.size()) {
			return endOffset;
		}

		Token token = tokens.get(currentIndex);
		int tokenEnd = token.getSP().getPos() + token.getLength();

		// ВАЖНО: не выходить за границы буфера
		return Math.min(tokenEnd, endOffset);
	}

    @Override
	public void advance() {
		if(null!=lspServer && null!=tokens) {
			currentIndex++;

			// Если вышли за пределы списка токенов - всё ок
			if(currentIndex>=tokens.size()) {
				return;
			}

			// Пропускаем токены, которые заканчиваются до startOffset
			while(currentIndex<tokens.size()) {
				Token token = tokens.get(currentIndex);
				int tokenEnd = token.getSP().getPos() + token.getLength();

				if(tokenEnd<=startOffset) {
					currentIndex++;
				}
				else{
					break;
				}
			}
		}
	}

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return null==lspServer ? "" : buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

	public static IElementType getIElementType(TokenType tokenType) {
		return ELEMENT_TYPES.get(tokenType);
	}
}