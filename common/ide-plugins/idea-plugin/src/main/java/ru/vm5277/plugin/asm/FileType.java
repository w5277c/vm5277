package ru.vm5277.plugin.asm;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.plugin.ASMLanguage;
import ru.vm5277.plugin.J8BLanguage;

import javax.swing.*;

public class FileType extends LanguageFileType {
    public static final FileType INSTANCE = new FileType();

    protected FileType() {
        super(ASMLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "ASM File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "ASM source file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "asm";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Any_type;
    }
}