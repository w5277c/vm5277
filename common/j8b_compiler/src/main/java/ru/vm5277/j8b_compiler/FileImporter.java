/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
12.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.j8b_compiler.tokens.Token;

public class FileImporter {
	private	final	List<String>		importedFiles	= new ArrayList<>();
	private	final	Path				runtimePath;
	private	final	Path				basePath;
	private	final	MessageContainer	mc;

	public FileImporter(Path runtimePath, Path basePath, MessageContainer mc) {
		this.runtimePath = runtimePath;
		this.basePath = basePath;
		
		this.mc = mc;
	}

	public List<Token> importFile(String importPath) throws IOException {
		// Проверка циклических зависимостей
		if (importedFiles.contains(importPath)) {
			mc.add(new WarningMessage("Circular import detected: " + importPath, null));
		}
		else {
			importedFiles.add(importPath);
		}

		File file = runtimePath.resolve(importPath).normalize().toFile();
		if (!file.exists()) {
			file = basePath.resolve(importPath).normalize().toFile();
			if(!file.exists()) {
				mc.add(new ErrorMessage("Imported file not found: " + importPath, null));
				file = null;
			}
		}
		if(null != file) {
			Lexer lexer = new Lexer(file, mc);
			return lexer.getTokens();
		}
		return new ArrayList<>();
	}
}