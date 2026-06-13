package ru.vm5277.plugin.j8b;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class SyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey PRIMITIVE_TYPE =
            createTextAttributesKey("J8B_PRIMITIVE_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME);

    @NotNull
    @Override
    public com.intellij.lexer.Lexer getHighlightingLexer() {
        return new Lexer();  // Адаптер вашего лексера
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        TextAttributesKey key = getColorForTokenType(tokenType.getDebugName().toUpperCase());
        return key != null ? new TextAttributesKey[]{key} : new TextAttributesKey[0x00];
    }


    public static TextAttributesKey getColorForTokenType(String type) {
        return switch (type) {
            // Ключевые слова
            case "LITERAL" -> DefaultLanguageHighlighterColors.KEYWORD;
            case "TYPE" -> PRIMITIVE_TYPE;
            case "COMMAND" -> DefaultLanguageHighlighterColors.KEYWORD;
            case "MODIFIER" -> DefaultLanguageHighlighterColors.KEYWORD;
            case "OOP" -> DefaultLanguageHighlighterColors.KEYWORD;
            case "KEYWORD" -> DefaultLanguageHighlighterColors.KEYWORD;

            // Идентификаторы и метки
            case "IDENTIFIER" -> DefaultLanguageHighlighterColors.IDENTIFIER;
            case "LABEL" -> DefaultLanguageHighlighterColors.FUNCTION_DECLARATION;

            // Числа и ноты
            case "NUMBER" -> DefaultLanguageHighlighterColors.NUMBER;
            case "NOTE" -> DefaultLanguageHighlighterColors.STRING;  // или METADATA

            // Операторы и разделители
            case "OPERATOR" -> DefaultLanguageHighlighterColors.OPERATION_SIGN;
            case "DELIMITER" -> DefaultLanguageHighlighterColors.BRACES;

            // Строки и символы
            case "STRING" -> DefaultLanguageHighlighterColors.STRING;
            case "CHARACTER" -> DefaultLanguageHighlighterColors.STRING;

            // Ошибки
            case "INVALID" -> DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE;

            // Пробелы и комментарии
            case "COMMENT" -> DefaultLanguageHighlighterColors.LINE_COMMENT;

            // ASM специфичные
            case "DIRECTIVE" -> DefaultLanguageHighlighterColors.METADATA;
            case "MNEMONIC" -> DefaultLanguageHighlighterColors.KEYWORD;
            case "INDEX_REG" -> DefaultLanguageHighlighterColors.NUMBER;
            case "MACRO_PARAM" -> DefaultLanguageHighlighterColors.PARAMETER;
            case "REGISTER" -> DefaultLanguageHighlighterColors.NUMBER;
            default -> null;
        };
    }
}