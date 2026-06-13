package ru.vm5277.plugin;

import com.intellij.lang.Language;

public class J8BLanguage extends Language {
    public static final J8BLanguage INSTANCE = new J8BLanguage();

    private J8BLanguage() {
        super("J8B");
    }
}