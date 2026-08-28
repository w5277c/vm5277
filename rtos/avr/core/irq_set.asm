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

.IFNDEF OS_IRQ_SET
;-----------------------------------------------------------
OS_IRQ_SET:
;-----------------------------------------------------------
;Устанавливает адрес обработчика
;IN:ACCUM_L-номер прерывания в блоке, ACCUM_EL/EH-адрес
;обработчика, 0x0000-нет обработчика, Y-адрес HEAP или
;выделенной памяти, ACCUM_H-RES_ID
;Z-адрес на IRQ RAM table блок
;MOD:Z
;-----------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG

	LDI_Z _OS_IRQ_RAM_TABLE
	ADD ZL,ACCUM_L
	ADC ZH,C0X00
	CLI
	ST Z+,ACCUM_EL											;_OS_IRQ_SET__OFFSET_HANDLER_ADDR
	ST Z+,ACCUM_EH
	ST Z+,YL												;_OS_IRQ_SET__OFFSET_HEAP_ADDR
	ST Z+,YH
	ST Z,ACCUM_H											;RES_ID

	STS SREG,TEMP_L
	POP TEMP_L
	RET
.ENDIF
