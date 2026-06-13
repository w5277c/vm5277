/*
 * Copyright 2026 konstantin@5277.ru
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

.IFDEF OS_FT_DRAM
.IFNDEF OS_DRAM_SHOWMAP
.IF _DRAM_AVAILABLE_SIZE > 0
	.include "stdio/out_char.asm"
	.include "stdio/out_hex8.asm"
	.include "stdio/out_cstr.asm"
	.include "stdio/out_rambytes.asm"
	
_OS_DRAM_SHOWMAP__TEXT:
	.db "~DRAMSM:",0x0a,0x00

;-----------------------------------------------------------;
OS_DRAM_SHOWMAP:
;-----------------------------------------------------------;
;Выводит в STDOUT битовую карту динамической памяти
;-----------------------------------------------------------;
	PUSH_A16
	PUSH_T16
	PUSH_Z

	LDI ACCUM_H,high(_OS_DRAM_SHOWMAP__TEXT*2)
	LDI ACCUM_L,low(_OS_DRAM_SHOWMAP__TEXT*2)
	MCALL OS_OUT_CSTR
	LDI_Z OS_DRAM_BMASK_ADDR
	LDI TEMP_H,high(OS_DRAM_BMASK_SIZE)
	LDI TEMP_L,low(OS_DRAM_BMASK_SIZE)
	MCALL OS_OUT_RAMBYTES_NR
	LDI ACCUM_L,'\n'
	MCALL OS_OUT_CHAR

	POP_Z
	POP_T16
	POP_A16
	RET
.ELSE
OS_DRAM_SHOWMAP:
	RET
.ENDIF
.ENDIF
.ENDIF
