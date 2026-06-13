package ru.vm5277.plugin.j8b;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.plugin.J8BLanguage;

import javax.swing.*;

public class FileType extends LanguageFileType {
    public static final FileType INSTANCE = new FileType();

    protected FileType() {
        super(J8BLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "J8B File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "J8B source file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "j8b";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Any_type;
    }
}