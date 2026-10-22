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
;OUT: ACCUM_EH/EL/H/L результат, флаг C=1-переполнение
;-----------------------------------------------------------
OS_SHL32:
	PUSH FLAGS

	CPI TEMP_L,0x00
	BREQ _OS_SHL32__END
	CPI TEMP_L,0x20
	BRCC _OS_SHL32__ZERO

	LDI FLAGS,0x00
_OS_SHL32__LOOP:
	OR FLAGS,ACCUM_EH
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	DEC TEMP_L
	BRNE _OS_SHL32__LOOP
	LSL FLAGS
	RJMP _OS_SHL32__END
_OS_SHL32__ZERO:
	LDI ACCUM_EH,0x00
	LDI ACCUM_EL,0x00
	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x00
	SEC
_OS_SHL32__END:

	POP FLAGS
	RET
.ENDIF
