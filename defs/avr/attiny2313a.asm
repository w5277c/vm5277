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

.include "common.asm"

;---DESCRIPTION---------------------------------------------
;Header for ATtiny2313a (2KB ROM, 128B EEPROM, 128B SRAM, LE)

	.DEVICE ATtiny2313a
	.EQU DEVICE_SIGNATURE							= 0x001E910A
	
	.EQU FLASH_SIZE									= 0x0800; 2KB(1K words)
	.EQU FLASH_PAGESIZE								= 0x20	; 32B
	.EQU EEPROM_SIZE								= 0x0080; 128B
	.EQU EEPROM_PAGESIZE							= 0x04	; 4B
	.EQU SRAM_START									= 0x0060; 96B
	.EQU SRAM_SIZE									= 0x0080; 128B

	.EQU BOOT_256W_ADDR								= 0x300; Начальный адрес полукилобайтного бутлоадера В СЛОВАХ

	;---SBI/CBI/SBIS/SBIC-port-defines----------------------
	.EQU	PORTA									= 0x1b
	.EQU	DDRA									= 0x1a
	.EQU	PINA									= 0x19
	.EQU	PORTB									= 0x18
	.EQU	DDRB									= 0x17
	.EQU	PINB									= 0x16
	.EQU	PORTC									= 0xff
	.EQU	DDRC									= 0xff
	.EQU	PINC									= 0xff
	.EQU	PORTD									= 0x12
	.EQU	DDRD									= 0x11
	.EQU	PIND									= 0x10
	.EQU	PORTE									= 0xff
	.EQU	DDRE									= 0xff
	.EQU	PINE									= 0xff
	.EQU	PORTF									= 0xff
	.EQU	DDRF									= 0xff
	.EQU	PINF									= 0xff
	.EQU	PORTG									= 0xff
	.EQU	DDRG									= 0xff
	.EQU	PING									= 0xff

	;---IO-REGISTERS----------------------------------------
	.EQU	SREG									= 0x5f
	.EQU	SPL										= 0x5d
	.EQU	MCUCSR									= 0x54
	.EQU	MCUSR									= MCUCSR
	.EQU		WDRF								= 0x03
	.EQU		BORF								= 0x02
	.EQU		EXTRF								= 0x01
	.EQU		PORF								= 0x00
	.EQU	PRR										= 0x26
	.EQU		PRTIM1								= 0x03
	.EQU		PRTIM0								= 0x02
	.EQU		PRUSI								= 0x01
	.EQU		PRUSART0							= 0x00
	;---TIMER0---
	.EQU	TIMSK									= 0x59
	.EQU		TOIE1								= 0x07
	.EQU		OCIE1A								= 0x06
	.EQU		OCIE1B								= 0x05
	.EQU		ICIE1								= 0x03
	.EQU		OCIE0B								= 0x02
	.EQU		TOIE0								= 0x01
	.EQU		OCIE0A								= 0x00
	.EQU	TCCR0A									= 0x50
	.EQU		COM0A1								= 0x07
	.EQU		COM0A0								= 0x06
	.EQU		COM0B1								= 0x05
	.EQU		COM0B0								= 0x04
	.EQU		WGM01								= 0x01
	.EQU		WGM00								= 0x00
	.EQU	TCCR0B									= 0x53
	.EQU		FOC0A								= 0x07
	.EQU		FOC0B								= 0x06
	.EQU		WGM02								= 0x03
	.EQU		CS02								= 0x02
	.EQU		CS01								= 0x01
	.EQU		CS00								= 0x00
	.EQU	TCNT0									= 0x52
	.EQU	OCR0A									= 0x56
	.EQU	OCR0B									= 0x5c
	.EQU	TIFR									= 0x58
	.EQU		TOV1								= 0x07
	.EQU		OCF1A								= 0x06
	.EQU		OCF1B								= 0x05
	.EQU		ICF1								= 0x03
	.EQU		OCF0B								= 0x02
	.EQU		TOV0								= 0x01
	.EQU		OCF0A								= 0x00
	;---TIMER1---
	.EQU	TCCR1A									= 0x4f
	.EQU		COM1A1								= 0x07
	.EQU		COM1A0								= 0x06
	.EQU		COM1B1								= 0x05
	.EQU		COM1B0								= 0x04
	.EQU		WGM11								= 0x01
	.EQU		WGM10								= 0x00
	.EQU	TCCR1B									= 0x4e
	.EQU		ICNC1								= 0x07
	.EQU		ICES1								= 0x06
	.EQU		WGM13								= 0x04
	.EQU		WGM12								= 0x03
	.EQU		CS12								= 0x02
	.EQU		CS11								= 0x01
	.EQU		CS10								= 0x00
	.EQU	TCCR1C									= 0x42
	.EQU		FOC1A								= 0x07
	.EQU		FOC1B								= 0x06
	.EQU	TCNT1H									= 0x4d
	.EQU	TCNT1L									= 0x4c
	.EQU	OCR1AH									= 0x4b
	.EQU	OCR1AL									= 0x4a
	.EQU	OCR1BH									= 0x49
	.EQU	OCR1BL									= 0x48
	;---UART---
	.EQU	UDR										= 0x2c
	.EQU	UDR0									= UDR
	.EQU	UCSRA									= 0x2b
	.EQU	UCSR0A									= UCSRA
	.EQU		RXC0								= 0x07
	.EQU		TXC0								= 0x06
	.EQU		UDRE0								= 0x05
	.EQU		FE0									= 0x04
	.EQU		DOR0								= 0x03
	.EQU		UPE0								= 0x02
	.EQU		U2X0								= 0x01
	.EQU		MPCM0								= 0x00
	.EQU	UCSR0B									= 0x2a
	.EQU		RXCIE0								= 0x07
	.EQU		TXCIE0								= 0x06
	.EQU		UDRIE0								= 0x05
	.EQU		RXEN0								= 0x04
	.EQU		TXEN0								= 0x03
	.EQU		UCSZ02								= 0x02
	.EQU		RXB80								= 0x01
	.EQU		TXB80								= 0x00
	.EQU	UCSR0C									= 0x23
	.EQU		UMSEL01								= 0x07
	.EQU		UMSEL00								= 0x06
	.EQU		UPM01								= 0x05
	.EQU		UPM00								= 0x04
	.EQU		USBS0								= 0x03
	.EQU		UCSZ01								= 0x02
	.EQU		UCSZ00								= 0x01
	.EQU		UCPOL0								= 0x00
	.EQU	UBRR0H									= 0x22
	.EQU	UBRR0L									= 0x29
	;---PCINT---
	.EQU	PCMSK0									= 0x40
	.EQU		PCINT7								= 0x07
	.EQU		PCINT6								= 0x06
	.EQU		PCINT5								= 0x05
	.EQU		PCINT4								= 0x04
	.EQU		PCINT3								= 0x03
	.EQU		PCINT2								= 0x02
	.EQU		PCINT1								= 0x01
	.EQU		PCINT0								= 0x00
	.EQU	PCMSK1									= 0x24
	.EQU		PCINT10								= 0x02
	.EQU		PCINT9								= 0x01
	.EQU		PCINT8								= 0x00
	.EQU	PCMSK2									= 0x25
	.EQU		PCINT17								= 0x06
	.EQU		PCINT16								= 0x05
	.EQU		PCINT15								= 0x04
	.EQU		PCINT14								= 0x03
	.EQU		PCINT13								= 0x02
	.EQU		PCINT12								= 0x01
	.EQU		PCINT11								= 0x00
	;---INT---
	.EQU	MCUCR									= 0x55
	.EQU		PUD									= 0x07
	.EQU		SM1									= 0x06
	.EQU		SE									= 0x05
	.EQU		SM0									= 0x04
	.EQU		ISC11								= 0x03
	.EQU		ISC10								= 0x02
	.EQU		ISC01								= 0x01
	.EQU		ISC00								= 0x00
	.EQU	GIMSK									= 0x5b
	.EQU		INT1								= 0x07
	.EQU		INT0								= 0x06
	.EQU		PCIE0								= 0x05
	.EQU		PCIE2								= 0x04
	.EQU		PCIE1								= 0x03
	.EQU	GIFR									= 0x5a
	.EQU		INTF1								= 0x07
	.EQU		INTF0								= 0x06
	.EQU		PCIF0								= 0x05
	.EQU		PCIF2								= 0x04
	.EQU		PCIF1								= 0x03
	.EQU	EICRA									= MCUCR
	.EQU	EIMSK									= GIMSK
	.EQU	EIFR									= GIFR
	;---EEPROM---
	.EQU	EEARL									= 0x3e
	.EQU	EEDR									= 0x3d
	.EQU	EECR									= 0x3c
	.EQU		EEPM1								= 0x05
	.EQU		EEPM0								= 0x04
	.EQU		EERIE								= 0x03
	.EQU		EEMPE								= 0x02
	.EQU		EEPE								= 0x01
	.EQU		EERE								= 0x00
	;--SPM---
	.EQU	SPMCSR									= 0x57
	.EQU	SPMCR									= SPMCSR
	.EQU		RSIG								= 0x05
	.EQU		CTPB								= 0x04
	.EQU		RFLB								= 0x03
	.EQU		PGWRT								= 0x02
	.EQU		PGERS								= 0x01
	.EQU		SPMEN								= 0x00
	;---WATCHDOG---
	.EQU	WDTCR									= 0x41
	.EQU		WDIF								= 0x07
	.EQU		WDTIF								= WDIF
	.EQU		WDIE								= 0x06
	.EQU		WDTIE								= WDIE
	.EQU		WDP3								= 0x05
	.EQU		WDCE								= 0x04
	.EQU		WDE									= 0x03
	.EQU		WDP2								= 0x02
	.EQU		WDP1								= 0x01
	.EQU		WDP0								= 0x00

	;---OSCILLATOR-CALIBRATION-REGISTER---
	.EQU	OSCCAL									= 0x51
