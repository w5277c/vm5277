/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public abstract class ExpressionNode extends AstNode {
	public ExpressionNode(TokenBuffer tb) {
		super(tb);
	}
	
	//TODO перенести сюда содержимое ExpressionParser
	
	public abstract VarType semanticAnalyze(SymbolTable symbolTable);
	
	// Для визитора (понадобится при кодогенерации)
    public abstract <T> T accept(ExpressionVisitor<T> visitor);
	
	protected boolean isUnaryOperationValid(VarType type, Operator op) {
		switch (op) {
			case NOT: return VarType.BOOL == type;
			case BIT_NOT: return type.isInteger();
			case PLUS:
			case MINUS: return type.isNumeric();
			default: return false;
		}
	}
}
