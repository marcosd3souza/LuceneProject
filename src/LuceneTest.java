import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.EnglishStemmer;


public class LuceneTest 
{
	public static void main(String[] args)
	{
		try
		{
			// bases
			String stopwordStemmingPath = "/home/marcos/Documentos/UFPE/Recuperação Inteligente de Informação/documentos/base-index/stopword-stemming";
			String stopwordPath = "/home/marcos/Documentos/UFPE/Recuperação Inteligente de Informação/documentos/base-index/stopword";
			String stemmingPath = "/home/marcos/Documentos/UFPE/Recuperação Inteligente de Informação/documentos/base-index/stemming";
			String docsOriginPath = "/home/marcos/Documentos/UFPE/Recuperação Inteligente de Informação/documentos/base-index/origin";
			String docsOrigin = "/home/marcos/Documentos/UFPE/Recuperação Inteligente de Informação/documentos/base-index/ORIGINAL";
			
			//this directory will contain the indexes
		    Directory indexDirectory = null;
		    
			StopwordAnalyzerBase analyzer = null;
			//	Specify the analyzer for tokenizing text.
		    //	The same analyzer should be used for indexing and searching
			
			switch (Integer.parseInt(args[0])) {
			case 1:
				
				//origin
				analyzer = new StandardAnalyzer(new CharArraySet(0, true));
				indexDirectory = FSDirectory.open(Paths.get(docsOriginPath));
				break;
				
			case 2:

				//stopword
				analyzer = new StandardAnalyzer();
				indexDirectory = FSDirectory.open(Paths.get(stopwordPath));
				break;
				
			case 3:
				
				//stemming
				analyzer = new EnglishAnalyzer(new CharArraySet(0, true));
				indexDirectory = FSDirectory.open(Paths.get(stemmingPath));
				break;
				
			case 4:
				
				//stemming-stopword
				analyzer = new EnglishAnalyzer();
				indexDirectory = FSDirectory.open(Paths.get(stopwordStemmingPath));
				break;
			
			}
			
			if (Integer.parseInt(args[1])==1) {
				
			Indexer indexer = new Indexer(indexDirectory, analyzer);
			int docs = indexer.createIndex(docsOrigin);
			indexer.close();
			
			System.out.println("docs indexados: "+docs);
			}

			//	Text to search
			// "\"a solar cells\" OR spectrum";
			String querystr = args[2];
			
			//remove stopword from query
			if (Integer.parseInt(args[3]) == 1 || Integer.parseInt(args[3]) == 3) {
				List<CharArraySet> stopList = Arrays.asList(new EnglishAnalyzer().getStopwordSet());
				String query = querystr;
				for(CharArraySet stop : stopList){
					
					for(String word : query.split("\\s|\"")){
						
						if(stop.contains(word)){
						query = query.replaceAll(word, "");	
						}
					}
				}
				
				querystr = query;
			}
			
			//Stem the query
			if (Integer.parseInt(args[3]) == 2 || Integer.parseInt(args[3]) == 3) {
				
				EnglishStemmer stemmer = new EnglishStemmer();
				String queryStemmed = "";
				for (String word : Arrays.asList(querystr.toLowerCase().split("\\s+"))) {
					
					stemmer.setCurrent(word);
					stemmer.stem();
					queryStemmed += " "+stemmer.getCurrent();
					
				}; 
				querystr = queryStemmed;
			}
			
			if (Integer.parseInt(args[0]) == 2 || Integer.parseInt(args[0]) == 4) {
				analyzer.tokenStream("content", querystr);
				
			}
			
			System.out.println("query: "+querystr);
			
			//	The \"title\" arg specifies the default field to use when no field is explicitly specified in the query
			Query q = new QueryParser("content", analyzer).parse(querystr);
			
			// Searching code
			int hitsPerPage = 200;
		    IndexReader reader = DirectoryReader.open(indexDirectory);
		    IndexSearcher searcher = new IndexSearcher(reader);
		    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
		    searcher.search(q, collector);
		    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		    
		    //	Code to display the results of search
		    System.out.println("Found " + hits.length + " hits.");
		    for(int i=0;i<hits.length;++i) 
		    {
		      int docId = hits[i].doc;
		      Document d = searcher.doc(docId);
		      System.out.println((i + 1) + ". " + d.get("name"));
		    }
		    
		    // reader can only be closed when there is no need to access the documents any more
		    reader.close();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	private static void addDoc(IndexWriter w, String title, String isbn) throws IOException 
	{
		  Document doc = new Document();
		  // A text field will be tokenized
		  doc.add(new TextField("title", title, Field.Store.YES));
		  // We use a string field for isbn because we don\'t want it tokenized
		  doc.add(new StringField("isbn", isbn, Field.Store.YES));
		  w.addDocument(doc);
	}
}