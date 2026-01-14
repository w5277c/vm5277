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

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.tokens.Token;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;

public class LiteralExpression extends ExpressionNode {
    private Object value;
    
    public LiteralExpression(TokenBuffer tb, MessageContainer mc, Object value) {
        super(tb, mc);
        
		this.value = value;
		type = VarType.UNKNOWN;
		
		if(null==value) type = VarType.NULL;
		else if(value instanceof Boolean) type = VarType.BOOL;
		else if(value instanceof Character) type = VarType.BYTE;
		else if(value instanceof String) type = VarType.CSTR;
		else if(value instanceof VarType) type = (VarType)value;
		else if(value instanceof Number)  {
			if(value instanceof Double && ((double)value)!=((Double)value).longValue()) type = VarType.FIXED;
			else {
				long l = ((Number)value).longValue();
				if(l<0) type = VarType.FIXED;
				else if(l<=255) type = VarType.BYTE;
				else if(l<=65535) type = VarType.SHORT;
				else type = VarType.INT;
			}
		}
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
		boolean result = true;
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());
		
		try{
			if(value instanceof Number) {
				if(VarType.UNKNOWN==type) {
					markError("Unsupported numeric literal type");
					result = false;
				}
				else {
					// Проверка диапазона через VarType.checkRange
					type.checkRange((Number)value);
				}
			}
		} 
		catch(CompileException e) {
			markError(e.getMessage());
			result = false;
		}
		
		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		CGScope cgs = null == parent ? cgScope : parent;
		
		//cg.setDataSymbol(cg.defineData(vScope.getResId(), -1, (String)le.getValue()));
		//TODO попыка вынести запись в аккумулятор на свое место
		if(toAccum) {
			//cg.constToAcc(cgScope, returnType.getSize(), isFixed() ? getFixedValue() : getNumValue(), isFixed());
			if(isString()) {
				cg.cellsToAcc(cgs, new CGCells(cg.defineData(cg.genId(), -1, value).getLabel()));
			}
			else {
				long v = isFixed() ? getFixedValue() : getNumValue();
				// LiteralExpression не должен задавать размер аккумулятору!
				cg.constToAcc(cgs, -1, v, isFixed());
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
