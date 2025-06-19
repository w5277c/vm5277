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
package ru.vm5277.avr_asm.tokens;

import ru.vm5277.common.SourceBuffer;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.Operator;

public class TOpearator extends Token {
	private TOpearator(SourceBuffer sb, Operator op) {
		super(sb);
		this.type = TokenType.OPERATOR;
		this.value = op;
	}

	public static TOpearator parse(SourceBuffer sb) {
		
		// Проверяем трехсимвольные операторы (если есть)
        if (sb.hasNext(2)) {
            Operator op = Operator.fromSymbol(sb.getSource().substring(sb.getPos(), sb.getPos()+3));
            if (null != op) {
				TOpearator result = new TOpearator(sb, op);
				sb.next(3);
				return result;
			}
        }
        
        // Проверяем двухсимвольные операторы
        if (sb.hasNext(1)) {
            Operator op = Operator.fromSymbol(sb.getSource().substring(sb.getPos(), sb.getPos()+2));
            if (null != op) {
				TOpearator result = new TOpearator(sb, op);
				sb.next(2);
				return result;
			}
        }
        
		// Проверяем односимвольные операторы
        if (sb.hasNext()) {
            Operator op = Operator.fromSymbol(sb.getSource().substring(sb.getPos(), sb.getPos()+1));
            if (null != op) {
				TOpearator result = new TOpearator(sb, op);
				sb.next();
				return result;
			}
        }
		return null;
	}
}
