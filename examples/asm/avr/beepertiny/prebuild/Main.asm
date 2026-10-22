; vm5277.avr:atmega328p, opt:size v0.5.0
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

.equ CORE_FREQ = 16000 ;KHz

.set OS_FT_IRQ_TIMER_BLOCK = 1
.set OS_FT_BLDR_API_REUSE = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "sys/mcu_halt.asm"
.include "math/div16.asm"
.include "math/shr16.asm"
.include "core/wait_ms.asm"
.include "core/irq_set.asm"

PERIODS:
.db 0x75,0x77,0xc4,0x70,0x6f,0x6a,0x6a,0x64,0xcf,0x5e,0x78,0x59,0x70,0x54,0xb8,0x4f,0x3c,0x4b,0x05,0x47,0x06,0x43,0x44,0x3f
MELODY:
.db 0x90,0x40,0xd0,0x40,0xd0,0x40,0xd0,0x43,0xd0,0x41,0xd0,0x40,0xd0,0x3b,0xd0,0x40

MAIN:
	LDI_X 500
	LDI_Z MELODY*2
	LDI FLAGS,0x10
_MAIN_LOOP:
	LPM ACCUM_EL,Z+
	PUSH_Z
	SBRS ACCUM_EL,0x07
	RJMP _MAIN_LOOP__PLAY_NOTE
	;Выдерживаем паузу
	PUSH ACCUM_EL
	ANDI ACCUM_EL,0x3f
	LDI ACCUM_EH,0x00
	LDI ACCUM_L,low(1000)
	LDI ACCUM_H,high(1000)
	MCALL OS_DIV16
	POP ACCUM_EL
	;Выдерживает длительность или паузу
	SBRS ACCUM_EL,0x06
	RJMP _MAIN__SKIP_TIMER_STOP
	;Останавливаем таймер
	STS TCCR1B,C0x00
    STS TCCR1A,C0x00
_MAIN__SKIP_TIMER_STOP:
	MOVW XL,ACCUM_L
	MCALL OS_WAIT_MS
	RJMP _MAIN__NEXT
_MAIN_LOOP__PLAY_NOTE:
	;Инициируем прогирывание ноты
	MOV ACCUM_L,ACCUM_EL
	ANDI ACCUM_L,0x0f
	LSL ACCUM_L
	LDI_Z PERIODS*2
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z+
	LPM ACCUM_H,Z
	SWAP ACCUM_EL
	ANDI ACCUM_EL,0x07
	MCALL OS_SHR16
	;Инициализируем таймер
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
	LDI ACCUM_EH,high(CALLBACK)
	LDI ACCUM_EL,low(CALLBACK)
	LDI_Y 0xffff
	LDI ACCUM_H,0x00
	MCALL OS_IRQ_SET
	SBI DDRD,0x07
	LDI ACCUM_EL,(1<<OCIE1A)
	STS TIMSK1,ACCUM_EL

	MOVW ACCUM_L,XL
	MCALL OS_WAIT_MS
_MAIN__NEXT:
	POP_Z
	DEC FLAGS
	BREQ PC+0x02
	RJMP _MAIN_LOOP

    CBI DDRD,0x07
	JMP MCU_HALT

CALLBACK:
	SBI PIND,0x07
   	RET
