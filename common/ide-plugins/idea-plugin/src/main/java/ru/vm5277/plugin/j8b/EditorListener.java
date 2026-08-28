/*
 * Copyright 2026 konstantin@5277.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.plugin.j8b;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.exceptions.CompilationAbortedException;
import ru.vm5277.lsp.server.J8BLanguageServer;
import java.util.Arrays;
import java.util.List;

public class EditorListener implements EditorFactoryListener {
    private final static J8BLanguageServer server = new J8BLanguageServer();
	static {
		VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
			@Override
			public void contentsChanged(@NotNull VirtualFileEvent event) {
				VirtualFile file = event.getFile();
				if (null!=file && "j8b".equalsIgnoreCase(file.getExtension())) {
					try {
                        server.updateImportsFromFile(file.getPath());
                    }
                    catch (CompilationAbortedException ignored) {}
				}
			}
		});
	}

	@Override
	public void editorCreated(@NotNull EditorFactoryEvent event) {
		Editor editor = event.getEditor();

		if (editor instanceof EditorEx) {
			VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());

			if (file != null && "j8b".equalsIgnoreCase(file.getExtension())) {
				EditorEx editorEx = (EditorEx) editor;

				// Устанавливаем визуальные направляющие
				List<Integer> guides = Arrays.asList(160);
				editorEx.getSettings().setSoftMargins(guides);
				editorEx.getSettings().setUseTabCharacter(true);
                try {
                    server.updateImportsFromFile(file.getPath());
                }
                catch (CompilationAbortedException ignored) {}
			}
		}
	}

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Можно очистить кеш при закрытии файла
        Editor editor = event.getEditor();
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file != null && "j8b".equalsIgnoreCase(file.getExtension())) {
            server.invalidateCache(file.getPath()); // нужно добавить этот метод
        }
    }
}