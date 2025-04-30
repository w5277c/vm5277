/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ClassBlockNode extends BlockNode {
	private	List<AstNode>		declarations	= new ArrayList<>();
	
	public ClassBlockNode(TokenBuffer tb, String className) {
		this.tb = tb;
        
		tb.consume(Delimiter.LEFT_BRACE);

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {		
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
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
				name = tb.consume().getStringValue();
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

			if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
				declarations.add(new ArrayDeclarationNode(tb, modifiers, type, name));
			}
			else { // Поле
				declarations.add(new FieldNode(tb, modifiers, type, name));
			}

			// todo обработать вложенных блоков, в том числе и static
		}
		
		tb.consume(Delimiter.RIGHT_BRACE);
    }
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}