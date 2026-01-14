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
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.Assembler;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class Const {
	protected	int		bits;
	protected	long	value;

	protected Const() {
	}
	
	public Const(MessageContainer mc, Scope scope, SourcePosition sp, Expression expr, int min, int max, int bits) throws CompileException {
		this.bits = bits;
		Long _value = Expression.getLong(expr, sp);
		if(null == _value) {
			throw new CompileException("Cannot resolve constant '" + expr + "'", sp);
		}

		long mask = (1<<bits)-1;
		if(Assembler.STRICT_STRONG != Scope.getStrincLevel() && 0==min && _value>max) {
			long new_value = _value & mask;
			if(new_value<=max) {
				if(Assembler.STRICT_NONE != Scope.getStrincLevel()) {
					mc.add(new WarningMessage("Constant value " + _value + " exceeds " + bits + "-bit range. Truncated to: " + new_value, sp));
				}
				_value = new_value;
			}
		}
		
		if(min>_value || max<_value) {
			mc.add(new ErrorMessage("Constant value out of range (" + _value + "), expected " + min + "≤ value <" + max, sp));
			_value &= mask;
		}
		value = _value;
	}	

	public int getBits() {
		return bits;
	}
	
	public long getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return Long.toString(value);
	}
}
