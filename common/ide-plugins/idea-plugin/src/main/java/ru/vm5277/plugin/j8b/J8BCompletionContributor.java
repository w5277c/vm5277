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

package ru.vm5277.plugin.j8b;

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
import ru.vm5277.lsp.server.J8BLanguageServer;
import ru.vm5277.lsp.server.tokens_info.*;
import ru.vm5277.plugin.J8BLanguage;

public class J8BCompletionContributor extends CompletionContributor {

	private final J8BLanguageServer server = new J8BLanguageServer();

	public J8BCompletionContributor() {
		extend(
			com.intellij.codeInsight.completion.CompletionType.BASIC,
			PlatformPatterns.psiElement().withLanguage(J8BLanguage.INSTANCE),
			new com.intellij.codeInsight.completion.CompletionProvider<>() {
				@Override
				protected void addCompletions(@NotNull CompletionParameters parameters,
											  @NotNull ProcessingContext context,
											  @NotNull CompletionResultSet result) {

                    PsiFile psiFile = parameters.getOriginalFile();
                    PsiElement element = psiFile.findElementAt(parameters.getOffset() - 1);
                    StringBuilder qualifiedPathSb = new StringBuilder();
                    String prefix = element.getText();
                    PsiElement prevElement = extractQualifiedPath(prefix.equals(".") ? element : PsiTreeUtil.prevVisibleLeaf(element), qualifiedPathSb);
                    if(null!=prevElement && null!=element.getNode() &&
                        TokenType.KEYWORD.toString().equals(prevElement.getNode().getElementType().toString()) &&
                        prevElement.getText().equals("import")) {

                        for (String info : server.getImportSuggestions(qualifiedPathSb.toString() + (prefix.equals(".") ? "" : element.getText().trim()))) {
                            String displayPath = info.substring(qualifiedPathSb.toString().length());
                            result.addElement(
                                    LookupElementBuilder.create(displayPath)
                                            .withTypeText("import")
                                            .withTailText(" (package)", true)
                            );
                        }
                    }
                    else {
                        EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
                        int dotIndex = qualifiedPathSb.toString().lastIndexOf('.');

                        if(dotIndex>=0) {
                            String className = qualifiedPathSb.substring(0, qualifiedPathSb.length()-1);//extractClassName(qualifiedPath, dotIndex);
                            LSPNodeInfo nodeInfo = server.findNode(className, psiFile.getVirtualFile().getPath());
                            if(null!=nodeInfo) {
                                if(nodeInfo instanceof LSPNodeInfoHasMethods) {
                                    if(null != ((LSPNodeInfoHasMethods) nodeInfo).getMethods()) {
                                        for(LSPMethodInfo method : ((LSPNodeInfoHasMethods) nodeInfo).getMethods()) {
											result.addElement(
                                            	LookupElementBuilder.create(method.getName() + "(" + method.argsToString() + ");")
													.withPresentableText(method.getName() + "(" + method.paramtersToString() + ");")
                                                    .withTypeText(method.getVarType())
                                            );
                                        }
                                    }
                                }
                                if(nodeInfo instanceof LSPClassInfo) {
									for(LSPNodeInfo info : ((LSPClassInfo)nodeInfo).getInternals()) {
										String type = info.getType().name();
										TextAttributesKey colorForTokenType = SyntaxHighlighter.getColorForTokenType(type);
										java.awt.Color foregroundColor = null;
										if(null!=colorForTokenType) {
											var attributes = globalScheme.getAttributes(colorForTokenType);
											if(null!=attributes) {
												foregroundColor = attributes.getForegroundColor();
											}
										}

										String descr = (info.getKind().equals("keyword") ? type : info.getKind());
										LookupElementBuilder leb = LookupElementBuilder.create(info.getName())
											.withTypeText(descr)
											.withCaseSensitivity(true);

										if(null!=foregroundColor) {
											leb = leb.withItemTextForeground(foregroundColor);
										}
										result.addElement(PrioritizedLookupElement.withPriority(leb, info.getPriority()));
									}
                                    for(LSPFieldInfo field : ((LSPClassInfo) nodeInfo).getFields()) {
										result.addElement(
											LookupElementBuilder.create(field.getName())
												.withTypeText(field.getKind())
                                        );
                                    }
                                }
                                if(nodeInfo instanceof LSPExceptionInfo) {
                                    for(String item : ((LSPExceptionInfo) nodeInfo).getItems()) {
                                        result.addElement(
                                                LookupElementBuilder.create(item)
                                                        .withTypeText("code")
                                        );
                                    }
                                }
                            }
                        }
						else {
                            for(LSPNodeInfo info : server.getCompletionSuggestions(prefix, psiFile.getVirtualFile().getPath())) {
                                String type = info.getType().name();
                                TextAttributesKey colorForTokenType = SyntaxHighlighter.getColorForTokenType(type);
                                java.awt.Color foregroundColor = null;
                                if(null!=colorForTokenType) {
                                    var attributes = globalScheme.getAttributes(colorForTokenType);
                                    if(null!=attributes) {
                                        foregroundColor = attributes.getForegroundColor();
                                    }
                                }

                                String descr = (info.getKind().equals("keyword") ? type : info.getKind());
                                LookupElementBuilder leb = LookupElementBuilder.create(info.getName())
                                        .withTypeText(descr)
                                        .withCaseSensitivity(true);

                                if(null!=foregroundColor) {
                                    leb = leb.withItemTextForeground(foregroundColor);
                                }
                                result.addElement(PrioritizedLookupElement.withPriority(leb, info.getPriority()));
                            }
                        }
                    }
				}
			}
		);
	}

    private PsiElement extractQualifiedPath(PsiElement element, StringBuilder path) {
        while(null!=element && null!=element.getNode()) {
            String type = element.getNode().getElementType().toString();
            if(type.equals(TokenType.DELIMITER.name()) && element.getText().equals(".")) {
                element = PsiTreeUtil.prevVisibleLeaf(element);
                type = element.getNode().getElementType().toString();
                if (type.equals(TokenType.IDENTIFIER.name())) {
                    path.insert(0, element.getText() + ".");
                    element = PsiTreeUtil.prevVisibleLeaf(element);
                }
				else {
                    path.setLength(0);
                    return null;
                }
            }
			else return element;
        }
        return element;
    }
}