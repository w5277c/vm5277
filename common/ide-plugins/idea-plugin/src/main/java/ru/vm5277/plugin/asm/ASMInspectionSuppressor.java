package ru.vm5277.plugin.asm;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ASMInspectionSuppressor implements InspectionSuppressor {

	@Override
	public boolean isSuppressedFor(@NotNull PsiElement element,
								   @NotNull String toolId) {
		// Отключаем все инспекции для ASM
		return true;
	}

	@NotNull
	@Override
	public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element,
												 @NotNull String toolId) {
		return SuppressQuickFix.EMPTY_ARRAY;
	}
}