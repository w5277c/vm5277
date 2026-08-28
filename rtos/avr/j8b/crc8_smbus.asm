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

.IFNDEF J8BPROC_CRC8_SMBUS
.include "j8b/_arr_offset.asm"
.include "math/crc8_smbus.asm"

;-----------------------------------------------------------
J8BPROC_CRC8_SMBUS:
;-----------------------------------------------------------
;Вычисление crc8 SMBUS из одномерного byte массива
;IN: X-адрес заголовка массива, ACCUM_L/H-смещение
;ACCUM_EL/EH-длина
;OUT: ACCUM_L-8b результат или BoundsException
;MOD: X,ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	CP ACCUM_EL,C0x00
	CPC ACCUM_EH,C0x00
	BREQ J8BPROC_CRC8_SMBUS__END

	MCALL _J8BPROC_ARR_OFFSET								;Проверяем границы и вычисляем адрес ячейки для массива
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRTS J8BPROC_CRC8_SMBUS__END
.ENDIF
	PUSH_T16
	MOVW TEMP_L,ACCUM_EL
	MCALL OS_CRC8_SMBUS_NR
	POP_T16
J8BPROC_CRC8_SMBUS__END:
	RET
.ENDIF