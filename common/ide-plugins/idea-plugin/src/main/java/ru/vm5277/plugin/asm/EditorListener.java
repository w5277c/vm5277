package ru.vm5277.plugin.asm;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class EditorListener implements EditorFactoryListener {

	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		Editor editor = event.getEditor();

		if (editor instanceof EditorEx) {
			VirtualFile file = FileDocumentManager.getInstance()
				.getFile(editor.getDocument());

			if (file != null && "asm".equalsIgnoreCase(file.getExtension())) {
				EditorEx editorEx = (EditorEx) editor;

				// Устанавливаем визуальные направляющие
				List<Integer> guides = Arrays.asList(60);
				editorEx.getSettings().setSoftMargins(guides);
				editorEx.getSettings().setUseTabCharacter(true);
			}
		}
	}
}