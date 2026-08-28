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

.IFNDEF OS_CRC8_NR
.include "math/reflect.asm"
;-----------------------------------------------------------
OS_CRC8_NR:
;-----------------------------------------------------------
;Вычисление crc8 из блока данных
;IN: X-адрес на данные, TEMP_EL-xorOut,TEMP_EH-poly
;ACCUM_EL-init,ACCUM_EH-bit0-refIn,bit1-refOut,
;TEMP_L/H-длина данных
;OUT: ACCUM_L-8b результат
;MOD: X,ACCUM_EL,TEMP_L/H
;-----------------------------------------------------------
_OS_CRC8__LOOP_BYTES:
	LD ACCUM_L,X+
	SBRC ACCUM_EH,0x00
	MCALL OS_REFLECT

	EOR ACCUM_EL,TEMP_EL

	LDI ACCUM_L,0x08
_OS_CRC8__LOOP_BITS:
	LSL ACCUM_EL
	BRCC PC+0x02
	EOR ACCUM_EL,TEMP_EH
	DEC ACCUM_L
	BRNE _OS_CRC8__LOOP_BITS
	SBIW TEMP_L,0x01
	BRNE _OS_CRC8__LOOP_BYTES

	MOV ACCUM_L,ACCUM_EL
	SBRC ACCUM_EH,0x01
	MCALL OS_REFLECT

	EOR ACCUM_L,TEMP_EL
OS_CRC8_NR__RET:
	RET
.ENDIF