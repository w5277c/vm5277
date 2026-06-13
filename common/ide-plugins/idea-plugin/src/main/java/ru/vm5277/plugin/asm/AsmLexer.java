package ru.vm5277.plugin.asm;

import com.intellij.application.options.CodeStyle;
import com.intellij.lexer.LexerBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.lsp.server.ASMLanguageServer;
import ru.vm5277.plugin.ASMLanguage;
import ru.vm5277.plugin.j8b.FileType;

import java.util.*;

public class AsmLexer extends LexerBase {
    private	Project     		project;
    private	int					tabSize;
	private ASMLanguageServer	server;
	private Map<TokenType, IElementType> elementTypes	= new HashMap();
	private	List<Token> 		tokens;
    private	int         		currentIndex 	= 0;
	private	int         		startOffset;
	private	int         		endOffset;
    private	CharSequence		buffer;

    public AsmLexer(Project project, PlatformType platformType, String mcu) {
		this.project = project;
		CodeStyleSettings settings = CodeStyle.getSettings(project);
		tabSize = settings.getTabSize(FileType.INSTANCE);



		if(null!=platformType) {
			//TODO необходимо загружать из бибилиотеки тулкита, иначе плагин будет привязан к конретной версии тулкита.
			//Особенно чувствительно при изменениях в binding.cfg
			server = new ASMLanguageServer(platformType, mcu);
		}
    }

	@Override
	public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.currentIndex = 0;

		if(null==server || buffer.isEmpty()) {
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
			this.tokens = server.tokenize(sb);
//			for(Token token : tokens) {
//				System.out.println("Token: " + token.toString());
//			}

			// Пропускаем токены до startOffset
			while (	currentIndex < tokens.size() &&
					tokens.get(currentIndex).getSP().getPos() +	tokens.get(currentIndex).getLength() <= startOffset) {

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
		if (server == null || tokens == null || currentIndex >= tokens.size()) {
			return null;
		}

		Token token = tokens.get(currentIndex);
		int tokenStart = token.getSP().getPos();
		int tokenEnd = tokenStart + token.getLength();

		// Токен полностью до начала - не показываем
		if (tokenEnd <= startOffset) {
			return null;
		}

		// Токен начинается после конца - не показываем
		if (tokenStart >= endOffset) {
			return null;
		}

		IElementType ieet = elementTypes.get(token.getType());
		if(null==ieet) {
			ieet = new IElementType(token.getType().name(), ASMLanguage.INSTANCE);
			elementTypes.put(token.getType(), ieet);
		}
		return ieet;
	}
	
    @Override
    public int getTokenStart() {
        return null==server ? 0 : tokens.get(currentIndex).getSP().getPos();
    }

    @Override
    public int getTokenEnd() {
		if (server == null || tokens == null || currentIndex >= tokens.size()) {
			return endOffset;
		}

		Token token = tokens.get(currentIndex);
		int tokenEnd = token.getSP().getPos() + token.getLength();

		// ВАЖНО: не выходить за границы буфера
		return Math.min(tokenEnd, endOffset);
	}

    @Override
	public void advance() {
		if (server != null && tokens != null) {
			currentIndex++;

			// Если вышли за пределы списка токенов - всё ок
			if (currentIndex >= tokens.size()) {
				return;
			}

			// Пропускаем токены, которые заканчиваются до startOffset
			while (currentIndex < tokens.size()) {
				Token token = tokens.get(currentIndex);
				int tokenEnd = token.getSP().getPos() + token.getLength();

				if (tokenEnd <= startOffset) {
					currentIndex++;
				} else {
					break;
				}
			}
		}
	}

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return null == server ? "" : buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}