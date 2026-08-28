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

.IFNDEF J8BPROC_STDOUT_ARR
.include "j8b/arr_size.asm"
.include "stdio/out_bytes.asm"
;-----------------------------------------------------------
J8BPROC_STDOUT_ARR:
;-----------------------------------------------------------
;Выводим весь одномерный byte массив
;IN: X-адрес заголовка массива
;MOD: X,ACCUM_L/H
;-----------------------------------------------------------
	MCALL J8BPROC_ARR_SIZE
	ADIW XL,OS_J8B_1D_BYTE_ARRAY_HEADER_SIZE
	MJMP OS_OUT_BYTES
.ENDIF
