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

public enum RTOSLibs {
	STACK_ALLOC("sys/stack_alloc.asm"),
	STACK_FREE("sys/stack_free.asm"),
	DRAM("dmem/dram.asm"),
	INC_REFCOUNT("j8b/inc_refcount.asm"),
	DEC_REFCOUNT("j8b/dec_refcount.asm"),
	INCTANCEOF("j8b/instanceof.asm"),
	CLEAR_FIELDS("j8b/clear_fields.asm"),
	DISPATCHER("core/dispatcher.asm"),
	INVOKE_METHOD("j8b/invoke_method.asm"),
	MATH_MUL8("math/mul8.asm"),
	MATH_MUL16("math/mul16.asm"),
	MATH_MULQ7N8("math/mulq7n8.asm"),
	MATH_MUL32("math/mul32.asm"),
	MATH_DIV8("math/div8.asm"),
	MATH_DIV16("math/div16.asm"),
	MATH_DIVQ7N8("math/divq7n8.asm"),
	MATH_DIV32("math/div32.asm");
	
	private	final	String	path;
	private			boolean	required;
	
    RTOSLibs(String path) {
        this.path = path;
    }
	
	public void setRequired() {
		required = true;
	}
	public boolean isRequired() {
		return required;
	}
	
	public String getPath() {
		return path;
	}
}
