// J8BParserDefinition.java
package ru.vm5277.plugin.j8b;

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
import ru.vm5277.plugin.J8BLanguage;

public class J8BParserDefinition implements ParserDefinition {
	private static final IFileElementType FILE = new IFileElementType(J8BLanguage.INSTANCE);

	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		return new Lexer(); // ваш существующий Lexer
	}

	@NotNull
	@Override
	public PsiParser createParser(Project project) {
		return new J8BParser();
	}

	@NotNull
	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		return new J8BPsiElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new J8BPsiFile(viewProvider);
	}

	@Override
	public SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}
}