/*
 * Copyright 2026 konstantin@5277.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.plugin.asm;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.lsp.server.*;
import ru.vm5277.lsp.server.tokens_info.LSPNodeInfo;

public class AsmCompletionContributor extends CompletionContributor {
	public AsmCompletionContributor() {
		extend(
			com.intellij.codeInsight.completion.CompletionType.BASIC,
			PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile(AsmPsiFile.class)),
			new com.intellij.codeInsight.completion.CompletionProvider<>() {
				@Override
				protected void addCompletions(@NotNull CompletionParameters parameters,
											  @NotNull ProcessingContext context,
											  @NotNull CompletionResultSet result) {

					ASMLanguageServer lspServer = AsmSyntaxHighlighterFactory.getLspSever(parameters.getEditor().getProject());
					PsiFile psiFile = parameters.getOriginalFile();
					PsiElement element = psiFile.findElementAt(parameters.getOffset() - 1);

					PsiElement prevElement = PsiTreeUtil.prevVisibleLeaf(element);
					if(	null!= prevElement && null!= prevElement.getNode() &&
						TokenType.DIRECTIVE.name().equals(prevElement.getNode().getElementType().toString()) &&
						prevElement.getText().equals(".include")) {

						String includePath = "";
						if(!element.getText().trim().isEmpty()) {
							int offsetInElement = parameters.getOffset() - element.getTextOffset();
							String textBeforeCaret = element.getText().substring(0, offsetInElement);
							if (!textBeforeCaret.trim().isEmpty()) {
								includePath = textBeforeCaret.split("\\s")[0].replace("\\", "/");
							}
						}
						for (String info : lspServer.getImportSuggestions(includePath.startsWith("\"") ? includePath.substring(1) : includePath)) {
							String info2 = (includePath.startsWith("\"") ? "" : "\"") + info + "\"";
							result.addElement(
								LookupElementBuilder.create(info2)
									.withTypeText("include")
									.withTailText(" (package)", true)
							);
						}
					}

					EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
					for (LSPNodeInfo info : lspServer.getCompletionSuggestions(element.getText().trim())) {
						String type = info.getType().name();
						TextAttributesKey colorForTokenType = AsmSyntaxHighlighter.getColorForTokenType(type);

						java.awt.Color foregroundColor = null;
						if (colorForTokenType != null) {
							var attributes = globalScheme.getAttributes(colorForTokenType);
							if (attributes != null) {
								foregroundColor = attributes.getForegroundColor();
							}
						}
						LookupElementBuilder leb = LookupElementBuilder.create(info.getName())
							.withTypeText(type)
							.withCaseSensitivity(false);

						if (foregroundColor != null) {
							leb = leb.withItemTextForeground(foregroundColor);
						}

						result.addElement(PrioritizedLookupElement.withPriority(leb, info.getPriority()));
					}
				}
			}
		);
	}
}