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
package ru.vm5277.common.cg.items;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.cg.scopes.CGClassScope;

public class CGIContainer extends CGItem {
	private	final	List<CGItem>	items	= new ArrayList<>();
	private			int				pos		= 0;
	private			String			tag;
	
	public CGIContainer() {
		tag = "";
	}
	
	public CGIContainer(String tag) {
		this.tag = tag;
	}
	
	public void begin() {
		pos = 0;
	}

	public void append(CGItem item) {
		items.add(item);
		pos = items.size()-1;
	}
	
	public void prepend(CGItem item) {
		items.add(0, item);
		pos = 1;
	}

	public void insert(CGItem item) {
		items.add(pos++, item);
	}
	
	public void remove(CGItem item)  {
		items.remove(item);
	}
	
	public List<CGItem> getItems() {
		return items;
	}
	
	@Override
	public String getSource() {
		StringBuilder sb = new StringBuilder();
		
		for(CGItem item : items) {
			if(!item.isDisabled()) {
				sb.append(item.getSource());
			}
		}

		return sb.toString();
	}
	
	public String getTag() {
		return tag;
	}
}
