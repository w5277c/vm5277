/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

public class SemanticError extends RuntimeException {
    private	final	SourcePosition	sp;

    public SemanticError(String message) {
        super("[Semantic Error] " + message);
        
		this.sp = null;
    }

	public SemanticError(String message, SourcePosition sp) {
        super("[Semantic Error] " + message + " at " + sp.toString());
        
		this.sp = sp;
    }
}