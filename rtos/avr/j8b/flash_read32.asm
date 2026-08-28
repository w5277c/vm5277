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

.IFNDEF J8BPROC_FLASH_READ32
.include "j8b/_flash_offset.asm"
;-----------------------------------------------------------
J8BPROC_FLASH_READ32:
;-----------------------------------------------------------
;Читаем из flash байт
;IN: Z-адрес на Flash данные (2 первых байта-длина),
;ACCUM_EL,EH-смещение во Flash
;OUT: ACCUM_L/H/EL/EH или BoundsException
;MOD: Z,ACCUM_H/L
;-----------------------------------------------------------
	MCALL _J8BPROC_FLASH_OFFSET
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRTS J8BPROC_FLASH_READ32__END
.ENDIF
	LPM ACCUM_L,Z+
	LPM ACCUM_H,Z+
	LPM ACCUM_EL,Z+
	LPM ACCUM_EH,Z
J8BPROC_FLASH_READ32__END:
	RET
.ENDIF
