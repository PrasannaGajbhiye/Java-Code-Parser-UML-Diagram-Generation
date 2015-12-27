package example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.TypeDeclarationStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;



public class Umlparser {
	static ConcurrentHashMap<String,String> listOfFieldMembers = new ConcurrentHashMap<String,String>();
	static ConcurrentHashMap<String,String> listOfPropertyFields = new ConcurrentHashMap<String,String>();

	static LinkedHashMap<String,String> listOfMemberFunctions = new LinkedHashMap<String,String>();
	static LinkedHashMap<String,String> listOfClassTypeFields = new LinkedHashMap<String,String>();

	static ConcurrentHashMap<String,String> dependencyMultiplicity = new ConcurrentHashMap<String,String>();
	static ConcurrentHashMap<String,String> uses=new ConcurrentHashMap<String,String>();

	static ConcurrentHashMap<String,String> classesExtendedClasses = new  ConcurrentHashMap<String,String>();
	static ConcurrentHashMap<String,String> classesImplementedInterfaces = new  ConcurrentHashMap<String,String>();

	static ArrayList<String> refDataTypes = new ArrayList<String>(){{add("string");add("boolean");add("double");add("arraylist");}};

	static List<String> interfaces=new ArrayList<String>();
	static List<String> classes=new ArrayList<String>();

	static List<LinkedList<MethodCallSequence>> finalList = new ArrayList<LinkedList<MethodCallSequence>>();



	public static void main(String[] args) {

		String dirPath=null;
		String outputFileName=null;
		String outputFileFormat=null;
		File file=null;
		int index=-1;

		if(args.length>0 && args.length==2)
		{
			dirPath=args[0];
			outputFileName=args[1];

			file=new File(dirPath);
			Boolean isValidDirectory = file.isDirectory();
			if(!isValidDirectory)
			{
				System.out.println("Invalid Directory Path.");
				System.exit(1);
			}

			index = outputFileName.indexOf('.');
			if(index<0){
				System.out.println("Please mention the file format along with the filename. For eg: .png or .pdf");
				System.exit(1);
			}
			else
			{
				outputFileFormat = outputFileName.substring(index+1, outputFileName.length());
				Boolean hasAllowedFormat = CheckFileFormatValidity(outputFileFormat);
				if(!hasAllowedFormat)
				{
					System.out.println("Allowed file formats: .png");
					System.exit(1);
				}	
			}
		}
		else{
			System.out.println("Enter command line arguments.\n");
			System.out.println("Cmpe202JavaparserApplication {directory_path_of_JavaSourceFiles} {Name of output file}\n");
			System.exit(1);
		}

		// creates an input stream for the file to be parsed

		FileInputStream in=null;
		try {

			File[] files= file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".java");
				}
			});

			StringBuilder plantUmlSource = new StringBuilder();


			plantUmlSource.append("@startuml\nskinparam classAttributeIconSize 0\nhide interface circle\n");
			for(File currFile:files){
				in = new FileInputStream(dirPath +"/"+currFile.getName());
				CompilationUnit cu=null;
				try {
					// parse the file
					try {
						cu = JavaParser.parse(in);

					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} finally {
					try {
						in.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				// Parse File
				StringBuilder sb = ParseFile(cu,plantUmlSource);

			}
			plantUmlSource.append("\n");



			// Manipulations
			for(int i=0;i<finalList.size();i++){
				LinkedList<MethodCallSequence> llmethCallSequence = new LinkedList<MethodCallSequence>();
				llmethCallSequence=finalList.get(i);
				for(int j=0;j<llmethCallSequence.size();j++){

					Set<String> keySet = listOfClassTypeFields.keySet();
					Iterator<String> keySetIterator = keySet.iterator();

					while (keySetIterator.hasNext()) 
					{
						String key = keySetIterator.next();
						String value=listOfClassTypeFields.get(key);
						if(key.equals(llmethCallSequence.get(j).className+"_"+llmethCallSequence.get(j).classObject))
						{
							llmethCallSequence.get(j).className=value;
						}
					}
				}
			}

			StringBuilder plantUmlSourceSeq = new StringBuilder();
			plantUmlSourceSeq.append("@startuml\nautonumber\n");

			// SequenceDiagram Generation
			List<LinkedList<MethodCallSequence>> finalListCopy = finalList;
			for(int i=0;i<finalList.size();i++)
			{
				LinkedList<MethodCallSequence> llmethCallSequence = new LinkedList<MethodCallSequence>();
				llmethCallSequence=finalList.get(i);
				if(llmethCallSequence.size()>1)
				{
					MethodCallSequence mcs = llmethCallSequence.get(0);
					if(mcs.methodName.equals("main"))
					{
						for(int j=1;j<llmethCallSequence.size();j++)
						{
							int flagFound=0;
							MethodCallSequence mcsInner =llmethCallSequence.get(j); 
							for(int k=0;k<finalListCopy.size();k++)
							{
								LinkedList<MethodCallSequence> llSequenceCopy = finalListCopy.get(k);
								MethodCallSequence mcsInnerCopy =llSequenceCopy.get(0);
								if(!mcsInnerCopy.methodName.equals("main"))
								{
									if((mcsInnerCopy.className.equals(mcsInner.className)&&mcsInnerCopy.methodName.equals(mcsInner.methodName)))
									{
										flagFound=1;
										plantUmlSourceSeq.append(mcs.className+"->"+mcsInner.className+":"+mcsInner.methodName+"\n");	
										for(int l=1;l<llSequenceCopy.size();l++){
											if(l!=llSequenceCopy.size()-1)
											{
												MethodCallSequence mc = new MethodCallSequence();
												mc=llSequenceCopy.get(l);
												plantUmlSourceSeq.append(mcsInner.className+"->"+mc.className+":"+mc.methodName+"\n");
												plantUmlSourceSeq.append(mcsInner.className+"<--"+mc.className+":returnMessageOf"+mc.methodName+ "\n");
											}
											else{
												MethodCallSequence mc = new MethodCallSequence();
												mc=llSequenceCopy.get(l);
												plantUmlSourceSeq.append(mcsInner.className+"->"+mc.className+":"+mc.methodName+"\n");
												plantUmlSourceSeq.append(mcsInner.className+"<--"+mc.className+":returnMessageOf"+mc.methodName+"\n");
												plantUmlSourceSeq.append(mcs.className+"<--"+mcsInner.className+":returnMessageOf"+mcsInner.methodName+"\n");
											}
										}

										break;
									}
								}

							}
							if(flagFound==0)
							{
								plantUmlSourceSeq.append(mcs.className+"->"+mcsInner.className+":"+mcsInner.methodName+"\n");
								plantUmlSourceSeq.append(mcs.className+"<--"+mcsInner.className+":returnMessageOf"+mcsInner.methodName+"\n");
							}
						}
						break;
					}
				}
			}
			plantUmlSourceSeq.append("@enduml\n");
//			System.out.println(plantUmlSourceSeq.toString());

			// Dependencies & Multiplicity
			Set<String> keySet = dependencyMultiplicity.keySet();
			Iterator<String> keySetIterator = keySet.iterator();

			while (keySetIterator.hasNext()) 
			{
				String key = keySetIterator.next();
				String currKey=key;
				String currValue=dependencyMultiplicity.get(key);

				String[] classes = key.split("_");
				Set<String> propkeySet = dependencyMultiplicity.keySet();
				Iterator<String> propkeySetIterator = propkeySet.iterator();
				while(propkeySetIterator.hasNext())
				{   
					String innercurrKey=propkeySetIterator.next();
					String innercurrValue=dependencyMultiplicity.get(innercurrKey);
					String[] innerclasses = innercurrKey.split("_");
					if(classes[0].equals(innerclasses[1]) && classes[1].equals(innerclasses[0]))
					{
						if(!interfaces.contains(classes[1])){
							plantUmlSource.append(classes[0]+"\""+innercurrValue+"\"--\""+currValue+"\""+classes[1]+"\n");
						}
						else{

						}
						dependencyMultiplicity.remove(currKey);
						dependencyMultiplicity.remove(innercurrKey);
					}
				}	
			}

			keySet = dependencyMultiplicity.keySet();
			keySetIterator = keySet.iterator();

			while (keySetIterator.hasNext()) 
			{
				String key = keySetIterator.next();
				String currValue=dependencyMultiplicity.get(key);

				String[] classes = key.split("_");
				plantUmlSource.append(classes[0]+"--\""+currValue+"\""+classes[1]+"\n");
			}

			// Uses
			Set<String> useskeySet= uses.keySet();
			Iterator<String> useskeySetIterator = useskeySet.iterator();
			while (useskeySetIterator.hasNext()) {

				String key = useskeySetIterator.next();

				String[] clas = key.split("_");
				if(!classes.contains(clas[1])){
					plantUmlSource.append(clas[0]+"..>"+clas[1]+":uses\n");
				}

			}

			// Getter Setter
			Set<String> membersKeySet = listOfFieldMembers.keySet();
			Iterator<String> membersKeySetIterator = membersKeySet.iterator();
			while(membersKeySetIterator.hasNext()){
				String key = membersKeySetIterator.next();
				String currKey=key;
				String currValue=listOfFieldMembers.get(key);
				String[] memberDetails = currValue.split("_");

				Set<String> propkeySet = listOfPropertyFields.keySet();
				Iterator<String> propkeySetIterator = propkeySet.iterator();
				while(propkeySetIterator.hasNext())
				{   
					String innercurrKey=propkeySetIterator.next();
					String innercurrValue=listOfPropertyFields.get(innercurrKey);
					String []innerclasses=innercurrKey.split("_");
					if(currKey.equals(innerclasses[0])){

						int indexStart= plantUmlSource.indexOf(memberDetails[0]+memberDetails[1]+memberDetails[2]+memberDetails[3]);
						if(indexStart!=-1)
							plantUmlSource.replace(indexStart, indexStart+1, "+");
						indexStart=plantUmlSource.indexOf(innercurrValue);
						if(indexStart!=-1){
							int indexEnd=innercurrValue.length();
							plantUmlSource.replace(indexStart, indexStart+indexEnd, "");
						}

					}
				}

			}
			plantUmlSource.append("@enduml");
//			System.out.println(plantUmlSource.toString());

			SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
			SourceStringReader readerSeq = new SourceStringReader(plantUmlSourceSeq.toString());

			FileOutputStream output=null;
			FileOutputStream outputSeq=null;
			try {
				output = new FileOutputStream(new File(dirPath+ "/"+outputFileName));
				outputSeq = new FileOutputStream(new File(dirPath+ "/"+"Sequence_"+outputFileName));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			FileFormatOption fileFormat=null;
			if(outputFileFormat.equalsIgnoreCase("png"))
				fileFormat = new FileFormatOption(FileFormat.PNG);
			else
				fileFormat = new FileFormatOption(FileFormat.PDF);
			try {
				reader.generateImage(output, fileFormat);
				readerSeq.generateImage(outputSeq,fileFormat);
				System.out.println("Files Generated successfully at the below path:\n"+dirPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static Boolean CheckFileFormatValidity(String outputFileFormat) {

		// TODO Auto-generated method stub
		if(outputFileFormat.equalsIgnoreCase("png")  || outputFileFormat.equalsIgnoreCase("pdf"))
			return true;
		else
			return false;
	}

	private static StringBuilder ParseFile(CompilationUnit cu, StringBuilder plantUmlSource) {
		// Imports
		List<ImportDeclaration> importsList = cu.getImports();
		if(importsList!=null)
		{
			for(ImportDeclaration indImport : importsList){
				System.out.println("\tName:"+indImport.getName());
				System.out.println("\thasAsterisk:"+indImport.isAsterisk());
				System.out.println("\tisStatic:"+indImport.isStatic());
			}
		}

		PackageDeclaration pkg = cu.getPackage();
		if(pkg!=null)
			System.out.println("Package Name:" +pkg.getName());

		// All Types
		List<TypeDeclaration> types = cu.getTypes();
		for (TypeDeclaration type : types) 
		{
			if(type instanceof EnumDeclaration)
			{
				// Enumeration
				plantUmlSource.append("enum "+type.getName()+"{\n");
				EnumDeclaration enumeration=(EnumDeclaration)type;
				List<EnumConstantDeclaration> enumEntries= enumeration.getEntries();
				if(enumEntries!=null)
				{
					for(EnumConstantDeclaration e:enumEntries){
						plantUmlSource.append(e.getName()+"\n");
					}
				}
				plantUmlSource.append("}\n");
			}
			else if(type instanceof ClassOrInterfaceDeclaration)
			{
				ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) type;
				// Check for a class or interface

				if(classOrInterface.isInterface())
				{
					// Interface name

					plantUmlSource.append("interface "+classOrInterface.getName()+"<<interface>>{\n");
					if(!interfaces.contains(classOrInterface.getName()))
						interfaces.add(classOrInterface.getName());
					// Interface members
					List<BodyDeclaration> members = classOrInterface.getMembers();
					if(members!=null){
						StringBuilder sbField =new StringBuilder();
						for(BodyDeclaration member:members){
							if(member instanceof MethodDeclaration){
								MethodDeclaration method = (MethodDeclaration)member;
								sbField.append(GetUMLMethodOfInterface(method,classOrInterface.getName())+"\n");
							}
						}
						plantUmlSource.append(sbField.toString());
						plantUmlSource.append("}\n");
					}

					// Extended interfaces
					List<ClassOrInterfaceType> extendedInterfaces = classOrInterface.getExtends();
					if(extendedInterfaces!=null){
						for(ClassOrInterfaceType extendedInterface : extendedInterfaces){

							plantUmlSource.append(classOrInterface.getName()+"--|>"+
									extendedInterface.getName()+"\n");

							if(!interfaces.contains(extendedInterface.getName()))
								interfaces.add(extendedInterface.getName());
						}
					}
				}
				else
				{
					// Class Syntax
					if(!classes.contains(classOrInterface.getName()))
						classes.add(classOrInterface.getName());

					plantUmlSource.append("class "+classOrInterface.getName());
					// TypeParameters
					List<TypeParameter> parametersList = classOrInterface.getTypeParameters();
					if(parametersList!=null){
						plantUmlSource.append("<");
						for(int i=0;i< parametersList.size();i++){
							if(i!=parametersList.size()-1){
								plantUmlSource.append(parametersList.get(i).getName()+",");
							}else{
								plantUmlSource.append(parametersList.get(i).getName());
							}
						}
						plantUmlSource.append(">");
					}

					plantUmlSource.append("{\n");

					// Body Declaration of class
					List<BodyDeclaration> members = classOrInterface.getMembers();
					if(members!=null)
					{
						StringBuilder sbField =new StringBuilder();
						for(BodyDeclaration member:members)
						{
							// Check for the type of member
							if(member instanceof FieldDeclaration)
							{
								// Field Declaration in class
								FieldDeclaration field =(FieldDeclaration)member;
								sbField.append(GetUMLFieldsInClass(field,classOrInterface.getName())+"\n");

							}
							else if(member instanceof MethodDeclaration) 
							{

								// Method Declaration
								MethodDeclaration method = (MethodDeclaration)member;

								listOfMemberFunctions.put(classOrInterface.getName()+"_"+method.getName(), "1");
								String appendMethod = GetUMLMethod(method,classOrInterface.getName())+"\n";

								if(appendMethod.length()>1)
								{
									sbField.append(appendMethod);
									ParseMethodBody(method,appendMethod,classOrInterface.getName());
								}
							}
							else if(member instanceof ConstructorDeclaration)
							{
								ConstructorDeclaration constructor =(ConstructorDeclaration)member;
								sbField.append(GetUMLConstructor(constructor,classOrInterface.getName())+"\n");
							}
						}
						plantUmlSource.append(sbField.toString());
						plantUmlSource.append("}\n");
					}


					// Extended classes
					List<ClassOrInterfaceType> extendedClasses = classOrInterface.getExtends();
					if(extendedClasses!=null){
						for(ClassOrInterfaceType extendedClass : extendedClasses){
							plantUmlSource.append(classOrInterface.getName()+"--|>"+
									extendedClass.getName()+"\n");

							if(!classes.contains(extendedClass.getName()))
								classes.add(extendedClass.getName());
						}
					}

					// Interfaces implemented
					List<ClassOrInterfaceType> interfaceList = classOrInterface.getImplements();
					if(interfaceList!=null){
						for(ClassOrInterfaceType indInterface : interfaceList){
							plantUmlSource.append("interface "+indInterface.getName()+"\n");
							plantUmlSource.append(classOrInterface.getName()+"..|>"+
									indInterface.getName()+"\n");

							if(!interfaces.contains(indInterface.getName()))
								interfaces.add(indInterface.getName());

						}
					}
				}
			}
		}
		return plantUmlSource;
	}

	private static void ParseMethodBody(MethodDeclaration method, String appendMethod, String className) {
		//		// TODO Auto-generated method stub
		LinkedList<MethodCallSequence> llmethCallSequence = new LinkedList<MethodCallSequence>();
		MethodCallSequence headmcseqObj = new MethodCallSequence();
		headmcseqObj.className=className;
		headmcseqObj.classObject=className;
		headmcseqObj.methodName=method.getName();
		llmethCallSequence.add(headmcseqObj);

		BlockStmt blockStmt = method.getBody();
		if(blockStmt!=null){
			List<Statement> statementsList= blockStmt.getStmts();
			if(statementsList!=null)
			{
				for(Statement statement:statementsList)
				{
					if(statement instanceof ReturnStmt)
					{
						ReturnStmt retStmt =(ReturnStmt)statement;
						Expression exp = retStmt.getExpr();
						if(exp instanceof FieldAccessExpr)
						{
							FieldAccessExpr fieldAccessExpr =(FieldAccessExpr)exp;
							listOfPropertyFields.put(fieldAccessExpr.getField()+"_"+appendMethod, appendMethod);
						}
						else if(exp instanceof NameExpr)
						{
							NameExpr nameExp =(NameExpr)exp;
							String methodName = method.getName().toLowerCase();
							if(methodName.contains("get")){
								methodName=methodName.replaceFirst("get", "");
								Set<String> membersKeySet = listOfFieldMembers.keySet();
								Iterator<String> membersKeySetIterator = membersKeySet.iterator();
								while(membersKeySetIterator.hasNext()){
									String key = membersKeySetIterator.next();
									if(key.equals(nameExp.getName().toString()) && key.equals(methodName)){
										listOfPropertyFields.put(nameExp.getName().toString()+"_"+appendMethod, appendMethod);
										break;
									}
								}
							}
						}

					}
					else if(statement instanceof TypeDeclarationStmt)
					{
						TypeDeclarationStmt typeDecStmt =(TypeDeclarationStmt)statement;
					}
					else if(statement instanceof ExpressionStmt)
					{
						ExpressionStmt exprStmt =(ExpressionStmt)statement;
						Expression exp = exprStmt.getExpression();
						String fieldName="";
						if(exp instanceof AssignExpr)
						{
							String methodName = method.getName().toLowerCase();
							if(methodName.contains("set")){
								methodName=methodName.replaceFirst("set", "");
								AssignExpr assignExpr = (AssignExpr)exp;
								fieldName=assignExpr.getTarget().toString();
								if(fieldName.contains("this."))
								{
									fieldName= fieldName.substring(fieldName.indexOf(".")+1, fieldName.length());
								}
								String setterParams = appendMethod.substring(appendMethod.indexOf("(")+1, appendMethod.indexOf(")"));
								String []Param = setterParams.split(",");
								if(Param[0]!=""){

									String []paramsList = Param[0].split(":");
									if(paramsList[0].equals(assignExpr.getValue().toString())&&fieldName.equals(methodName)){
										listOfPropertyFields.put(fieldName+"_"+appendMethod, appendMethod);
									}
								}
							}
						}
						else if(exp instanceof VariableDeclarationExpr)
						{
							VariableDeclarationExpr varDecExpr = (VariableDeclarationExpr)exp;
							List<VariableDeclarator> vars = varDecExpr.getVars();
							if(vars!=null){
								for(VariableDeclarator var:vars){
								}
							}
							Type varDecExprType= varDecExpr.getType();

							if(varDecExprType instanceof ReferenceType)
							{
								for(VariableDeclarator var:vars)
								{
									VariableDeclaratorId varId=var.getId();
									ReferenceType tt =(ReferenceType)varDecExprType;
									Type refType = tt.getType();
									if(refType instanceof ClassOrInterfaceType)
									{
										String regex="[a-zA-Z]+<[a-zA-Z]+>";
										if(tt.getType().toString().matches(regex)){
											String varType = tt.getType().toString();
											String multiplicityClass= varType.substring(varType.indexOf("<")+1, varType.indexOf(">"));
											uses.put(className+"_"+multiplicityClass, "uses");

											//For Sequence Diagram
											listOfClassTypeFields.put(className+"_"+varId.getName(), multiplicityClass);
										}
										else if(!refDataTypes.contains(tt.getType().toString().toLowerCase())){
											uses.put(className+"_"+tt.getType().toString(), "uses");
											listOfClassTypeFields.put(className+"_"+varId.getName(),tt.getType().toString());
										}
									}
								}
							}
						}
						else if(exp instanceof MethodCallExpr)
						{
							MethodCallExpr methCallExpr = (MethodCallExpr)exp;
							MethodCallSequence mcseqObj = new MethodCallSequence();
							mcseqObj.className=className;
							mcseqObj.methodName=methCallExpr.getName();
							if(methCallExpr.getScope()!=null)
								mcseqObj.classObject=methCallExpr.getScope().toString();
							else
								mcseqObj.classObject=className;
							llmethCallSequence.add(mcseqObj);
						}
					}
					else if(statement instanceof IfStmt){
						IfStmt ifStmt =(IfStmt)statement;
						Expression ifExp= ifStmt.getCondition();
						if(ifExp instanceof MethodCallExpr)
						{
							MethodCallExpr methCallExpr = (MethodCallExpr)ifExp;
							MethodCallSequence mcseqObj = new MethodCallSequence();
							mcseqObj.className=className;
							mcseqObj.methodName=methCallExpr.getName();
							if(methCallExpr.getScope()!=null)
								mcseqObj.classObject=methCallExpr.getScope().toString();
							else
								mcseqObj.classObject=className;
							llmethCallSequence.add(mcseqObj);
						}

						Statement elseStatement= ifStmt.getElseStmt();

						Statement thenStatement= ifStmt.getThenStmt();
						if(thenStatement instanceof ExpressionStmt){
							ExpressionStmt exprStmt =(ExpressionStmt)thenStatement;
							Expression exp = exprStmt.getExpression();
							if(exp instanceof MethodCallExpr)
							{
								MethodCallExpr methCallExpr = (MethodCallExpr)exp;
								MethodCallSequence mcseqObj = new MethodCallSequence();
								mcseqObj.className=className;
								mcseqObj.methodName=methCallExpr.getName();
								if(methCallExpr.getScope()!=null)
									mcseqObj.classObject=methCallExpr.getScope().toString();
								else
									mcseqObj.classObject=className;
								llmethCallSequence.add(mcseqObj);
							}
						}

					}
					else{
						System.out.println("\tElse");
						System.out.println("\t"+statement.toString());
					}

				}//for ends

				finalList.add(llmethCallSequence);
			} // empty statements check ends
		}

	}//method ends

	private static String GetUMLConstructor(ConstructorDeclaration constructor,String className) {
		// TODO Auto-generated method stub
		StringBuilder sbField = new StringBuilder();
		String appendFieldType=GetFieldType(constructor.getModifiers());
		if(appendFieldType.startsWith("+")){
			sbField.append(GetFieldType(constructor.getModifiers()));
			sbField.append(constructor.getName()+"(");

			List<Parameter> params = constructor.getParameters();
			if(params!=null){
				for(int i=0;i< params.size();i++){
					Type typeofParam = params.get(i).getType();
					if(typeofParam instanceof ReferenceType)
					{
						ReferenceType refTypeParam = (ReferenceType)typeofParam;
						//here
						if(!refDataTypes.contains(refTypeParam.getType().toString().toLowerCase())  && refTypeParam.getType() instanceof ClassOrInterfaceType)
						{
							ClassOrInterfaceType tt =(ClassOrInterfaceType)refTypeParam.getType();
							uses.put(className+"_"+tt.getName(), "use");
						}

					}
					VariableDeclaratorId varId= params.get(i).getId();
					if(i!=params.size()-1){
						sbField.append(varId.getName()+":"+params.get(i).getType()+",");
					}else{
						sbField.append(varId.getName()+":"+params.get(i).getType());
					}
				}

			}
			sbField.append(")\n");
		}
		return sbField.toString();
	}

	private static String GetUMLMethod(MethodDeclaration method, String className) {
		// TODO Auto-generated method stub
		StringBuilder sbField = new StringBuilder();

		String appendType=GetFieldType(method.getModifiers());
		if(appendType.startsWith("+"))
		{
			sbField.append(appendType);
			sbField.append(method.getName()+"(");

			List<Parameter> params = method.getParameters();
			if(params!=null)
			{
				for(int i=0;i< params.size();i++)
				{
					Type typeofParam = params.get(i).getType();
					if(typeofParam instanceof ReferenceType)
					{
						ReferenceType refTypeParam = (ReferenceType)typeofParam;
						if(!refDataTypes.contains(refTypeParam.getType().toString().toLowerCase()) && refTypeParam.getType() instanceof ClassOrInterfaceType)
						{
							ClassOrInterfaceType tt =(ClassOrInterfaceType)refTypeParam.getType();
							uses.put(className+"_"+tt.getName(), "use");
						}

					}
					VariableDeclaratorId varId= params.get(i).getId();
					if(i!=params.size()-1)
					{
						sbField.append(varId.getName()+":"+params.get(i).getType()+",");
					}
					else
					{
						sbField.append(varId.getName()+":"+params.get(i).getType());
					}
				}
			}
			sbField.append("):");
			sbField.append(method.getType().toString()+"\n");
		}
		return sbField.toString();
	}

	private static String GetUMLMethodOfInterface(MethodDeclaration method, String className) {
		// TODO Auto-generated method stub

		StringBuilder sbField = new StringBuilder();

		sbField.append(GetFieldType(method.getModifiers()));
		sbField.append(method.getName()+"(");

		List<Parameter> params = method.getParameters();
		if(params!=null)
		{
			for(int i=0;i< params.size();i++)
			{
				VariableDeclaratorId varId= params.get(i).getId();
				if(i!=params.size()-1)
				{
					sbField.append(varId.getName()+":"+params.get(i).getType()+",");
				}
				else
				{
					sbField.append(varId.getName()+":"+params.get(i).getType());
				}
			}
		}
		sbField.append("):");
		sbField.append(method.getType().toString()+"\n");

		return sbField.toString();
	}



	private static String GetUMLFieldsInClass(FieldDeclaration n, String className) {
		StringBuilder sbField = new StringBuilder();
		List<VariableDeclarator> vars = n.getVariables();
		if(vars!=null)
		{
			if(n.getType() instanceof PrimitiveType)
			{
				for(VariableDeclarator var:vars)
				{
					String appendFieldType= GetFieldType(n.getModifiers());
					if(appendFieldType.startsWith("+")||appendFieldType.startsWith("-")){
						sbField.append(appendFieldType);
						sbField.append(var.toString());
						sbField.append(':');
						sbField.append(n.getType().toString() + "\n");
						listOfFieldMembers.put(var.toString(), 
								appendFieldType+"_"+var.toString()+"_"+":"+"_"+n.getType().toString());
					}

				}
			}
			else if(n.getType() instanceof ReferenceType)
			{
				for(VariableDeclarator var:vars)
				{
					ReferenceType tt =(ReferenceType)n.getType();
					Type refType = tt.getType();
					if(refType instanceof PrimitiveType)
					{
						String appendFieldType= GetFieldType(n.getModifiers());
						if(appendFieldType.startsWith("+")||appendFieldType.startsWith("-")){
							sbField.append(appendFieldType);
							sbField.append(var.toString());
							sbField.append(':');
							sbField.append(n.getType().toString() + "\n");
							listOfFieldMembers.put(var.toString(), 
									appendFieldType+"_"+var.toString()+"_"+":"+"_"+n.getType().toString());
						}
					}
					else if(refType instanceof ClassOrInterfaceType)
					{
						String regex="[a-zA-Z]+<[a-zA-Z]+>";
						if(tt.getType().toString().matches(regex)){
							String varType = tt.getType().toString();
							String multiplicityClass= varType.substring(varType.indexOf("<")+1, varType.indexOf(">"));
							dependencyMultiplicity.put(className+"_"+multiplicityClass, "*");
							listOfClassTypeFields.put(className+"_"+var.getId().getName(),multiplicityClass);
						}
						else if(refDataTypes.contains(tt.getType().toString().toLowerCase())){
							String appendFieldType= GetFieldType(n.getModifiers());
							if(appendFieldType.startsWith("+")||appendFieldType.startsWith("-")){
								sbField.append(appendFieldType);
								sbField.append(var.toString());
								sbField.append(':');
								sbField.append(n.getType().toString() + "\n");
								listOfFieldMembers.put(var.toString(), 
										appendFieldType+"_"+var.toString()+"_"+":"+"_"+n.getType().toString());

							}
						}else{
							dependencyMultiplicity.put(className+"_"+tt.getType().toString(), "1");

							listOfClassTypeFields.put(className+"_"+var.getId().getName(),tt.getType().toString());
						}
					}
				}
			}
			else{
				System.out.println("Else");
				System.out.println(n.toString());
				Type tt =n.getType();
				System.out.println(tt.getClass().toString());
			}
		}
		return sbField.toString();
	}

	private static String GetFieldType(Integer type) {
		// TODO Auto-generated method stub

		switch(type)
		{
		case 1: return "+";
		case 2: return "-";
		case 4: return "#";
		case 8: return "- {static}";
		case 9: return "+ {static}";
		case 10: return "- {static}";
		case 12: return "# {static}";
		case 16: return "final";
		case 1024: return "- {abstract}";
		case 1025: return "+ {abstract}";
		case 1026: return "- {abstract}";
		case 1028: return "# {abstract}";

		default: return " ";
		}
	}
}
