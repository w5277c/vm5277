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

import ru.vm5277.common.NumUtils;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.tokens.Token;

public class LiteralExpression extends ExpressionNode {
    private Object value;
    
    public LiteralExpression(TokenBuffer tb, MessageContainer mc, Object value) {
        super(tb, mc);
        
		this.value = value;
    }

	@Override
	public VarType getType(Scope scope) {
		if (value == null) return VarType.NULL;
		if (value instanceof Boolean) return VarType.BOOL;
		if (value instanceof Number)  {
			if(value instanceof Double && ((double)value) != ((Double)value).longValue()) return VarType.FIXED;
			
			long l = ((Number)value).longValue();
			if(l<0) return VarType.FIXED;
			if(l<=255) return VarType.BYTE;
			if(l<=65535) return VarType.SHORT;
			return VarType.INT;
		}
		if (value instanceof Character) return VarType.BYTE;
		if (value instanceof String) return VarType.CSTR;
		if (value instanceof VarType) return (VarType)value;
		return VarType.UNKNOWN;
	}	

	public Object getValue() {
		return value;
	}
	
	public boolean isInteger() {
		return value instanceof Integer || value instanceof Long;
	}
	
	public boolean isString() {
		return value instanceof String;
	}

	public boolean isFixed() {
		return (value instanceof Double) || ((value instanceof Number) && 0>((Number)value).longValue());
	}
	
	public long getFixedValue() {
		if(value instanceof Double) {
			return Math.round(((Double)value)*256.0) & 0xffffl;
		}
		return (getNumValue()*256l) & 0xffffl;
	}
	
	public boolean isBoolean() {
		return value instanceof Boolean;
	}
	
	public boolean isNull() {
		return null == value;
	}
	
	public long getNumValue() {
		if(value instanceof Character) {
			return (int)(Character)value;
		}
		else if(value instanceof Boolean) {
			return ((Boolean)value) ? 0x01 : 0x00;
		}
		else if(value instanceof VarType) {
			return ((VarType)value).getId();
		}
		else if(value instanceof Double) {
			return Math.round(((Double)value));
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
		return (String)value;
	}
	
	@Override
	public boolean preAnalyze() {
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());
		
		try {
			if(value instanceof Number) {
				VarType type = getType(scope);
				if(type == VarType.UNKNOWN) {
					markError("Unsupported numeric literal type");
					result = false;
				}
				else {
					// Проверка диапазона через VarType.checkRange
					type.checkRange((Number)value);
				}
			}
		} 
		catch (CompileException e) {
			markError(e.getMessage());
			result = false;
		}
		
		cg.leaveExpression();
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		//TODO попыка вынести запись в аккумулятор на свое место
		if(toAccum) {
			//cg.constToAcc(cgScope, returnType.getSize(), isFixed() ? getFixedValue() : getNumValue(), isFixed());
			long v = isFixed() ? getFixedValue() : getNumValue();
			cg.constToAcc(cgScope, NumUtils.getBytesRequired(v), v, isFixed());
			return CodegenResult.RESULT_IN_ACCUM;
		}

		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + Token.toStringValue(value);
    }
}