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

.include "dmem/dram.asm"

.IFNDEF J8BPROC_DEC_ARRREFCOUNT_CONST
;-----------------------------------------------------------
J8BPROC_DEC_ARRREFCOUNT_CONST:
;-----------------------------------------------------------
;Декремент количества ссылок массива
;IN: X-адрес заголовка массива, ACCUM_L/H-размер
;-----------------------------------------------------------
	PUSH TEMP_L
	PUSH ZL
	PUSH ZH

	MOVW ZL,XL
	LDD TEMP_L,Z+0x01
	CPI TEMP_L,0x00
    BREQ _J8BPROC_DEC_ARRREFCOUNT_CONST__END
	DEC TEMP_L
	STD Z+0x01,TEMP_L
	BRNE _J8BPROC_DEC_ARRREFCOUNT_CONST__END

	MCALL OS_DRAM_FREE

_J8BPROC_DEC_ARRREFCOUNT_CONST__END:
	POP ZH
	POP ZL
	POP TEMP_L
	RET
.ENDIF

