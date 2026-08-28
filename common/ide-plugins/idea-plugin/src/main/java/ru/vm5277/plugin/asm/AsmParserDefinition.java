// J8BParserDefinition.java
package ru.vm5277.plugin.asm;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.plugin.ASMLanguage;

public class AsmParserDefinition implements ParserDefinition {
	private static final IFileElementType FILE = new IFileElementType(ASMLanguage.INSTANCE);
	@NotNull
	@Override
	public AsmLexer createLexer(Project project) {
		return new AsmLexer(project); // ваш существующий Lexer
	}

	@NotNull
	@Override
	public PsiParser createParser(Project project) {
		return new AsmParser();
	}

	@NotNull
	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return TokenSet.create(AsmLexer.getIElementType(TokenType.WHITESPACE));
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return TokenSet.create(AsmLexer.getIElementType(TokenType.COMMENT));
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return TokenSet.create(AsmLexer.getIElementType(TokenType.STRING));
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		return new AsmPsiElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new AsmPsiFile(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}
}