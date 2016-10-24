package com.commonSense;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
	static long count = 0;
	TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	Properties prop = null;
	String PCG_MODEL = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";

	private final TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(),
			"invertible=true");
	public LexicalizedParser parser = null;
	private List<HasWord> currentSentence;
	
	public Tree parse(String str) {
		List<CoreLabel> tokens = tokenize(str);
		Tree tree = parser.apply(tokens);
		return tree;
	}
	
	public void parseFromFile(LexicalizedParser lp, File currentFile) {
		List<Tree> result = new ArrayList<Tree>();
		for(List<HasWord> sentence : new DocumentPreprocessor(currentFile.getAbsolutePath())) {
			currentSentence = sentence;
			Tree tree = lp.apply(sentence);
			getKnowledgeFromTree(currentFile, tree);
		}
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
			parseFromFile(parser, currentFile);
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
						printOutput(currentFile, dependencyList, verb1, verb2, nsubjverb1, nsubjverb2, "nsubj",  "nsubj");
					}
					// if nsubj(verb1) == dobj(verb2), then print it
					if(nsubjverb1 != null && dobjverb2 !=null && nsubjverb1.toString().equals(dobjverb2.toString())){
						printOutput(currentFile, dependencyList, verb1, verb2, nsubjverb1, dobjverb2, "nsubj",  "dobj");
					}
					// if dobj(verb1) == dobj(verb2), then print it
					if(dobjverb1 != null && dobjverb2!=null && dobjverb1.toString().equals((dobjverb2.toString()))){
						printOutput(currentFile, dependencyList, verb1, verb2, dobjverb1, dobjverb2, "dobj",  "dobj");
					}
					// if dobj(verb1) == nsubj(verb2), then print it
					if(dobjverb1 != null && nsubjverb2!=null && dobjverb1.toString().equals(nsubjverb2.toString())){
						printOutput(currentFile, dependencyList, verb1, verb2, dobjverb1, nsubjverb2, "dobj",  "nsubj");
					}
					
				}
			}
		}
	}

	/**
	 * @param currentFile
	 * @param t
	 * @param dependencyList
	 * @param verb1
	 * @param verb2
	 * @param nsubjverb1
	 * @param nsubjverb2
	 */
	private void printOutput(	File currentFile,
														List<TypedDependency> dependencyList,
														IndexedWord verb1, IndexedWord verb2,
														IndexedWord nsubjverb1, IndexedWord nsubjverb2,
														String verb1GramRel, String verb2GramRel) {
		// found 1 knowledge, increment count;
		count++;
		IndexedWord neg1 = null;
		IndexedWord neg2 = null;
		//get negations for verb1 and verb2 if any
		for(TypedDependency dependency3: dependencyList){
			if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb1)==0)
				neg1=dependency3.dep();
			if(dependency3.reln().getShortName().equals("neg") && dependency3.gov().compareTo(verb2)==0)
				neg2=dependency3.dep();
		}
		// compare subject objects of both verbs and print output
		log.info("#########################");
		log.info("###Found Knowledge###");
		log.info("#File Name [ "+currentFile.getName()+" ] #");
		String sentenceinString = extractSentence(currentSentence);
		log.info(count+". Sentence that gave knowledge [ "+sentenceinString+" ]#");
		System.out.println(count+". Sentence that gave knowledge [ "+sentenceinString+" ]#");
		if(neg1 != null && neg2 != null){
			log.info(neg1.word()+" "+verb1.word()+" "+nsubjverb1.word()+" => "+neg2.word()+" "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println(neg1.word()+" "+verb1.word()+" "+nsubjverb1.word()+" => "+neg2.word()+" "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println("not verb 1 "+verb1GramRel+" = not verb 2 "+verb2GramRel);
			log.info("not verb 1 "+verb1GramRel+" = not verb 2 "+verb2GramRel);
		}
		else if(neg1 == null && neg2 == null){
			log.info(verb1.word()+" "+nsubjverb1.word()+" => "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println(verb1.word()+" "+nsubjverb1.word()+" => "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println("verb 1 "+verb1GramRel+" = verb 2 "+verb2GramRel);
			log.info("verb 1 "+verb1GramRel+" = verb 2 "+verb2GramRel);
		}
		else if(neg1 != null && neg2 == null){
			log.info(neg1.word()+" "+verb1.word()+" "+nsubjverb1.word()+" => "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println(neg1.word()+" "+verb1.word()+" "+nsubjverb1.word()+" => "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println("not verb 1 "+verb1GramRel+" = verb 2 "+verb2GramRel);
			log.info("not verb 1 "+verb1GramRel+" = verb 2 "+verb2GramRel);
		}
		else if(neg1 == null && neg2 != null){
			log.info(verb1.word()+" "+nsubjverb1.word()+" => "+neg2.word()+" "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println(verb1.word()+" "+nsubjverb1.word()+" => "+neg2.word()+" "+verb2.word()+" "+" "+nsubjverb2.word());
			System.out.println("verb 1 "+verb1GramRel+" = not verb 2 "+verb2GramRel);
			log.info("verb 1 "+verb1GramRel+" = not verb 2 "+verb2GramRel);
		}
		log.info("##### knowledge count: "+count+"#####");
		log.info("#########################");
	}

	/**
	 * @return
	 */
	private static String extractSentence(List<HasWord> currentSentence) {
		String sentenceinString = "";
		for(HasWord w: currentSentence){
			sentenceinString = sentenceinString+w.word() + " ";
		}
		return sentenceinString;
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