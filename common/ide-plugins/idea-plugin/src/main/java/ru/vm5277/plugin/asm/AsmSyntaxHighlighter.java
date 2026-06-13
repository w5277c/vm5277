package ru.vm5277.plugin.asm;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.enums.PlatformType;

import java.awt.*;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class AsmSyntaxHighlighter extends SyntaxHighlighterBase {
    private	Project		project;
    private	VirtualFile	virtualFile;
	private PlatformType platformType;
	private String mcu;

    private static class LazyHolder {
        public static final TextAttributesKey COLOR_MACRO = createTextAttributesKey(
                "DARK_RED",
                new TextAttributes(
                        new Color(255, 92, 54),
                        null,
                        null,
                        null,
                        Font.PLAIN
                )
        );
        public static final TextAttributesKey COLOR_REGS = createTextAttributesKey(
                "DARK_RED",
                new TextAttributes(
                        new Color(184, 132, 11),
                        null,
                        null,
                        null,
                        Font.PLAIN
                )
        );
    }

    public AsmSyntaxHighlighter(Project project, VirtualFile virtualFile, PlatformType platformType, String mcu) {
        this.project = project;
        this.virtualFile = virtualFile;
		this.platformType  = platformType;
		this.mcu = mcu;
    }

    @NotNull
    @Override
    public com.intellij.lexer.Lexer getHighlightingLexer() {
        return new AsmLexer(project, platformType, mcu);
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		String name = tokenType.getDebugName().toUpperCase();
		TextAttributesKey key = getColorForTokenType(name);
		return key != null ? new TextAttributesKey[]{key} : new TextAttributesKey[0x00];
    }


    public static TextAttributesKey getColorForTokenType(String type) {
        return switch (type) {
            // Ключевые слова
            case "LITERAL" -> DefaultLanguageHighlighterColors.KEYWORD;
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
			case "MACRO" -> LazyHolder.COLOR_MACRO;
			case "INDEX_REG" -> LazyHolder.COLOR_REGS;
            case "MACRO_PARAM" -> DefaultLanguageHighlighterColors.PARAMETER;
            case "REGISTER" -> LazyHolder.COLOR_REGS;
            default -> null;
        };
    }
}