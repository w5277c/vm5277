/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.List;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;

public class FunctionNode extends AstNode {
	private	Keyword				type;
	private	String				name;
	private	List<ParameterNode>	parameters;
	private	BlockNode			body;

	public FunctionNode(TokenBuffer tb, Keyword type, String name) {
		super(tb);

		this.type = type;
		this.name = name;
		
		//TODO
	}
}
