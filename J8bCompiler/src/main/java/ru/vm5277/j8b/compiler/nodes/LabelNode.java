/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

public class LabelNode extends AstNode {
	private final String name;

	public LabelNode(TokenBuffer tb) {
		super(tb);

		this.name = tb.consume().getStringValue();
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "label: " + name;
	}
}