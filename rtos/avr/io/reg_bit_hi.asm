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
;Включаем бит по в ячейке расположенной по адресу 0-255
;-----------------------------------------------------------------------------------------------------------------------
.IFNDEF REG_BIT_HI_NR
;-----------------------------------------------------------
REG_BIT_HI_NR:
;-----------------------------------------------------------
;Включаем бит регистра
;IN: ACCUM_L-адрес,ACCUM_H-число бита
;MOD: XH,ACCUM_L/EL
;-----------------------------------------------------------
	MOV XL,ACCUM_L
	LDI XH,0x00
	LDS ACCUM_EL,SREG
	CLI
	LD ACCUM_L,X
	OR ACCUM_L,ACCUM_H
	ST X,ACCUM_L
	STS SREG,ACCUM_EL
	RET
.ENDIF
