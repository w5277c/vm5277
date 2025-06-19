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
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.common.exceptions.ParseException;

public class IRegExpression extends Expression {
	protected	int	id;
	protected	boolean	inc	= false;
	protected	boolean	dec	= false;

	public IRegExpression(int id, boolean isDec, boolean isInc) {
		this.id = id;
		this.dec = isDec;
		this.inc = isInc;
	}
	
	public IRegExpression(String ireg) throws ParseException {
		switch (ireg) {
			case "x": id=26; break;
			case "x+": id=26; inc=true; break;
			case "-x": id=26; dec=true; break;
			case "y": id=28; break;
			case "y+": id=28; inc=true; break;
			case "-y": id=28; dec=true; break;
			case "z": id=30; break;
			case "z+": id=30; inc=true; break;
			case "-z": id=30; dec=true; break;
			default:
				throw new ParseException("TODO ожидаем индексный регистр, получили " + ireg, sp);
		}
	}

	public int getId() {
		return id;
	}
	
	public boolean isDec() {
		return dec;
	}
	
	public boolean isInc() {
		return inc;
	}
	
	public String getMnemPart() {
		switch(id) {
			case 26:
				return (dec ? "m" : "") + "x" + (inc ? "p" : "");
			case 28:
				return (dec ? "m" : "") + "y" + (inc ? "p" : "");
			default:
				return (dec ? "m" : "") + "z" + (inc ? "p" : "");
		}
	}
	
	@Override
	public String toString() {
		return (dec ? "-" : "") + "r"+id + (inc ? "+" : "");
	}
}
