package ru.vm5277.plugin.old;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.plugin.j8b.FileType;
import ru.vm5277.plugin.J8BLanguage;

public class J8BFile extends PsiFileBase {

    public J8BFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, J8BLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public com.intellij.openapi.fileTypes.FileType getFileType() {
        return FileType.INSTANCE;
    }
}