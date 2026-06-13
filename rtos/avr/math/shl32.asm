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

.IFNDEF OS_SHL32
;-----------------------------------------------------------
;Сдвиг влево 32бит числа на 8бит величину сдвига
;IN: ACCUM_EH/EL/H/L-сдвигаемое число, TEMP_L-величина
;сдвига
;OUT: ACCUM_EH/EL/H/L результат
;-----------------------------------------------------------
OS_SHL32:
	CPI TEMP_L,0x20
	BRCC _OS_SHL32__ZERO
_OS_SHL32__LOOP:
	SUBI TEMP_L,0x01
	BRCS _OS_SHL32__END
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	RJMP _OS_SHL32__LOOP
_OS_SHL32__ZERO:
	LDI ACCUM_EH,0x00
	LDI ACCUM_EL,0x00
	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x00
_OS_SHL32__END:
	RET
.ENDIF
