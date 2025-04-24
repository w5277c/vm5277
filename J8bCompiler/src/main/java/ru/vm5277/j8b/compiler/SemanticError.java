/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

public class SemanticError extends RuntimeException {
    private	final	int	line;
    private	final	int	column;

    public SemanticError(String message, int line, int column) {
        super("[Semantic Error] " + message + " at line " + line + ", column " + column);
        
		this.line = line;
        this.column = column;
    }

    public SemanticError(String message) {
        this(message, -1, -1); // Для случаев, когда позиция неизвестна
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}