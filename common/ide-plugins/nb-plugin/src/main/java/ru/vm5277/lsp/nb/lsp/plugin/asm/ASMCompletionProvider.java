package ru.vm5277.lsp.nb.lsp.plugin.asm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionProvider;
import ru.vm5277.lsp.nb.lsp.plugin.MyCompletionProvider;
import ru.vm5277.lsp.server.ASMLanguageServer;

@MimeRegistration(mimeType = "text/x-vm5277-asm", service = CompletionProvider.class)
public class ASMCompletionProvider extends MyCompletionProvider {
	@Override
	public List<String> getKeyWords() {
		Set<String> set = ASMLspLexerAdapter.getLSPServer().getLabels();
		set.addAll(ASMLanguageServer.getKeywords());
		set.addAll(ASMLspLexerAdapter.getLSPServer().getInstructions());
		List<String> result = new ArrayList<>(set);
		Collections.sort(result);
		return result;
	}
}