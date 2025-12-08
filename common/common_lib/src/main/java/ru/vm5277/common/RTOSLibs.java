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
	MCU_HALT("sys/mcu_halt.asm"),
	MCU_BLINK_FOREVER("sys/mcu_blick_forever.asm"),
	MCU_RESET("sys/mcu_reset.asm"),
	MCU_BLINK_N_RESET("sys/mcu_blink_n_reset.asm"),
	ETRACE_OUT("j8b/etrace_out.asm"),
	ETRACE_ADD("j8b/etrace_add.asm"),
	DRAM("dmem/dram.asm"),
	CLASS_REFCOUNT("j8b/class_refcount.asm"),
	INCTANCEOF("j8b/instanceof.asm"),
	CLEAR_FIELDS("j8b/clear_fields.asm"),
	DISPATCHER("core/dispatcher.asm"),
	INVOKE_METHOD("j8b/invoke_method.asm"),
	MATH_MUL8P2("math/mul8p2.asm"),
	MATH_MUL16P2("math/mul16p2.asm"),
	MATH_MUL32P2("math/mul32p2.asm"),
	MATH_MUL8("math/mul8.asm"),
	MATH_MUL16("math/mul16.asm"),
	MATH_MULQ7N8("math/mulq7n8.asm"),
	MATH_MUL32("math/mul32.asm"),
	MATH_DIV8P2("math/div8p2.asm"),
	MATH_DIV16P2("math/div16p2.asm"),
	MATH_DIV32P2("math/div32p2.asm"),
	MATH_DIV8("math/div8.asm"),
	MATH_DIV16("math/div16.asm"),
	MATH_DIVQ7N8("math/divq7n8.asm"),
	MATH_DIV32("math/div32.asm"),
	NEW_ARRAY("j8b/new_array.asm"),
	ARR_CELLADDR("j8b/arr_celladdr.asm"),
	ARR_SIZE("j8b/arr_size.asm"),
	ARR_REFCOUNT("j8b/arr_refcount.asm"),
	ARRVIEW_MAKE("j8b/arrview_make.asm"),
	ARRVIEW_ARRADDR("j8b/arrview_arraddr.asm"),
	ROM_READ16("mem/rom_read16.asm"),
	METHOD_FIN("j8b/mfin.asm"), //Без STACKFRAME IReg в стеке
	METHOD_FIN_SF("j8b/mfin_sf.asm"), //С STACKFRAME IReg в стеке
	MEM_RAMCLEAR("mem/ram_clear.asm");
	
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
