package ru.vm5277.plugin.asm;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.plugin.ASMLanguage;

public class AsmPsiFile extends PsiFileBase {
	public AsmPsiFile(@NotNull FileViewProvider viewProvider) {
		super(viewProvider, ASMLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public FileType getFileType() {
		return FileType.INSTANCE;
	}

	@Override
	public String toString() {
		return "ASM File";
	}
}