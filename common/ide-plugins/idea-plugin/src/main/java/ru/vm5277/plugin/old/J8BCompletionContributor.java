package ru.vm5277.plugin.old;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.lsp.server.J8BLanguageServer;

public class J8BCompletionContributor extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters,
                                       @NotNull CompletionResultSet result) {
        PsiFile file = parameters.getOriginalFile();
        J8BLanguageServer server = LanguageServerManager.getJ8BServer();

        // Запрос к твоему серверу
        // server.getCompletions(...);

        // Добавление вариантов
        // result.addElement(...);
    }
}