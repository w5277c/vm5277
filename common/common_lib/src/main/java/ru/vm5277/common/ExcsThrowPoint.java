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

package ru.vm5277.common;

import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGItem;

public class ExcsThrowPoint {
	private	Integer			id;
	private	SourcePosition	sp;
	private	String			signature;
	private	boolean			inUse;

	private	CGIContainer	cont7b;
	private	CGIContainer	cont15b;
	
	public ExcsThrowPoint(int id, SourcePosition sp, String signature) {
		this.id = id;
		this.sp = sp;
		this.signature = signature;
	}

	public Integer getId() {
		return id;
	}

	public void apply(int id) {
		this.id = id;
	}

	public CGIContainer makeContainer(CGIContainer cont7b, CGIContainer cont15b) {
		this.cont7b = cont7b;
		this.cont15b = cont15b;
		
		CGIContainer result = new CGIContainer();
		result.append(cont7b);
		result.append(cont15b);
		return result;
	}
	
	public void chooseContainer(boolean is7Bit) {
		if(null!=cont15b) {
			if(is7Bit) {
				cont15b.disable();
			}
			else {
				cont7b.disable();
			}
		}
	}
	
	@Override
	public String toString() {
		return null!=id ? id + " " + sp.toString() + " " + signature : "";
	}

}
