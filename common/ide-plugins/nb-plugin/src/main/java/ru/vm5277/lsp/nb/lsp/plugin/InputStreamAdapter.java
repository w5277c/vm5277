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

package ru.vm5277.lsp.nb.lsp.plugin;

import java.io.IOException;
import java.io.InputStream;
import org.netbeans.spi.lexer.LexerInput;

public class InputStreamAdapter extends InputStream {
    private final LexerInput lexerInput;
    
    public InputStreamAdapter(LexerInput lexerInput) {
        this.lexerInput = lexerInput;
    }
    
    @Override
	public int read() throws IOException {
		int result = lexerInput.read();
		if (result == LexerInput.EOF) {
			return -1;
		}
		return result;
	}    
	
    @Override
    public int available() {
        return lexerInput.readLength();
    }
    
    @Override
    public long skip(long n) {
        int toSkip = (int) Math.min(n, lexerInput.readLength());
        for (int i = 0; i < toSkip; i++) {
            lexerInput.read();
        }
        return toSkip;
    }
}