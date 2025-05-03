/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.Set;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ClassBlockNode extends BlockNode {
	public ClassBlockNode(TokenBuffer tb, String className) throws ParseException {
		this.tb = tb;
        
		consumeToken(tb, Delimiter.LEFT_BRACE); // в случае ошибки, останавливаем парсинг файла

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {		
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP, Keyword.CLASS)) {
				declarations.add(new ClassNode(tb, modifiers, null));
				continue;
			}

			// Определение типа (примитив, класс или конструктор)
			VarType type = checkPrimtiveType();
			boolean isClassName = false;
			if (null == type) {
				isClassName = checkClassName(className);
				if(!isClassName) type = checkClassType();
			}
			
			// Получаем имя метода/конструктора
			String name = null;
			if(tb.match(TokenType.ID)) {
				if(isClassName) {
					isClassName = false;
					type = VarType.fromClassName(className);
				}
				name = consumeToken(tb).getStringValue();
			}

			if(tb.match(Delimiter.LEFT_PAREN)) { //'(' Это метод
				if(isClassName) {
					declarations.add(new MethodNode(tb, modifiers, null, className));
				}
				else {
					declarations.add(new MethodNode(tb, modifiers, type, name));
				}
				continue;
			}

			if(null != type) {
				if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
					declarations.add(new ArrayDeclarationNode(tb, modifiers, type, name));
				}
				else { // Поле
					declarations.add(new FieldNode(tb, modifiers, type, name));
				}
			}

			if(tb.match(Delimiter.LEFT_BRACE)) {
				tb.getLoopStack().add(this);
				try {
					BlockNode blockNode = new BlockNode(tb);
					declarations.add(blockNode);
					blocks.add(blockNode);
				}
				catch(ParseException e) {}
				tb.getLoopStack().remove(this);
			}
		}
		
		//Попытка потребить '}'
		try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(ParseException e) {markFirstError(e);}
    }
}