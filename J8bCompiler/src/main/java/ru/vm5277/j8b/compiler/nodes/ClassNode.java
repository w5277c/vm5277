/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ClassNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			String			name;
	private			String			parentClassName;
	private			List<String>	interfaces	= new ArrayList<>();
	
	public ClassNode(TokenBuffer tb, Set<Keyword> modifiers, String parentClassName) throws ParseException {
		super(tb);
		
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск class токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();
			VarType.addClassName(this.name);
		}
		catch(ParseException e) {markFirstError(e);} // ошибка в имени, оставляем null
		
        // Парсинг интерфейсов (если есть)
		if (tb.match(Keyword.IMPLEMENTS)) {
			consumeToken(tb);
			while(true) {
				try {
					interfaces.add((String)consumeToken(tb, TokenType.ID).getValue());
				}
				catch(ParseException e) {markFirstError(e);} // встретили не ID интерфейса, пропускаем
				if (!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}
        // Парсинг тела класса
		blocks.add(new ClassBlockNode(tb, name)); // может бросить ParseException, после котрого парсинг файла
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return null == parentClassName ? name : parentClassName + "." + name;
	}
	
	public ClassBlockNode getBody() {
		return blocks.isEmpty() ? null : (ClassBlockNode)blocks.get(0);
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + name + ", " + interfaces;
	}
}
