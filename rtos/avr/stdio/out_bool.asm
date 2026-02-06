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
.include "stdio/out_cstr.asm"

.IFNDEF OS_OUT_BOOL

_OS_OUT_BOOL_STR_TRUE:
.db "true",0x00,0x00
_OS_OUT_BOOL_STR_FALSE:
.db "false",0x00

;--------------------------------------------------------
OS_OUT_BOOL:
;--------------------------------------------------------
;Вывод bool значения
;IN: ACCUM_L(младший бит)-значение(1-true)
;--------------------------------------------------------
.IF OS_FT_STDOUT == 0x01
	PUSH_Z

	SBRC ACCUM_L,0x00
	LDI ZL,low(_OS_OUT_BOOL_STR_TRUE*2)
	SBRC ACCUM_L,0x00
	LDI ZH,high(_OS_OUT_BOOL_STR_TRUE*2)
	SBRS ACCUM_L,0x00
	LDI ZL,low(_OS_OUT_BOOL_STR_FALSE*2)
	SBRS ACCUM_L,0x00
	LDI ZH,high(_OS_OUT_BOOL_STR_FALSE*2)
	MCALL OS_OUT_CSTR

	POP_Z
.ENDIF
	RET
.ENDIF
