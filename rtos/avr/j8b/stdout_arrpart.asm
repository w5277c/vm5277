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

.IFNDEF J8BPROC_STDOUT_ARRPART
.include "j8b/_arr_offset.asm"
.include "stdio/out_bytes.asm"
;-----------------------------------------------------------
J8BPROC_STDOUT_ARRPART:
;-----------------------------------------------------------
;Выводим весь одномерный byte массив
;IN: X-адрес заголовка массива,ACCUM_L/H-смещение
;ACCUM_EL/EH-длина
;OUT: BoundsException
;MOD: X,ACCUM_L/H
;-----------------------------------------------------------
	MCALL _J8BPROC_ARR_OFFSET
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRTS J8BPROC_STDOUT_ARRPART__END
.ENDIF
	MOVW ACCUM_L,ACCUM_EL
	MJMP OS_OUT_BYTES

J8BPROC_STDOUT_ARRPART__END:
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	RET
.ENDIF
.ENDIF
