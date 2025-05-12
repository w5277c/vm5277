/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
12.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.messages.ErrorMessage;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.messages.WarningMessage;
import ru.vm5277.j8b.compiler.tokens.Token;

public class FileImporter {
	private	final	List<String>		importedFiles	= new ArrayList<>();
	private	final	String				basePath;
	private	final	MessageContainer	mc;

	public FileImporter(String basePath, MessageContainer mc) {
		this.basePath = basePath;
		this.mc = mc;
	}

	public List<Token> importFile(String importPath) throws IOException {
		String filePath = resolveFilePath(importPath);

		// Проверка циклических зависимостей
		if (importedFiles.contains(filePath)) {
			mc.add(new WarningMessage("Circular import detected: " + importPath, null));
		}
		else {
			importedFiles.add(filePath);
		}

		File file = new File(filePath);
		if (!file.exists()) {
			mc.add(new ErrorMessage("Imported file not found: " + filePath, null));
		}
		else {
			try (FileReader reader = new FileReader(file)) {
				Lexer lexer = new Lexer(reader, mc);
				return lexer.getTokens();
			}
		}
		return new ArrayList<>();
	}

	private String resolveFilePath(String importPath) {
		return basePath + File.separator + importPath.replace('.', File.separatorChar) + ".j8b";
	}
}