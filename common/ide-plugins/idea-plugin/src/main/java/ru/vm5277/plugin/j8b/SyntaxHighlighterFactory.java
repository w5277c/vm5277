package ru.vm5277.plugin.j8b;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class SyntaxHighlighterFactory extends com.intellij.openapi.fileTypes.SyntaxHighlighterFactory {

    @NotNull
    @Override
    public com.intellij.openapi.fileTypes.SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        return new SyntaxHighlighter();
    }
}