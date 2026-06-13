package ru.vm5277.plugin.old;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.lsp.server.J8BLanguageServer;
import ru.vm5277.plugin.j8b.FileType;

public class J8BAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {

        // Получаем сервер
        J8BLanguageServer server = LanguageServerManager.getJ8BServer();
        CodeStyleSettings settings = CodeStyle.getSettings(psiElement.getProject());
        int tabSize = settings.getTabSize(FileType.INSTANCE);
        String text = ((PsiFile)psiElement).getText();
        if(text.isEmpty()) return;

        try (SourceBuffer sb = new SourceBuffer(text, tabSize)) {
            for (Token token : server.tokenize(sb)) {
                if (    token.getType() == TokenType.EOF ||
                        token.getType() == TokenType.WHITESPACE ||
                        token.getType() == TokenType.NEWLINE) {
                    continue;
                }

                TextAttributesKey color = getColorForTokenType(token.getType());
                HighlightSeverity hs = TokenType.INVALID == token.getType() ? HighlightSeverity.ERROR :
                        HighlightSeverity.INFORMATION;
                int startOffset = token.getSP().getPos();
                int endOffset = token.getSP().getPos() + token.getLength();
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(new TextRange(startOffset, endOffset))
                        .textAttributes(color)
                        .create();
            }
        } catch (Exception ex) {

        }
    }

    public static TextAttributesKey getColorForTokenType(TokenType type) {
        switch (type) {
            // Ключевые слова
            case LITERAL:
                return DefaultLanguageHighlighterColors.KEYWORD;
            case TYPE:
                return DefaultLanguageHighlighterColors.CLASS_NAME;
            case COMMAND:
                return DefaultLanguageHighlighterColors.KEYWORD;
            case MODIFIER:
                return DefaultLanguageHighlighterColors.KEYWORD;
            case OOP:
                return DefaultLanguageHighlighterColors.KEYWORD;
            case KEYWORD:
                return DefaultLanguageHighlighterColors.KEYWORD;

            // Идентификаторы и метки
            case IDENTIFIER:
                return DefaultLanguageHighlighterColors.IDENTIFIER;
            case LABEL:
                return DefaultLanguageHighlighterColors.FUNCTION_DECLARATION;

            // Числа и ноты
            case NUMBER:
                return DefaultLanguageHighlighterColors.NUMBER;
            case NOTE:
                return DefaultLanguageHighlighterColors.STRING;  // или METADATA

            // Операторы и разделители
            case OPERATOR:
                return DefaultLanguageHighlighterColors.OPERATION_SIGN;
            case DELIMITER:
                return DefaultLanguageHighlighterColors.BRACES;

            // Строки и символы
            case STRING:
                return DefaultLanguageHighlighterColors.STRING;
            case CHARACTER:
                return DefaultLanguageHighlighterColors.STRING;

            // Ошибки
            case INVALID:
                return DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE;

            // Пробелы и комментарии
            case COMMENT:
                return DefaultLanguageHighlighterColors.LINE_COMMENT;

            // ASM специфичные
            case DIRECTIVE:
                return DefaultLanguageHighlighterColors.METADATA;
            case MNEMONIC:
                return DefaultLanguageHighlighterColors.KEYWORD;
            case INDEX_REG:
                return DefaultLanguageHighlighterColors.NUMBER;
            case MACRO_PARAM:
                return DefaultLanguageHighlighterColors.PARAMETER;
            case REGISTER:
                return DefaultLanguageHighlighterColors.NUMBER;
            default:
                return null;
        }
    }
}