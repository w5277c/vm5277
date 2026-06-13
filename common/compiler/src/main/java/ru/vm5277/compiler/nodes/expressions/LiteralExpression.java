/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.compiler.nodes.expressions;

import java.util.Locale;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.tokens.Token;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.StrUtils;
import static ru.vm5277.common.VarType.FIXED_MAX;
import static ru.vm5277.common.VarType.FIXED_MIN;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.Main;

//TODO RTOS умеет округлять? Проверить и выполнять округление по среднему

public class LiteralExpression extends ExpressionNode {
    private Object	value;
	private	boolean	fromVarField	= false; // Получен из переменной или свойства т.е. имеет фиксированный тип не зависимый от значения
	private	boolean	isOverflow		= false; // Включен если при вычислении с final VarField значение превысило диапазон типа переменной или свойства
	
	public LiteralExpression(Instance inst, TokenBuffer tb, Object value) {
        super(inst, tb);
        
		this.value = value;
		type = VarType.UNKNOWN;
		
		if(value instanceof Double) {
			if(((Double)value)>0 && 0.000000001d>(((Double)value)-((Double)value).longValue())) {
				value = ((Double)value).longValue();
			}
		}
			
		
		if(null==value) type = VarType.NULL;
		else if(value instanceof Boolean) type = VarType.BOOL;
		else if(value instanceof Character) type = VarType.CHAR;
		else if(value instanceof String) type = VarType.CSTR;
		else if(value instanceof VarType) type = (VarType)value;
		else if(value instanceof Number)  {
			if(value instanceof Double)	{
				type = VarType.FIXED;
			}
			else {
				long l = ((Number)value).longValue();
				if(l<0) type = VarType.FIXED;
				else if(l<=255) type = VarType.BYTE;
				else if(l<=65535) type = VarType.SHORT;
				else type = VarType.INT;
			}
		}
    }

	public LiteralExpression(Instance inst, TokenBuffer tb, Number value, VarType type) {
        super(inst, tb);
        
		this.value = value;
		this.type = type;
		this.fromVarField = true;
		this.isOverflow = false; //TODO overflow должен быть посчитан в самом конце всех операций
	}
	
	void setVarFieldType(VarType type) {
		this.type = type;
		fromVarField = true;
	}

	public boolean fromVarField() {
		return fromVarField;
	}
	
	public Object getValue() {
		return value;
	}
	
	public long getFixedValue() {
		if(value instanceof Double) {
			return ((long)(((Double)value)*256.0)) & 0xffffl;
		}
		long result = ((Number)value).longValue();
		if(0>result) {
			return (result * 256L) & 0xffffl;
		}
		return (result*256l) & 0xffffl;
	}
	
	public boolean isBoolean() {
		return value instanceof Boolean;
	}
	
	public boolean isNull() {
		return null == value;
	}
	
	public long getNumValue() {
		if(null == value) {
			return 0;
		}
		else if(value instanceof Character) {
			return (int)(Character)value;
		}
		else if(value instanceof Boolean) {
			return ((Boolean)value) ? 0x01 : 0x00;
		}
		else if(value instanceof VarType) {
			return ((VarType)value).getId();
		}
		else if(value instanceof Double) {
			return ((Double)value).longValue();
		}
		else {
			long result = ((Number)value).longValue();
			if(0>result) return 0l;
			return result;
		}
	}
	
	public boolean getBooleanValue() {
		if(value instanceof Boolean) return (Boolean)value;
		if(value instanceof Number) return 0!=((Number)value).longValue();
		if(value instanceof Double) return 0.0!=((Double)value).doubleValue();
		return false;
	}
	
	public String getStringValue() {
		if(value instanceof Boolean) return ((Boolean)value).toString();
		if(value instanceof Character) return StrUtils.escapeChar((Character)value);
		if(value instanceof Double) {
			double truncated = ((long)(((Double)value) * 100)) / 100.0;
			return String.format(Locale.ENGLISH, "%.2f", truncated);
		}
		if(value instanceof Number) {
			if(VarType.CHAR==type) {
				return StrUtils.escapeChar((char)(((Number)value).intValue()));
			}
			return ((Number)value).toString();
		}
		return (String)value;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());
		
		if(value instanceof Number) {
			if(VarType.UNKNOWN==type) {
				markError("Unsupported numeric literal type");
				result = false;
			}
			else {
				if(type.isInteger()) {
					long l = getNumValue();
					if(value instanceof Double) {
						Double d = ((Double)value);
						if(0>d) {
							l = 0;
							if(StrictLevel.NONE==Main.getStrictLevel()) {
							}
							else if(StrictLevel.STRONG==Main.getStrictLevel()) {
								markError(String.format("Value is negative. Given: %.2f", d));
								result = false;
							}
							else {
								markWarning(String.format("Value is negative. Given: %.2f", d) + ", truncated: " + l);
							}
						}
						else if((d - Math.floor(d)) >= 0.01d) {
							if(StrictLevel.NONE==Main.getStrictLevel()) {
							}
							else if(StrictLevel.STRONG==Main.getStrictLevel()) {
								markError(String.format("Value is not integer. Given: %.2f", d));
								result = false;
							}
							else {
								markWarning(String.format("Value is not integer. Given: %.2f", d) + ", truncated: " + l);
							}
						}
						value = l;
					}

					if(VarType.INT==type && (l<0 || l> 0xffffffffl)) {
						long newValue = l & 0xffffffffl;
						if(StrictLevel.NONE==Main.getStrictLevel()) {
						}
						else if(StrictLevel.STRONG==Main.getStrictLevel()) {
							markError("Value out of range (0..4294967295). Given: " + l);
							result = false;
						}
						else {
							markWarning("Value out of range (0..4294967295). Given: " + l + ", truncated:" + newValue);
						}
						value = newValue;
					}
				}
				else if(VarType.FIXED==type) {
					double d = (value instanceof Double ? ((Double)value) : (((Number)value).doubleValue()));
					if(d<FIXED_MIN || d>FIXED_MAX) {
						int tmp = (int)(d * 256d);
						Double newValue = ((short)((tmp & 0x7FFF) | (tmp & 0x8000))) / 256d;

						if(StrictLevel.NONE==Main.getStrictLevel()) {
						}
						else if(StrictLevel.STRONG==Main.getStrictLevel()) {
							markError(String.format("fixed value out of range (" + FIXED_MIN + ".." + FIXED_MAX + "). Given: %.2f", d));
							result = false;
						}
						else {
							markWarning(String.format("Value out of range (" + FIXED_MIN + ".." + FIXED_MAX + "). Given: %.2f", d) +
										String.format(", truncated: %.2f", newValue));
						}
						value = newValue;
					}
				}
			}
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	public void valueTruncate() {
		if(value instanceof Number) {
			if(type.isInteger()) {
				long l = getNumValue();
				int bits = type.getSize()*8;
				long mask = (1L<<bits) - 1;
				value = l & mask;
			}
			else if(VarType.FIXED==type) {
				double d = (value instanceof Double ? ((Double)value) : (((Number)value).doubleValue()));
				if(d<FIXED_MIN || d>FIXED_MAX) {
					int tmp = (int)(d * 256d);
					value = ((short)((tmp & 0x7FFF) | (tmp & 0x8000))) / 256d;
				}
			}
		}
	}

	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		//cg.setDataSymbol(cg.defineData(vScope.getResId(), -1, (String)le.getValue()));
		//TODO попыка вынести запись в аккумулятор на свое место
		if(toAccum) {
			//cg.constToAcc(cgScope, returnType.getSize(), isFixed() ? getFixedValue() : getNumValue(), isFixed());
			if(type.isString()) {
				cg.cellsToAcc(cgScope, new CGCells(cg.defineData(cg.genId(), -1, value).getLabel()), false);
			}
			else {
				long v = type.isFixedPoint() ? getFixedValue() : getNumValue();
				// LiteralExpression не должен задавать размер аккумулятору! А кто должен? - не актуально
				// Меняем логику - было ошибкой ограничивать размер аккумулятора по типу назначения, теперь размер аккумулятора должне соотвествовать значению
				cg.constToAcc(cgScope, 0, v, type.isFixedPoint());
				if(isOverflow) {
					Integer exceptionId = cg.getExcsChecker().getHandled(excs, "MathOverflowException");
					if(null!=exceptionId) {
						cg.getExcsChecker().makeException(cg, cgScope, excs, exceptionId);
					}
				}
			}
			return CodegenResult.RESULT_IN_ACCUM;
		}

		return null;
	}
	
	@Override
	public String toString() {
		return Token.toStringValue(value);
    }
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
    }
}
