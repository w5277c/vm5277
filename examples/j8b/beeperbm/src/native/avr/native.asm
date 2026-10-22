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

.include "core/irq_set.asm"
;-----------------------------------------------------------
NATIVE_PLAY:
;-----------------------------------------------------------
;Инициализация и запуск таймера
;IN:ACCUM_L/H-период в микросекундах
;-----------------------------------------------------------
	STS TCCR1A,C0x00
	LDI ACCUM_EL,(1<<WGM12)|(1<<CS11)
	STS TCCR1B,ACCUM_EL
    STS TCNT1H,C0x00
    STS TCNT1L,C0x00
	LSL ACCUM_L
	ROL ACCUM_H
	STS OCR1AH,ACCUM_H
	STS OCR1AL,ACCUM_L

	LDI ACCUM_L,OS_IRQ_TIMER1_CMPA
	LDI ACCUM_EH,high(NATIVE_CALLBACK)
	LDI ACCUM_EL,low(NATIVE_CALLBACK)
	LDI_Y 0xffff
	LDI ACCUM_H,0x00
	MCALL OS_IRQ_SET

	SBI DDRD,0x07

	LDI ACCUM_EL,(1<<OCIE1A)
	STS TIMSK1,ACCUM_EL
    RET

;-----------------------------------------------------------
NATIVE_CALLBACK:
;-----------------------------------------------------------
;Обработка прерывания от таймера
;-----------------------------------------------------------
	SBI PIND,0x07
	RET

;-----------------------------------------------------------
NATIVE_STOP:
;-----------------------------------------------------------
;Останов таймера
;-----------------------------------------------------------
	STS TCCR1B,C0x00
    STS TCCR1A,C0x00
    CBI DDRD,0x07
	RET

NATIVE_FLASH_READ8:
;-----------------------------------------------------------
;Читаем из flash байт
;IN: Z-адрес на Flash данные (2 первых байта-длина),
;ACCUM_L-смещение во Flash
;OUT: ACCUM_L
;-----------------------------------------------------------
	ADIW ZL,0x02
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z
	RET

NATIVE_FLASH_READ16:
;-----------------------------------------------------------
;Читаем из flash слово
;IN: Z-адрес на Flash данные (2 первых байта-длина),
;ACCUM_L-смещение во Flash
;OUT: ACCUM_L/H
;-----------------------------------------------------------
	ADIW ZL,0x02
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z+
	LPM ACCUM_H,Z
	RET
