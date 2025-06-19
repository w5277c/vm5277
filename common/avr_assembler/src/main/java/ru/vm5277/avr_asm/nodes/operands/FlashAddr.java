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

import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class FlashAddr extends Const {
	public FlashAddr(MessageContainer mc, Scope scope, SourcePosition sp, Expression expr, int min, int max, int bits, int addr) throws ParseException {
		this.bits = bits;
		
		Long _value = Expression.getLong(expr, sp);
		if(null == _value) {
			throw new ParseException("Unable to resolve address: " + expr, sp);
		}
		value = _value - addr;
		
		if(min>value || max<value) {
			mc.add(new WarningMessage("Value " + value + " out of valid range [" + min + ".." + max + "]", sp));
			value = 0;
//throw new ParseException("Value " + value + " out of valid range [" + min + ".." + max + "]", sp);
		}
	}	
}
