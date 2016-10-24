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
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalRelation;
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
			// parses the whole while and returns trees for every sentence
			List<Tree> treeList = parseFromFile(parser, currentFile.getAbsolutePath());
				for(Tree t : treeList){
					getKnowledgeFromTree(currentFile, t);
				}
		}
						
	}

	/**
	 * @param currentFile
	 * @param t
	 */
	private void getKnowledgeFromTree(File currentFile, Tree t) {
		List<TypedDependency> dependencyList;
		if(t != null){
			dependencyList = parseSingleTree(t);
			
			for(TypedDependency dependency1: dependencyList){
				if(dependency1.reln().getShortName().equals("advcl")){
					IndexedWord verb1 = dependency1.gov();
					IndexedWord verb2 = dependency1.dep();
					// find nsubj and dobj of verb1 and verb2
					IndexedWord nsubjverb1 = null;
					IndexedWord dobjverb1 = null;
					IndexedWord nsubjverb2 = null;
					IndexedWord dobjverb2 = null;
					for(TypedDependency dependency2: dependencyList){
						// get nsubj, dobj of verb1 and verb2
						if(dependency2.reln().getShortName().equals("nsubj")){
							if(dependency2.gov().compareTo(verb1) == 0)
								nsubjverb1 = dependency2.dep();
							else if(dependency2.gov().compareTo(verb2) == 0)
								nsubjverb2 = dependency2.dep();
						}
						if(dependency2.reln().getShortName().equals("dobj")){
							if(dependency2.gov().compareTo(verb1) == 0)
								dobjverb1 = dependency2.dep();
							else if(dependency2.gov().compareTo(verb2) == 0)
								dobjverb2 = dependency2.dep();
						}
					}
					// if nsubj(verb1) == nsubj(verb2), then print it
					if(nsubjverb1 != null && nsubjverb2 != null && nsubjverb1.toString().equals(nsubjverb2.toString())){
						IndexedWord neg1 = null;
					  IndexedWord neg2 = null;
						for(TypedDependency dependency3: dependencyList){
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb1)==0)
								neg1=dependency3.dep();
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb2)==0)
								neg2=dependency3.dep();
						}
						log.info("#########################");
						log.info("###Found Knowledge###");
						log.info("#File Name [ "+currentFile.getName()+" ] #");
					  log.info("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  System.out.println("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  if(neg1 != null && neg2 != null){
							log.info(neg1+" "+verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println(neg1+" "+verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println("not verb 1 dobj = not verb 2 dobj");
							log.info("not verb 1 dobj = not verb 2 dobj");
						}
						else if(neg1 == null && neg2 == null){
							log.info(verb1+" "+nsubjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println(verb1+" "+nsubjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println("verb 1 dobj = verb 2 dobj");
							log.info("verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 != null && neg2 == null){
							log.info(neg1+" "+verb1+" "+nsubjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println(neg1+" "+verb1+" "+nsubjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println("not verb 1 dobj = verb 2 dobj");
							log.info("not verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 == null && neg2 != null){
							log.info(verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println(verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println("verb 1 dobj = not verb 2 dobj");
							log.info("verb 1 dobj = not verb 2 dobj");
						}
						log.info("#########################");
					}
					// if nsubj(verb1) == dobj(verb2), then print it
					if(nsubjverb1 != null && dobjverb2 !=null && nsubjverb1.toString().equals(dobjverb2.toString())){
						IndexedWord neg1 = null;
					  IndexedWord neg2 = null;
						for(TypedDependency dependency3: dependencyList){
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb1)==0)
								neg1=dependency3.dep();
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb2)==0)
								neg2=dependency3.dep();
						}
						log.info("#########################");
						log.info("###Found Knowledge###");
						log.info("#File Name [ "+currentFile.getName()+" ] #");
					  log.info("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  System.out.println("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  if(neg1 != null && neg2 != null){
							log.info(neg1+" "+verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println(neg1+" "+verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println("not verb 1 dobj = not verb 2 dobj");
							log.info("not verb 1 dobj = not verb 2 dobj");
						}
						else if(neg1 == null && neg2 == null){
							log.info(verb1+" "+nsubjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println(verb1+" "+nsubjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println("verb 1 dobj = verb 2 dobj");
							log.info("verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 != null && neg2 == null){
							log.info(neg1+" "+verb1+" "+nsubjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println(neg1+" "+verb1+" "+nsubjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println("not verb 1 dobj = verb 2 dobj");
							log.info("not verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 == null && neg2 != null){
							log.info(verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println(verb1+" "+nsubjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println("verb 1 dobj = not verb 2 dobj");
							log.info("verb 1 dobj = not verb 2 dobj");
						}
					  log.info("#########################");
					}
					// if dobj(verb1) == dobj(verb2), then print it
					if(dobjverb1 != null && dobjverb2!=null && dobjverb1.toString().equals((dobjverb2.toString()))){
						IndexedWord neg1 = null;
					  IndexedWord neg2 = null;
						for(TypedDependency dependency3: dependencyList){
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb1)==0)
								neg1=dependency3.dep();
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb2)==0)
								neg2=dependency3.dep();
						}
						log.info("#########################");
						log.info("###Found Knowledge###");
						log.info("#File Name [ "+currentFile.getName()+" ] #");
					  log.info("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  System.out.println("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
						if(neg1 != null && neg2 != null){
							log.info(neg1+" "+verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println(neg1+" "+verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println("not verb 1 dobj = not verb 2 dobj");
							log.info("not verb 1 dobj = not verb 2 dobj");
						}
						else if(neg1 == null && neg2 == null){
							log.info(verb1+" "+dobjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println(verb1+" "+dobjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println("verb 1 dobj = verb 2 dobj");
							log.info("verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 != null && neg2 == null){
							log.info(neg1+" "+verb1+" "+dobjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println(neg1+" "+verb1+" "+dobjverb1+" => "+verb2+" "+" "+dobjverb2);
							System.out.println("not verb 1 dobj = verb 2 dobj");
							log.info("not verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 == null && neg2 != null){
							log.info(verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println(verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+dobjverb2);
							System.out.println("verb 1 dobj = not verb 2 dobj");
							log.info("verb 1 dobj = not verb 2 dobj");
						}
						log.info("#########################");
					}
					// if dobj(verb1) == nsubj(verb2), then print it
					if(dobjverb1 != null && nsubjverb2!=null && dobjverb1.toString().equals(nsubjverb2.toString())){
						IndexedWord neg1 = null;
					  IndexedWord neg2 = null;
						for(TypedDependency dependency3: dependencyList){
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb1)==0)
								neg1=dependency3.dep();
							if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb2)==0)
								neg2=dependency3.dep();
						}
						log.info("#########################");
						log.info("###Found Knowledge###");
						log.info("#File Name [ "+currentFile.getName()+" ] #");
					  log.info("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  System.out.println("#Sentence that gave knowledge [ "+t.flatten()+" ]#");
					  if(neg1 != null && neg2 != null){
							log.info(neg1+" "+verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println(neg1+" "+verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println("not verb 1 dobj = not verb 2 dobj");
							log.info("not verb 1 dobj = not verb 2 dobj");
						}
						else if(neg1 == null && neg2 == null){
							log.info(verb1+" "+dobjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println(verb1+" "+dobjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println("verb 1 dobj = verb 2 dobj");
							log.info("verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 != null && neg2 == null){
							log.info(neg1+" "+verb1+" "+dobjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println(neg1+" "+verb1+" "+dobjverb1+" => "+verb2+" "+" "+nsubjverb2);
							System.out.println("not verb 1 dobj = verb 2 dobj");
							log.info("not verb 1 dobj = verb 2 dobj");
						}
						else if(neg1 == null && neg2 != null){
							log.info(verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println(verb1+" "+dobjverb1+" => "+neg2+" "+verb2+" "+" "+nsubjverb2);
							System.out.println("verb 1 dobj = not verb 2 nsubj");
							log.info("verb 1 dobj = not verb 2 nsubj");
						}
						log.info("#########################");
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
		// TODO: use debug for logging
		//log.debug("###Returned Typed Dependency List###");
		return tdl;
		// TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
				// tp.printTree(parse);
		
	}
	
}