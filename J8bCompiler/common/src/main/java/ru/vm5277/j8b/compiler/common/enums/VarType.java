/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common.enums;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;

public class VarType {
	private	static	final	Map<String, VarType>	CLASS_TYPES = new HashMap<>();
	static {
		CLASS_TYPES.put("Object", new VarType("class:Object", "Object"));
	}
	// Примитивные типы	 Object имеет id 0
	public	static	final	VarType	VOID		= new VarType(1, "void");
	public	static	final	VarType	BOOL		= new VarType(2, "bool");
	public	static	final	VarType	BYTE		= new VarType(3, "byte");
	public	static	final	VarType	SHORT		= new VarType(4, "short");
	public	static	final	VarType	INT			= new VarType(5, "int");
	public	static	final	VarType	FIXED		= new VarType(6, "fixed") {
		@Override
		public boolean isFixedPoint() { return true; }
	};
	public	static	final	VarType	CSTR		= new VarType(7, "cstr");
	
	// Ссылочные типы
	public	static	final	VarType	CLASS		= new VarType(-1, "class");

	// Специальные типы
	public	static	final	VarType	NULL		= new VarType(8, "null");
	public	static	final	VarType	UNKNOWN		= new VarType(-1, "?");

	private	static			int		idCntr		= 10;
	private					int		id;
	private			final	String	name;
	private			final	String	className; // Для классовых типов
	private					boolean	isArray;
    private					VarType	elementType; // Для массивов: тип элементов
	private					int		arraySize;

	// Конструктор для ссылочных типов
	private VarType(int id, String name) {
		this.id = id;
		this.name = name;
		this.className = null;
	}

	// Конструктор для классовых типов
	private VarType(String name, String className) {
		this.id = idCntr++;
		this.name = name;
		this.className = className;
	}

	// Конструктор для массивов
    public static VarType arrayOf(VarType elementType) {
        VarType type = new VarType(9, "array");
        type.isArray = true;
        type.elementType = elementType;
        return type;
    }
	
	public static VarType arrayOf(VarType elementType, int size) {
		VarType type = new VarType(9, "array");
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
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return className != null ? className : name;
	}

	public boolean isClassType() {
		return className != null || this == CLASS;
	}
	
	public String getClassName() {
		return className;
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