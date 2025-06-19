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
package ru.vm5277.avr_asm;

public enum Delimiter {
    LEFT_PAREN('('),
    RIGHT_PAREN(')'),
    COMMA(',');
    
	private	final	char	symbol;

    Delimiter(char symbol) {
        this.symbol = symbol;
    }

	@Override
	public String toString() {
		return String.valueOf(symbol);
	}
	
    public static Delimiter fromSymbol(char symbol) {
        // Сначала проверяем многозначные разделители
        for (Delimiter delim : values()) {
            if (delim.symbol==symbol) {
				return delim;
            }
        }
        return null;
    }
}