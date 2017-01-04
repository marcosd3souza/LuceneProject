import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

public class Indexer {

   private IndexWriter writer;

   public Indexer(Directory indexDirectory, Analyzer analyzer) throws IOException{
           
      
      //create the indexer
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      writer = new IndexWriter(indexDirectory, config);
    
   }

   public void close() throws CorruptIndexException, IOException{
      writer.close();
   }

   private Document getDocument(File file) throws IOException{
	   
	  Document document = new Document();
	   
	  String content = ""; 
	  try {
		    PDDocument pdfDoc = null;
		    pdfDoc = PDDocument.load(file);
		    pdfDoc.getClass();
		    if (!pdfDoc.isEncrypted()) {
		        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
		        stripper.setSortByPosition(true);
		        PDFTextStripper Tstripper = new PDFTextStripper();
		        content = Tstripper.getText(pdfDoc);
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
      
      document.add(new TextField("content", content, Field.Store.YES));
      
      document.add(new StringField("name", file.getName(), Field.Store.YES));	 

      return document;
   }   

   private void indexFile(File file) throws IOException{
      System.out.println("Indexing "+file.getCanonicalPath());
      writer.addDocument(getDocument(file));
   }

   public int createIndex(String dataDirPath) 
      throws IOException{
      //get all files in the data directory
      File[] files = new File(dataDirPath).listFiles();

      for (File file : files) {
         if(!file.isDirectory()
            && !file.isHidden()
            && file.exists()
            && file.canRead()
         ){
            indexFile(file);
         }
      }
      return writer.numDocs();
   }
}
