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

package ru.vm5277.lsp.server;

import ru.vm5277.lsp.server.tokens_info.LSPParameterInfo;
import ru.vm5277.lsp.server.tokens_info.LSPFieldInfo;
import ru.vm5277.lsp.server.tokens_info.LSPClassInfo;
import ru.vm5277.lsp.server.tokens_info.LSPMethodInfo;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompilationAbortedException;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.ASTParser;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.EnumNode;
import ru.vm5277.compiler.nodes.ExceptionNode;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.InterfaceNode;
import ru.vm5277.compiler.nodes.MethodNode;
import ru.vm5277.compiler.nodes.ObjectTypeNode;
import ru.vm5277.compiler.nodes.ParameterNode;
import ru.vm5277.lsp.server.tokens_info.LSPExceptionInfo;
import ru.vm5277.lsp.server.tokens_info.LSPNodeInfo;

public class J8BLanguageServer extends LanguageServer {
	private	final			Lexer							lexer				= new Lexer(LexerType.J8B);
	private			static	Map<String,LSPNodeInfo>			keywords			= null;
	private final	static	List<String>					runtimeObjects		= new ArrayList<>();
	private			static	Map<String, LSPNodeInfo>		autoImportedObjects	= null;
	private			static	Map<String, List<LSPNodeInfo>>	importsCache		= new HashMap<>();
	private			static	Map<String, Long>				timestampsCache		= new HashMap<>();
	private			static	Path							toolkitPath;
	private			static	Path							runtimePath;
	
	static {
		if(null==keywords) {
			keywords = new HashMap<>();

			for(Keyword kw : Keyword.getItems()) {
				if(LexerType.ALL==kw.getLexerType() || LexerType.J8B==kw.getLexerType()) {
					keywords.put(kw.getName().toLowerCase(), new LSPNodeInfo(kw.getName(), kw.getTokenType(), "keyword", 100));
				}
			}
		}
	}

	public J8BLanguageServer() {
		if(null==autoImportedObjects) {
			toolkitPath = FSUtils.getToolkitPath();
			runtimePath = toolkitPath.resolve("runtime");
			
			loadRuntimeClasses();
			
			autoImportedObjects = new HashMap<>();			
			loadRuntimeAutoImports();
		}
	}

	private void loadRuntimeClasses() {
		try {
			List<Path> files = new ArrayList<>();
			collectJ8bFiles(runtimePath.toFile(), files);

			for (Path path : files) {
				String name = runtimePath.relativize(path).toString().replace("\\", ".").replace("/", ".");
				runtimeObjects.add(name.substring(0, name.length()-".j8b".length()));
			}
			Collections.sort(runtimeObjects);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void collectJ8bFiles(File dir, List<Path> result) {
		File[] files = dir.listFiles();
		if(null==files) return;

		for(File file : files) {
			if(file.isDirectory()) {
				collectJ8bFiles(file, result);
			}
			else if(file.getName().endsWith(".j8b")) {
				result.add(file.toPath());
			}
		}
	}
	
	private void loadRuntimeAutoImports() {
		try {
			if(null==toolkitPath) return;
			if(!Files.exists(runtimePath)) return;

			Instance inst = new Instance(toolkitPath, runtimePath, new MessageContainer(1, true, true), 4);
			ASTParser parser = new ASTParser(inst, runtimePath);
			try {
				parser.parse(null, true, false);
			}
			catch(CompilationAbortedException ignore) {}
			
			for (ObjectTypeNode node : parser.getAutoImported()) {
				LSPNodeInfo info = convertToLSPTokenInfo(node);
				autoImportedObjects.put(info.getName().toLowerCase(), info);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public void updateImportsFromFile(String filePath) {
		try {
			if(null==toolkitPath) return;
			if(!Files.exists(runtimePath)) return;

			File file = new File(filePath);
			Long timestamp = timestampsCache.get(filePath);
			if(null==timestamp || timestamp!=file.lastModified()) {
				timestampsCache.put(filePath, file.lastModified());
				
				Lexer lexer = new Lexer(LexerType.J8B, file, null, 4, false);
				Path path = Path.of(filePath);
				Instance inst = new Instance(toolkitPath, runtimePath, new MessageContainer(Integer.MAX_VALUE, false, true), 4);
				ASTParser parser = new ASTParser(inst, path.getParent());
				try {
					parser.parse(lexer.getTokens(), false, false);
				}
				catch(CompilationAbortedException ignore) {}
				
				List<LSPNodeInfo> classInfos = new ArrayList<>();
				for(ObjectTypeNode otn : parser.getImported()) {
					classInfos.add(convertToLSPTokenInfo(otn));
				}
				importsCache.put(filePath, classInfos);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}	

	private LSPNodeInfo convertToLSPTokenInfo(ObjectTypeNode otn) {
		if(otn instanceof ExceptionNode) {
			ExceptionNode en = (ExceptionNode)otn;
			
			LSPExceptionInfo exceptionInfo = new LSPExceptionInfo(otn.getFullName());
			exceptionInfo.setFileUri(otn.getSP().getSourceFile().getPath());
			List<LSPMethodInfo> methods = new ArrayList<>();
			// Вручную заданные методв - временное решение, до тех пор пока не введем полноценный парсинг и семантику
			// throw(),throw(byte),typeId(),code();
			LSPMethodInfo mInfo = new LSPMethodInfo("throw");
			mInfo.setStatic(true);
			mInfo.setPublic(true);
			mInfo.setPriority(1);
			methods.add(mInfo);
			mInfo = new LSPMethodInfo("throw");
			mInfo.getParameters().add(new LSPParameterInfo(VarType.BYTE.getName(), "code"));
			mInfo.setStatic(true);
			mInfo.setPublic(true);
			mInfo.setPriority(1);
			methods.add(mInfo);
			mInfo = new LSPMethodInfo("typeId");
			mInfo.setStatic(true);
			mInfo.setPublic(true);
			mInfo.setVarType(VarType.BYTE.getName());
			mInfo.setPriority(1);
			methods.add(mInfo);
			exceptionInfo.setMethods(methods);
			exceptionInfo.setItems(((ExceptionNode)otn).getValues());
			return exceptionInfo;
		}
		else {
			LSPClassInfo classInfo = new LSPClassInfo(otn.getFullName());
			classInfo.setKind(otn instanceof ClassNode ? "class" : "interface");
			classInfo.setFileUri(otn.getSP().getSourceFile().getPath());

			// Конвертируем методы, если есть
			List<LSPFieldInfo> fields = new ArrayList<>();
			List<LSPMethodInfo> methods = new ArrayList<>();
			List<LSPNodeInfo> internals = new ArrayList<>();
			
			if(null!=otn.getBody()) {
				for(AstNode node : otn.getBody().getChildren()) {
					if(node instanceof MethodNode) {
						MethodNode mNode = (MethodNode)node;
						if(mNode.isStatic() && mNode.isPublic()) {
							LSPMethodInfo methodInfo = new LSPMethodInfo(mNode.getName());
							for(ParameterNode pNode : mNode.getParameters()) {
								methodInfo.getParameters().add(new LSPParameterInfo(pNode.getType().getName(), pNode.getName()));
							}
							methodInfo.setVarType(null==mNode.getReturnType() ? VarType.VOID.getName() : mNode.getReturnType().getName());
//							methodInfo.setStatic(mNode.isStatic());
//							methodInfo.setPublic(mNode.isPublic());
							methods.add(methodInfo);
						}
					}
					else if(node instanceof FieldNode) {
						FieldNode fNode = (FieldNode)node;
						if(fNode.isStatic() && fNode.isPublic()) {
							LSPFieldInfo fieldnIfo = new LSPFieldInfo(fNode.getName(), fNode.getType().getName());
							fieldnIfo.setStatic(fNode.isStatic());
							fields.add(fieldnIfo);
						}
					}
					else if(node instanceof EnumNode) {
						EnumNode interrnalNode = (EnumNode)node;
						if(interrnalNode.isPublic()) {
							LSPClassInfo internalInfo = new LSPClassInfo(interrnalNode.getName());
							internalInfo.setKind("enum");
							internalInfo.setFileUri(otn.getSP().getSourceFile().getPath());
							internals.add(internalInfo);
						}
					}
					else if(node instanceof ClassNode) {
						ClassNode interrnalNode = (ClassNode)node;
						if(interrnalNode.isStatic() && interrnalNode.isPublic()) {
							LSPClassInfo internalInfo = new LSPClassInfo(interrnalNode.getFullName());
							internalInfo.setKind("class");
							internalInfo.setFileUri(otn.getSP().getSourceFile().getPath());
							internals.add(internalInfo);
						}
					}
					else if(node instanceof InterfaceNode) {
						InterfaceNode interrnalNode = (InterfaceNode)node;
						if(interrnalNode.isStatic() && interrnalNode.isPublic()) { //TODO protected?
							LSPClassInfo internalInfo = new LSPClassInfo(interrnalNode.getFullName());
							internalInfo.setKind("interface");
							internalInfo.setFileUri(otn.getSP().getSourceFile().getPath());
							internals.add(internalInfo);
						}
					}
					else if(node instanceof ExceptionNode) {
						ExceptionNode interrnalNode = (ExceptionNode)node;
						if(interrnalNode.isPublic()) {
							LSPExceptionInfo exceptionInfo = new LSPExceptionInfo(interrnalNode.getFullName());
							exceptionInfo.setKind("exception");
							exceptionInfo.setFileUri(otn.getSP().getSourceFile().getPath());
							internals.add(exceptionInfo);
						}
					}
				}
			}
			classInfo.setFields(fields);
			classInfo.setMethods(methods);
			classInfo.setInternals(internals);
			return classInfo;
		}
	}
	
	public List<String> getImportSuggestions(String prefix) {
		List<String> result = new ArrayList<>();
		for(String name : runtimeObjects) {
			if(name.startsWith(prefix)) {
				result.add(name);
			}
		}
		return result;
	}
	
	public List<LSPNodeInfo> getCompletionSuggestions(String prefix, String filePath) {
		List<LSPNodeInfo> result = new ArrayList<>();
		
		final String prefixLc = (null==prefix ? null : prefix.toLowerCase());
		
		for(Map.Entry<String, LSPNodeInfo> entry : keywords.entrySet()) {
			if(null==prefixLc || prefixLc.isEmpty() || entry.getKey().startsWith(prefixLc)) {
				result.add(entry.getValue());
			}
		}
		
		if(null==prefix || prefix.isEmpty()) {
			result.addAll(autoImportedObjects.values());
			if(null!=filePath) {
				List<LSPNodeInfo> imported = importsCache.get(filePath);
				if(null!=imported) {
					for(LSPNodeInfo info : imported) {
						result.add(info);
					}
				}
			}
		}
		else {
			for(LSPNodeInfo info : autoImportedObjects.values()) {
				if(info.getName().toLowerCase().startsWith(prefixLc)) {
					result.add(info);
				}
			}

			if(null!=filePath) {
				List<LSPNodeInfo> imported = importsCache.get(filePath);
				if(null!=imported) {
					for (LSPNodeInfo info : imported) {
						if(!containsInfo(result, info.getName()) && info.getName().toLowerCase().startsWith(prefixLc)) {
							result.add(info);
						}
					}
				}
			}
		}

		Collections.sort(result, new Comparator<LSPNodeInfo>() {
			@Override
			public int compare(LSPNodeInfo o1, LSPNodeInfo o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return result;
	}
	
	private boolean containsInfo(List<LSPNodeInfo> list, String name) {
		for(LSPNodeInfo info : list) {
			if(info.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public LSPNodeInfo findNode(String name, String filePath) {
		if(null==name) return null;
		String nameLc = name.toLowerCase();
		
		// Сначала ищем по короткому имени
		LSPNodeInfo info = autoImportedObjects.get(nameLc);
		if(null!=info) return info;

		// Потом по полному имени
		for(LSPNodeInfo candidate : autoImportedObjects.values()) {
			if(candidate.getFullName().toLowerCase().equals(nameLc)) {
				return candidate;
			}
		}
		
		if(null!=filePath) {
			List<LSPNodeInfo> imported = importsCache.get(filePath);
			if(null!=imported) {
				for(LSPNodeInfo imp : imported) {
					if(imp.getName().toLowerCase().startsWith(nameLc)) {
						return imp;
					}
				}
			}
		}

		return null;
	}

	@Override
	public LSPToken readNextToken(SourceBuffer sb) {
		Token token = lexer.parseToken(sb);
		if(null==token || TokenType.EOF==token.getType()) {
			return null;
		}
		
		if(TokenType.LABEL==token.getType()) {
			labels.add(token.getRaw());
		}

		return new LSPToken(token);
	}
	
	@Override
	public List<Token> tokenize(SourceBuffer sb) {
		List<Token> result = new ArrayList<>();
		while(sb.available()) {
			result.add(lexer.parseToken(sb));
		}
		return result;
	}

	public static List<String> getKeywords() {
		List<String> result = new ArrayList<>();
		for(Keyword keyword : Keyword.getItems()) {
			if(LexerType.ALL==keyword.getLexerType() || LexerType.J8B==keyword.getLexerType()) {
				result.add(keyword.getName());
			}
		}
		return result;
	}
	
	public void invalidateCache(String filePath) {
		importsCache.remove(filePath);
		timestampsCache.remove(filePath);
	}
}

