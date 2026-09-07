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

.IFNDEF OS_US_TO_TICKS
;Является ли значение частоты не целым в мГц или не является степенью двойки
.IF (CORE_FREQ!=(CORE_FREQ/1000)*1000) || (0!=((CORE_FREQ/1000) & ((CORE_FREQ/1000)-1)))
.include "math/mul16.asm"
.include "math/div32.asm"
.ENDIF
;-----------------------------------------------------------
OS_US_TO_TICKS:
;-----------------------------------------------------------
;Конвертирую время (мкс) в тики CPU
;IN: ACCUM_H/L-16b-время в (мкс)
;OUT: ACCUM_EL/H/L-количестов тиков CPU
;MOD: ACCUM_EH (при делении всегда будет 0)
;-----------------------------------------------------------
.IF CORE_FREQ==1000
	LDI ACCUM_EL,0x00
	RET
.ELSE
.IF (CORE_FREQ==(CORE_FREQ/1000)*1000) && (0==((CORE_FREQ/1000) & ((CORE_FREQ/1000)-1)))
;Достаточно выполнить сдвиги
	LDI ACCUM_EH,LOG2(CORE_FREQ/1000)
	LDI ACCUM_EL,0x00
_OS_US_TO_TICKS__LOOP:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	DEC ACCUM_EH
	BRNE _OS_US_TO_TICKS__LOOP
	RET
.ELSE
	;Нужна математика (деление/умножение)
	LDI ACCUM_EH,high(CORE_FREQ)
	LDI ACCUM_EL,low(CORE_FREQ)
	MCALL OS_MUL16
	PUSH_T32
	LDI TEMP_EH,byte4(1000)
	LDI TEMP_EL,byte3(1000)
	LDI TEMP_H,byte2(1000)
	LDI TEMP_L,byte1(1000)
	;При максимальном значении период 65535us*20000kHz/1000=1310700 - достаточно 3 байт для результата
	MCALL OS_DIV32
	POP_T32
	RET
.ENDIF
.ENDIF
.ENDIF
