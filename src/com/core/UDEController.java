package com.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sun.org.apache.regexp.internal.recompile;

/*
 * @author : Ansar Samad
 * 
 */
public class UDEController  {


	private static String MAIN_VERSION = "";
	private static String MAIN_1_VERSION = "";
	private static String MAIN_2_VERSION = "";
	private static String BUILD_DEST_PATH = "dist/lib";
	private static String errorText = "";
	private static String DEST_FOLDER = "";
	private static HashMap<String, Map<String ,String>> udeMap = new LinkedHashMap<String, Map<String ,String>>();
	private Map<String, String> projectsMap = new LinkedHashMap<String, String>();
	private String version = null;
	private static String MAIN_ENTERPRISE_JAR = null;
	private static String MAIN_ENTERPRISE_JAR_NAME = null;
	private static String MAIN_IBSERVLET_JAR_NAME = null;
	// 16 version
	private List<String> mainDependencyProject = null;
	//15 version
	private List<String> main_1_DependencyProject = null;
	//14 version
	private List<String> main_2_DependencyProject = null;
	
	public UDEController() {

	}

	public UDEController(String version, Map<String ,String> projectMap) {
		this.version = version;
		this.projectsMap = projectMap;
	}
	
	public static void main(String[] args) {
		if(args != null && args[0] != null && args[0].length() > 0 && args[0].endsWith("properties")){		
			long startingTime = System.nanoTime();
			String propertyFile = args[0];
			boolean isValid = new UDEController().setupContext(propertyFile);
			if (!isValid) {
				udeMap.clear();
				System.out.println(errorText);
				return;
			}
					
			Iterator<String> it = udeMap.keySet().iterator();
			while (it.hasNext()) {
				String version = (String) it.next();
				UDEController udeHelper = new UDEController(version,udeMap.get(version));
				udeHelper.executeTask();
			}
			
			System.out.println("\nFinished all threads");
			long endingTime = System.nanoTime();
			float timems = (float)(endingTime - startingTime)/1000000;
			System.out.println("time taken :"+(timems/1000)+" seconds");
		} else {
			System.out.println("expected argument not fund , context property file missing");
		}

	}

	
	private void clearDistLib(String project) {
		File file = new File(project + "/" + BUILD_DEST_PATH);
		System.out.println("clearing dis/ib of project :" + project);
		for (File files : file.listFiles()) {
			if (files.isDirectory() || files.getName().endsWith(".jar") || files.getName().endsWith(".war")) {
				files.delete();
			}
		}
	}

	private void clearEnterpriseJarFromLib(String project, String version) {
		System.out.println("clearing enterprise jar from lib folder");
		File file = new File(project + "/lib");
		for (File files : file.listFiles()) {
			if (!files.isDirectory() && files.getName().endsWith(".jar")
					&&( ( files.getName().indexOf("cavion") > -1 && files.getName().indexOf("enterprise") > -1) || (files.getName().indexOf("cavion") > -1 && files.getName().indexOf("internetbanking") > -1))){
				files.delete();				
			}
		}

	}

	/*
	 * process builder accept the external application as argument 
	 * return the newly created jar
	 */
	private String buildProject(String project) {
		System.out.println("building project :"+project);
		try{
			clearDistLib(project);
			File file = new File(project+"\\build\\");
			int flag = 0;
			ProcessBuilder processBuilder = new ProcessBuilder(project+"\\build\\BUILD.BAT");
			processBuilder.directory(file);
			Process process = processBuilder.start();
			flag = process.waitFor();
			System.out.println("builed completed : "+flag);				
				
		}catch(Exception io){
			System.out.println(io);
		}
		String jar = findJar(project);
		if(jar == null){
			System.out.println("exception building project :"+project);
			System.out.println("please confirm that the project is error free and run again ");
			System.out.println("exiting ....");
			System.exit(0);
		}
		return jar;
	}
	
	private String findJar(String project){
		File file = new File(project + "/" + BUILD_DEST_PATH);
		for (File files : file.listFiles()) {
			if ((files.getName().endsWith(".jar") || files.getName().endsWith(".war")) && !files.getName().contains("checkdigit")) {
				return files.getAbsolutePath();
			}
		}
		return null;
	}

	private void copyFiles( String destFile ,String... sourceFiles) {
		if (sourceFiles != null && destFile !=null) {		
			Path destPath = Paths.get(destFile);
			
			File tmp = destPath.toFile();
			if(!tmp.exists()) {
				System.out.println("destnation file not exist creating new file");
				try{
			    tmp.mkdir();
				}catch(Exception e){
					System.out.println(e);
				}
			} 
			
			for (String file : sourceFiles) {
				Path sourcePath = Paths.get(file);
				try {
					if(sourcePath != null && destPath != null){
						Files.copy(sourcePath,
								destPath.resolve(sourcePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

	private void updateClassPAth(String version, String project, String... entry) {
		System.out.println("reading class path for the project :" + project);
		File f = new File(project + "\\" + ".classpath");
		FileInputStream fs = null;
		InputStreamReader in = null;
		BufferedReader br = null;
		StringBuffer sb = new StringBuffer();

		String textinLine;

		try {
			fs = new FileInputStream(f);
			in = new InputStreamReader(fs);
			br = new BufferedReader(in);

			while (true) {
				textinLine = br.readLine();
				if (textinLine == null)
					break;
				for(String en : entry){
					if(!textinLine.contains(en)){
						if ((en.indexOf("enterprise") > -1 && textinLine.indexOf("cavion") > -1 && textinLine.indexOf("enterprise") > -1) || (en.indexOf("internetbanking") > -1 && textinLine.indexOf("cavion") > -1 && textinLine.indexOf("internetbanking") > -1)){
							textinLine =  textinLine.substring(0,textinLine.indexOf("cavion"));
							textinLine = textinLine.trim()+en.trim()+ "\""+"/>";
							textinLine = "\t"+textinLine;
							break;
						}
					}	
										
				}
				sb.append(textinLine);
				sb.append("\n");
				
			}
			

			fs.close();
			in.close();
			br.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileWriter fstream = new FileWriter(f);
			BufferedWriter outobj = new BufferedWriter(fstream);
			outobj.write(sb.toString().trim());
			outobj.close();
			System.out.println("class path updated successfully");
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

	}

	private boolean setupContext(String property) {
		Properties prop = new Properties();
		InputStream input = null;
		errorText = "";
		try {
			input = new FileInputStream(property);
			prop.load(input);
			String root = prop.getProperty("root-folder");
			String version = prop.getProperty("versions");
			DEST_FOLDER = prop.getProperty("target-folder");
			Map<String,String> projectMap = null;
			if(!version.contains("main")){
				version = "main"+","+version;
			}
			if (root != null && root.length() > 1 && version != null
					&& version.length() > 0 ) {
				String[] versionArray = version.split(",");
				Arrays.sort(versionArray,Collections.reverseOrder());				
				int i=0;
				for (String vers : versionArray) {
					String dependency = prop.getProperty(vers+"-dependency");
					if(dependency != null && !dependency.isEmpty()){
						List<String> tempList = Arrays.asList(dependency.split(",")); // null check						
						projectMap = new LinkedHashMap<String, String>();
						for (String proj : tempList) {
							File projectFile = new File(root + "/" + vers + "/"+ proj);
							if (projectFile.exists()) {
								projectMap.put(proj ,projectFile.getAbsolutePath());
								
								continue;
							} else {
								errorText = "project not found :"
										+ projectFile.getAbsolutePath();
								return false;
							}
						}
						udeMap.put(vers, projectMap);
						
						
						if(i == 0 && vers.contains("main")){
							MAIN_VERSION = vers;
							mainDependencyProject = tempList;
						}else if(i == 1){ // n-1
							MAIN_1_VERSION = vers;
							main_1_DependencyProject = tempList;
						}else{
							MAIN_2_VERSION = vers;
							main_2_DependencyProject = tempList;
						}					
					i++;
					}else{
						errorText = "required dependency is not found for the "+vers+" version ";
						return false;
					}
				}
				System.out.println("context setup completed");
			} else {
				errorText = "context.property is not setup properly";
				return false;
			}

			return true;
		} catch (Exception ex) {
			errorText = ex.getMessage();
			return false;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	private void executeTask(){
		try{
			System.out.println("executing task for version :" + this.version + " ---- "+ this.projectsMap);
			String ibSevletLocation = projectsMap.get("ib-servlets"); 
			if(version.equals(MAIN_VERSION)){ // if 14 version there is no enterprise jar
				//clearDistLib(projectsMap.get("enterprise"));	// clear enterprise dist/lib
				if(projectsMap.containsKey("enterprise")){
					MAIN_ENTERPRISE_JAR = buildProject(projectsMap.get("enterprise"));	// build enterprise		
					System.out.println("MAIN_ENTERPRISE_JAR : "+MAIN_ENTERPRISE_JAR);
					MAIN_ENTERPRISE_JAR_NAME = MAIN_ENTERPRISE_JAR.replace(projectsMap.get("enterprise")+"\\dist\\lib\\", " ");
				}
			}			
			
		if(projectsMap.containsKey("ib-servlets")){	
			String destination = projectsMap.get("ib-servlets") +"/lib";
			System.out.println("copy enterprise jar to sefvlet lib");
			copyFiles(destination,MAIN_ENTERPRISE_JAR); 		// copy new enterprise jar to ib servlet/lib
			System.out.println(""+MAIN_ENTERPRISE_JAR);
			System.out.println("jar : "+projectsMap.get("enterprise"));
			System.out.println("binding new enterprise jar into servlet");
			updateClassPAth(this.version, ibSevletLocation, MAIN_ENTERPRISE_JAR_NAME.trim()); // bind new enterprise jar to ib servlet

			String ibservletjar =  buildProject(ibSevletLocation); 	// build ib servlet
			MAIN_IBSERVLET_JAR_NAME = ibservletjar.replace(projectsMap.get("ib-servlets")+"\\dist\\lib\\", " "); 
			
			if(version.equals(MAIN_VERSION)){ // if not 13 version
				destination = projectsMap.get("ib-admin") +"/lib";
				System.out.println("clearing enterprise and ib jars from ib admin lib");
				clearEnterpriseJarFromLib(projectsMap.get("ib-admin"), this.version); // clear enterprise and Ib jar from id-admin
				copyFiles(destination,MAIN_ENTERPRISE_JAR,ibservletjar); 	// copy enterprise and IB jar to ib admin/ib 
				System.out.println("binding new jar into ib admin");
				updateClassPAth(this.version, projectsMap.get("ib-admin"), MAIN_ENTERPRISE_JAR_NAME.trim(),MAIN_IBSERVLET_JAR_NAME.trim());
				buildProject(projectsMap.get("ib-admin")); 	// build ib admin
			}
	    }
		if(projectsMap.containsKey("templates")){	
			buildProject(projectsMap.get("templates")); //	build templates
		}
		if(projectsMap.containsKey("ib-multi-tenant-build")){
			String warFile = buildProject(projectsMap.get("ib-multi-tenant-build")); //	build ib multi tenant build
			if(DEST_FOLDER == null || DEST_FOLDER.trim().length() == 0 ){
				DEST_FOLDER = "C:/ude-war";
			}
			System.out.println("moving war to destination folder");
			copyFiles(DEST_FOLDER, warFile);
		}
			
		}catch(Exception e){
			e.printStackTrace();			
			System.exit(0);
		}
	}

}
