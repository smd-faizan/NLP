package com.commonSense;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class DependencyExtractor {
	
	static Logger log = Logger.getLogger(DependencyExtractor.class.getName());
	TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	Properties prop = null;
	String PCG_MODEL = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";

	private final TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(),
			"invertible=true");
	public LexicalizedParser parser = null;
	
	public Tree parse(String str) {
		List<CoreLabel> tokens = tokenize(str);
		Tree tree = parser.apply(tokens);
		return tree;
	}
	
	public List<Tree> parseFromFile(LexicalizedParser lp, String fileUrl) {
		List<Tree> result = new ArrayList<Tree>();
		for(List<HasWord> sentence : new DocumentPreprocessor(fileUrl)) {
			Tree parse = lp.apply(sentence);
			result.add(parse);
		}
		return result;
	}
	
	public List<CoreLabel> tokenize(String str) {
		Tokenizer<CoreLabel> tokenizer = tokenizerFactory.getTokenizer(new StringReader(str));
		return tokenizer.tokenize();
	}

	
	
	public void startParsing(){
		try{
		  parser = LexicalizedParser.loadModel(PCG_MODEL);
			loadPropertyFile("config.properties");
			Iterator<File> iter = getCorpusFiles();
			getKnowledgeFromFiles(iter);
		} catch(FileNotFoundException f){
			log.error(f.toString());
		} catch(IOException i){
			log.error(i.toString());
		}
	}
	
	public void loadPropertyFile(String fileName) throws IOException,FileNotFoundException{
		prop = new Properties();
		FileInputStream input = new FileInputStream(fileName);
		prop.load(input);
		log.info("###Properties file loaded successfully### ");
	}
	
	public Iterator<File> getCorpusFiles(){
		String accepted_file_format = prop.getProperty("FILE_FORMAT");
		String corpus_directory = prop.getProperty("CORPORA_DIRECTORY");
		log.info("###Corpus Directory [ "+corpus_directory+" ]###");
		log.info("###Accepted File Format [ "+accepted_file_format+" ]###");
		String[] patron = accepted_file_format.split(",");
		Iterator<File> iter =  FileUtils.iterateFiles(new File(corpus_directory), patron, true);
		log.info("###Returning Corpus File List###");
		return iter;
	}
	
	public void getKnowledgeFromFiles(Iterator<File> iter) throws FileNotFoundException,IOException{
		while(iter.hasNext()){
			File currentFile = iter.next();
			List<Tree> treeList = parseFromFile(parser, currentFile.getAbsolutePath());
				for(Tree t : treeList){
					List<TypedDependency> dependencyList;
					if(t != null){
						dependencyList = parseSingleTree(t);
						Map<String,String> knowledge_map= extractRequiredKnowledge(dependencyList);
						if(knowledge_map!=null) {
							String verb1 = knowledge_map.get("#VERB1#");
							String verb2 = knowledge_map.get("#VERB2#");
							if(knowledge_map.containsKey("nsubj_"+verb2) && knowledge_map.get("nsubj_"+verb2).equals(knowledge_map.get("nsubj_"+verb1))){

								log.info("#########################");
								log.info("###Found Knowledge###");
								log.info("#File Name [ "+currentFile.getName()+" ] #");
								//log.info("#Sentence that gave knowledge [ "+sentence+" ]#");
								System.out.println("Verb 1 nsubj = verb 2 nsubj");
								log.info("Verb 1 nsubj = verb 2 nsubj");
								log.info("#########################");
							}
						
							if(knowledge_map.containsKey("nsubj_"+verb2) && knowledge_map.get("nsubj_"+verb2).equals(knowledge_map.get("dobj_"+verb1))){

								log.info("#########################");
								log.info("###Found Knowledge###");
								log.info("#File Name [ "+currentFile.getName()+" ] #");
								//log.info("#Sentence that gave knowledge [ "+sentence+" ]#");
								System.out.println("Verb 1 dobj = verb 2 nsubj");
								log.info("Verb 1 dobj = verb 2 nsubj");
								log.info("#########################");
							}
						
							if(knowledge_map.containsKey("dobj_"+verb2) && knowledge_map.get("dobj_"+verb2).equals(knowledge_map.get("nsubj_"+verb1))){

								log.info("#########################");
								log.info("###Found Knowledge###");
								log.info("#File Name [ "+currentFile.getName()+" ] #");
								//log.info("#Sentence that gave knowledge [ "+sentence+" ]#");
								System.out.println("Verb 1 nsubj = verb 2 dobj_");
								log.info("Verb 1 nsubj = verb 2 dobj_");
								log.info("#########################");
							}
						
							if(knowledge_map.containsKey("dobj_"+verb2) && knowledge_map.get("dobj_"+verb2).equals(knowledge_map.get("dobj_"+verb1))){

								log.info("#########################");
								log.info("###Found Knowledge###");
								log.info("#File Name [ "+currentFile.getName()+" ] #");
								//log.info("#Sentence that gave knowledge [ "+sentence+" ]#");
								System.out.println("Verb 1 dobj = verb 2 dobj");
								log.info("Verb 1 dobj = verb 2 dobj");
								log.info("#########################");
							}
						}
					}
				}
			}
		}
	
	
	public List<TypedDependency> parseSentence(String sentence){
		//sentence = "Tom bullied Rom so we rescued Ron";
		log.info("###Given sentence to be parsed [ "+sentence+" ]###");
		Tree parse = parse(sentence);
		// parse.pennPrint();

		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		log.info("###Returned Typed Dependency List###");
		return tdl;
		// TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
				// tp.printTree(parse);
		
	}
	
	public List<TypedDependency> parseSingleTree(Tree tree){
		GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		log.info("###Returned Typed Dependency List###");
		return tdl;
		// TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
				// tp.printTree(parse);
		
	}
	public Map<String,String> extractRequiredKnowledge(List<TypedDependency> tdl){
		log.info("###Given list "+tdl+"###");
		boolean advcl = false;
		boolean mark = false;
		String verb1=null, verb2=null;
		Map<String,String> word_dependency_map = new HashMap<String,String>();
		for (TypedDependency node : tdl) {
			String node_str = node.toString();
			String map_key = node_str.split("\\(")[0];
			String temp = node_str.split("\\(")[1];
			map_key = map_key +"_"+temp.split("-")[0];
			String map_value = temp.split(",")[1].trim().split("-")[0];
			word_dependency_map.put(map_key, map_value);
			if (node_str.startsWith("mark")) {
				String discourse_connector = node_str.split("\\(")[1];
				discourse_connector = discourse_connector.split(",")[1];
				discourse_connector = discourse_connector.split("-")[0].trim();
				if (prop.getProperty("DISCOURSE_CONNECTORS").contains(discourse_connector))
					mark = true;
			}
			if (node_str.startsWith("advcl")) {
				advcl = true;
				temp = node_str.substring(node_str.indexOf('(') + 1, node_str.length() - 1);
				verb1 = temp.split(",")[0].trim();
				verb1 = verb1.substring(0, verb1.length()-2);
				verb2 = temp.split(",")[1].trim();
				verb2 = verb2.substring(0, verb2.length()-2);
				word_dependency_map.put("#VERB1#", verb1);
				word_dependency_map.put("#VERB2#", verb2);
			}
		}
		if(mark && advcl)
			return word_dependency_map;
		else
			return null;
	}
}