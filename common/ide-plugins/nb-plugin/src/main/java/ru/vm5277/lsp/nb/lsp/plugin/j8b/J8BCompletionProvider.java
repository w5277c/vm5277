package ru.vm5277.lsp.nb.lsp.plugin.j8b;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionProvider;
import ru.vm5277.lsp.nb.lsp.plugin.MyCompletionProvider;
import ru.vm5277.lsp.server.J8BLanguageServer;

@MimeRegistration(mimeType = "text/x-vm5277-j8b", service = CompletionProvider.class)
public class J8BCompletionProvider extends MyCompletionProvider {
	@Override
	public List<String> getKeyWords() {
		Set<String> set = J8BLspLexerAdapter.getLSPServer().getLabels();
		set.addAll(J8BLanguageServer.getKeywords());
		List<String> result = new ArrayList<>(set);
		Collections.sort(result);
		return result;
	}
}