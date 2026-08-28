// J8BPsiFile.java
package ru.vm5277.plugin.j8b;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.plugin.J8BLanguage;

public class J8BPsiFile extends PsiFileBase {
	public J8BPsiFile(@NotNull FileViewProvider viewProvider) {
		super(viewProvider, J8BLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public FileType getFileType() {
		return FileType.INSTANCE;
	}

	@Override
	public String toString() {
		return "J8B File";
	}
}