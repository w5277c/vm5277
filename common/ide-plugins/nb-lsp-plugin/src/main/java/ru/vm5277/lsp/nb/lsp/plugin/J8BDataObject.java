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
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages({
	"LBL_J8B_LOADER=Files of J8B"
})
@MIMEResolver.ExtensionRegistration(
		displayName = "#LBL_J8B_LOADER",
		mimeType = "text/x-vm5277-j8b",
		extension = {"j8b", "J8B"},
		position = 1000
)
@DataObject.Registration(
		mimeType = "text/x-vm5277-j8b",
		iconBase = "ru/vm5277/lsp/nb/lsp/plugin/list_16.png",
		displayName = "#LBL_J8B_LOADER",
		position = 300
)

public class J8BDataObject extends MultiDataObject {
	public J8BDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
		super(pf, loader);
		registerEditor("text/x-vm5277-j8b", true);
	}

	@Override
	protected int associateLookup() {
		return 1;
	}

	@MultiViewElement.Registration(
			displayName = "#LBL_J8B_EDITOR",
			iconBase = "ru/vm5277/lsp/nb/lsp/plugin/list_16.png",
			mimeType = "text/x-vm5277-j8b",
			persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
			preferredID = "J8B",
			position = 1000
	)
	
	@Messages("LBL_J8B_EDITOR=Source")
	public static MultiViewEditorElement createEditor(Lookup lkp) {
		return new MultiViewEditorElement(lkp);
	}
}
