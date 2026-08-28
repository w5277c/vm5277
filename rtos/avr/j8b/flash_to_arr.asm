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

.IFNDEF J8BPROC_FLASH_TO_ARR
.include "mem/rom_read16.asm"
.include "j8b/_flash_offset.asm"
.include "j8b/arr_size.asm"

;-----------------------------------------------------------
J8BPROC_FLASH_TO_ARR:
;-----------------------------------------------------------
;Копируем данные из flash в массив
;IN: Z-адрес на Flash данные (2 первых байта-длина),
;ACCUM_EL,EH-смещение во Flash, X-массив
;OUT: BoundsException
;MOD: Z,X,ACCUM_H/L
;-----------------------------------------------------------
	MCALL _J8BPROC_FLASH_OFFSET
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRTS J8BPROC_FLASH_TO_ARR__END
.ENDIF

	MCALL J8BPROC_ARR_SIZE
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ J8BPROC_FLASH_TO_ARR__END

	ADIW XL,OS_J8B_1D_BYTE_ARRAY_HEADER_SIZE
	MJMP OS_ROM_READ16_NR

J8BPROC_FLASH_TO_ARR__END:
	RET
.ENDIF
