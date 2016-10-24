package test;

import java.util.List;

import com.commonSense.DependencyExtractor;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

public class testDependencyExtractor {
public static void main(String[] args) {

		
		DependencyExtractor dependencyExtractor = new DependencyExtractor();
		//dependencyExtractor.parser = LexicalizedParser.loadModel(PCG_MODEL);
		//List<Tree> parsedTrees = dependencyExtractor.parseFromFile(dependencyExtractor.parser, "/home/faizi/Documents/nlp/project/CommonSense/inputs/face-to-face/charlotte/");
		
		dependencyExtractor.startParsing();
		//System.out.println(parsedTrees);
	}
}
