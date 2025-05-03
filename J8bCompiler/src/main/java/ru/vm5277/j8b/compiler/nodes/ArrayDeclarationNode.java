/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;

public class ArrayDeclarationNode extends AstNode {
    private	final	Set<Keyword>	modifiers;
	private	final	VarType			elementType;
    private	final	String			name;
	private			ExpressionNode	size;
	private			ExpressionNode	initializer;

	public ArrayDeclarationNode(TokenBuffer tb, Set<Keyword> modifiers, VarType type, String name) {
		super(tb);
		
		this.modifiers = modifiers;
		this.elementType = type;
		this.name = name;
		
		consumeToken(tb); // Потребляем '['
        
        // Размер массива
        try{size = new ExpressionNode(tb).parse();}  catch(ParseException e) {markFirstError(e);}
        
        try {consumeToken(tb, Delimiter.RIGHT_BRACKET);} catch(ParseException e) {markFirstError(e);} // Потребляем ']'

        // Инициализация (опционально)
        if (tb.match(Operator.ASSIGN)) {
            consumeToken(tb);
            try {initializer = new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
        }
		else {
			initializer = null;
		}
		
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}
}