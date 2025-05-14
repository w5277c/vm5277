/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.enums;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class VarType {
	private	static	final	Map<String, VarType>	CLASS_TYPES = new HashMap<>();
	static {
		CLASS_TYPES.put("Object", new VarType("class:Object", "Object"));
	}
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
	public	static	final	VarType	CSTR		= new VarType("cstr");
	
	// Ссылочные типы
	public	static	final	VarType	CLASS		= new VarType("class");

	// Специальные типы
	public	static	final	VarType	NULL		= new VarType("null");
	public	static	final	VarType	UNKNOWN		= new VarType("?");

	private			final	String	name;
	private			final	String	className; // Для классовых типов
	private					boolean	isArray;
    private					VarType	elementType; // Для массивов: тип элементов
	private					int		arraySize;

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
	public static VarType addClassName(String className) {
		if(!CLASS_TYPES.containsKey(className)) {
			VarType type = new VarType("class:" + className, className);
			CLASS_TYPES.put(className, type);
			return type;
		}
		return null;
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
			case CSTR: return CSTR;
			case CLASS: return CLASS;
			default: return UNKNOWN;
		}
	}
	
	public String getName() {
		return className != null ? className : name;
	}

	public boolean isClassType() {
		return className != null || this == CLASS;
	}

	public boolean isFixedPoint() {
		return this == FIXED;
	}

	public boolean isBoolean() {
		return this == BOOL;
	}
	
	public boolean isPrimitive() {
		return this == BOOL || this == BYTE || this == SHORT || this == INT || this == FIXED || this == CSTR;
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
	
	public boolean isReferenceType() {
		return this == VarType.CLASS || this.isArray() || this == VarType.CSTR;
	}

	public boolean isCompatibleWith(Scope scope, VarType other) {
		// Проверка одинаковых типов
		if (this == other) return true;

		// Специальные случаи для NULL
		if (VarType.NULL == this || VarType.NULL == other) return this.isReferenceType() || other.isReferenceType();

		// Большинство типов можно объединить со строковой константой
		if(VarType.CSTR == this && VarType.VOID != other) return true;

		// Проверка числовых типов
		if (this.isNumeric() && other.isNumeric()) {
			// FIXED совместим только с FIXED
			if (this == VarType.FIXED || other == VarType.FIXED) return this == VarType.FIXED && other == VarType.FIXED;

			// Для арифметических операций разрешаем смешивание любых целочисленных типов
			return true;
		}

		// Проверка классовых типов
		if (this.isClassType() && other.isClassType()) {
			if(null == this.className) return false;
			if(this.className.equals(other.className)) return true;
			return null != scope.resolveInterface(this.getName());
		}
		
		// Проверка массивов
		if (this.isArray() && other.isArray()) return this.elementType.isCompatibleWith(scope, other.elementType);
    
		return false;
	}
	
	public void checkRange(Number num) throws SemanticException {
		if (null == num) throw new SemanticException("Value cannot be null");
		
		if(isInteger()) {
			long l = num.longValue();
			if(this == BYTE && (l < 0 || l > 0xff)) throw new SemanticException("byte value out of range (0..255). Given:" + l);
			if(this == SHORT && (l < 0 || l > 0xffff)) throw new SemanticException("short value out of range (0..65535). Given: " + l);
			if(this == INT && (l < 0 || l > 0xffffffffl)) throw new SemanticException("int value out of range (0..4294967295). Given: " + l);
		}
		else if(this == FIXED) {
			double d = (num instanceof Double ? ((Double)num) : (num.doubleValue()));
			if(d<-128.0 || d > 127.99609375d) throw new SemanticException(String.format("fixed value out of range (-128.0..127.99609375). Given: %.8f", d));
		}
	}
	
	public int getSize() {
		if (this == BYTE) return 1;
		if (this == SHORT) return 2;
		if (this == FIXED) return 2;
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
		return isArray ? getElementType() + "[]" : getName();
	}
}