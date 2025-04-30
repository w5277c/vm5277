/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.enums;

import java.util.HashMap;
import java.util.Map;

public class VarType {
	private	static	final	Map<String, VarType>	CLASS_TYPES = new HashMap<>();
	
	// Примитивные типы
	public	static	final	VarType	VOID		= new VarType("void");
	public	static	final	VarType	BOOL		= new VarType("bool");
	public	static	final	VarType	BYTE		= new VarType("byte");
	public	static	final	VarType	SHORT		= new VarType("short");
	public	static	final	VarType	INT			= new VarType("int");
	public	static	final	VarType	FIXED		= new VarType("fixed") {
		@Override
		public boolean isFixedPoint() { return true; }
	};

	// Ссылочные типы
	public	static	final	VarType	STRING		= new VarType("string");
	public	static	final	VarType	CLASS		= new VarType("class");

	// Специальные типы
	public	static	final	VarType	NULL		= new VarType("null");
	public	static	final	VarType	UNKNOWN		= new VarType("?");

	private	final	String	name;
	private	final	String	className; // Для классовых типов
	private			boolean	isArray;
    private			VarType	elementType; // Для массивов: тип элементов
	private			int		arraySize;

	// Конструктор для ссылочных типов
	private VarType(String name) {
		this.name = name;
		this.className = null;
	}

	// Конструктор для классовых типов
	private VarType(String name, String className) {
		this.name = name;
		this.className = className;
	}

	// Конструктор для массивов
    public static VarType arrayOf(VarType elementType) {
        VarType type = new VarType("array");
        type.isArray = true;
        type.elementType = elementType;
        return type;
    }
	
	public static VarType arrayOf(VarType elementType, int size) {
		VarType type = new VarType("array");
		type.isArray = true;
		type.elementType = elementType;
		type.arraySize = size;
		return type;
	}

	public int getArrayDepth() {
		int depth = 0;
		VarType current = this;
		while (current.isArray()) {
			depth++;
			current = current.getElementType();
		}
		return depth;
	}
	
	// Создаем тип для конкретного класса.
	public static void addClassName(String className) {
		if(!CLASS_TYPES.containsKey(className)) {
			VarType type = new VarType("class:" + className, className);
			CLASS_TYPES.put(className, type);
		}
	}
	public static VarType fromClassName(String className) {
		return CLASS_TYPES.get(className);
	}
	
	// Преобразуем Keyword в VarType.
    public static VarType fromKeyword(Keyword value) {
		if (null == value) return UNKNOWN;

		switch (value) {
			case VOID: return VOID;
			case BOOL: return BOOL;
			case BYTE: return BYTE;
			case SHORT: return SHORT;
			case INT: return INT;
			case FIXED: return FIXED;
			case CLASS: return CLASS;
			default: return UNKNOWN;
		}
	}
	
/*	
	// Преобразует строку в VarType.
	public static VarType fromString(String typeName) {
		// Проверяем примитивные типы
		Keyword kw = Keyword.fromString(typeName);
		if (null != kw) {
			VarType fromKw = fromKeyword(kw);
			if (UNKNOWN != fromKw) return fromKw;
		}

		// Специальные случаи
		if (typeName.equalsIgnoreCase("string")) return STRING;

		// Классовые типы (начинаются с "class:")
		if (typeName.startsWith("class:")) {
			return forClassName(typeName.substring(6));
		}
		return UNKNOWN;
	}
*/
	public String getName() {
		return className != null ? className : name;
	}

	public boolean isClassType() {
		return className != null || this == CLASS;
	}

	public boolean isFixedPoint() {
		return this == FIXED;
	}

	public boolean isPrimitive() {
		return this == BOOL || this == BYTE || this == SHORT || this == INT || this == FIXED;
	}

	public boolean isNumeric() {
		return this == BYTE || this == SHORT || this == INT || this == FIXED;
	}

	public boolean isInteger() {
		return this == BYTE || this == SHORT || this == INT;
	}

	public boolean isVoid() {
		return this == VOID;
	}
	
	public boolean isCompatibleWith(VarType other) {
		if (this == other) return true;

		// Неявное приведение целочисленных типов
		if (this.isInteger() && other.isInteger()) {
			return this.getSize() >= other.getSize();
		}

		// fixed может принимать целочисленные значения
		if (this == FIXED && other.isInteger()) {
			return true;
		}

		// Проверка классовых типов
		if (this.isClassType() && other.isClassType()) {
			if (this.className == null || other.className == null) {
				return false;
			}
			return this.className.equals(other.className);
		}
		return false;
	}

	public int getSize() {
		if (this == BYTE) return 1;
		if (this == SHORT) return 2;
		if (this == INT) return 4;
		return 0;
	}

	public boolean isArray() {
		return isArray;
	}
    
	public VarType getElementType() {
		return elementType;
	}
	
	public Integer getArraySize() {
		return arraySize;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}