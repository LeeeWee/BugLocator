package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map.Entry;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class SourceCodeReositoryTest {
	
	public static void main(String[] args) throws Exception {
		String output = "D:\\data\\working\\test\\sourceCodeRepositoryTest.txt";
//		String output = "/Users/liwei/Documents/defect-prediction/working/SourceCodeTest.txt";
		
		String[] products = {"swt", "aspectj", "eclipse"};
		String[] bugFilePaths = {"D:\\data\\buglocalization\\BRTracer\\Dataset\\SWTBugRepository.xml",
								"D:\\data\\buglocalization\\BRTracer\\Dataset\\AspectJBugRepository.xml",
								"D:\\data\\buglocalization\\BRTracer\\Dataset\\EclipseBugRepository.xml"};
		String[] sourceCodeDirs = {"D:\\data\\buglocalization\\BRTracer\\Dataset\\swt-3.1\\src",
									"D:\\data\\buglocalization\\BRTracer\\Dataset\\aspectj",
									"D:\\data\\buglocalization\\BRTracer\\Dataset\\eclipse-3.1\\plugins"};
		
//		for (int index = 0; index < products.length; index++) {
		int index = 0;
			Property property = Property.loadInstance("test_" + products[index]);

			
			property.setProduct(products[index]);
			property.setBugFilePath(bugFilePaths[index]);
			property.setSourceCodeDir(sourceCodeDirs[index]);

			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			if (property.getProduct().equals("swt") || property.getProduct().equals("eclipse"))
				VSMRankTestOnBRTracerData.modifyFilesPath(brRepo, codeRepo);
			
			SourceCodeTfidfVectorizer codeTfidfVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeTfidfVectorizer.train();
			codeTfidfVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
//		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
//		int count = 0;
		for (Entry<String, SourceCode> entry : codeRepo.getSourceCodeMap().entrySet()) {
//			count++;
//			if (count == 100) 
//				break;
			StringBuilder builder = new StringBuilder();
			SourceCode sourceCode = entry.getValue();
			builder.append("Path: " + entry.getKey() + "\n");
			builder.append("FullClassName: " + sourceCode.getFullClassName() + "\n");
			builder.append("Content: " + sourceCode.getSourceCodeCorpus().getContent() + "\n");
			builder.append("LengthScore: " + sourceCode.getLengthScore() + "\n");
			builder.append("ChangeHistory: ");
			for (Long point : sourceCode.getChangeHistory()) {
				builder.append(point + " ");
			}
			builder.append("\nContentNorm: " + sourceCode.getSourceCodeCorpus().getContentNorm() + "\n");
			builder.append("ContentTokens: ");
			for (TokenScore tokenScore : sourceCode.getSourceCodeCorpus().getContentTokens().values()) {
				builder.append(String.format("%s(%f,%f,%f) ", tokenScore.getToken(), tokenScore.getTf(),
						tokenScore.getIdf(), tokenScore.getTokenWeight()));
			}
			builder.append("\nMethods: " + "\n");
			for (Method method : sourceCode.getMethodList()) {
				builder.append("\tMethodName: " + method.getName() + "\tParams: " + 
						method.getParams() + "\tReturnType: " + method.getReturnType() + "\n");
				builder.append("\t\tMethodContent: " + method.getContent() + "\n");
			}
			writer.write(builder.toString());
			writer.write("\n");
		}
		writer.close();
	}
	
}
