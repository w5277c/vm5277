package ru.vm5277.plugin;

import com.intellij.lang.Language;

public class ASMLanguage extends Language {
	public static final ASMLanguage INSTANCE = new ASMLanguage();

	private ASMLanguage() {
		super("ASM", "text/x-asm");
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}
}