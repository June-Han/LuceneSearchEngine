package lucene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class termQueryIndexFile {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Input folder
	    String docsPath = "inputFiles";
	     
	    //Output folder
	    String indexPath = "indexedFiles";
	 
        //Input Path Variable
        final Path docDir = Paths.get(docsPath);
 
        try
        {
            //org.apache.lucene.store.Directory instance
	        Directory dir = FSDirectory.open( Paths.get(indexPath) );
	         
	        //analyzer with the default stop words
	        Analyzer analyzer = new StandardAnalyzer();
	         
	        //IndexWriter Configuration
	        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
	        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
	         
	        //IndexWriter writes new index files to the directory
	        IndexWriter writer = new IndexWriter(dir, iwc);
         
	        //Its recursive method to iterate all files and directories
            indexDocs(writer, docDir);
 
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
	}
	
	static void indexDocs(final IndexWriter writer, Path path) throws IOException
    {
        //Check if the input path is a directory
        if (Files.isDirectory(path))
        {
            //Iterate directory if the input folder is a directory
            Files.walkFileTree(path, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    try
                    {
                        //Index this file
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        else
        {
            //Index this file
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }
 
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException
    {   
        try (InputStream stream = Files.newInputStream(file))
        {
            
        	//read the input file line by line
            try (BufferedReader bufferReader = new BufferedReader(new InputStreamReader(stream))) {
            	for (String line; (line = bufferReader.readLine()) != null;) {
            		
            		//Store for each document to return to interface
            		line = line.replaceAll("\"", "");
            		if (line.trim() != "" || line != null) //There is still empty lines going in (extra indexes)
            		{
            			//Create lucene Document
                        Document doc = new Document();
                         
                        doc.add(new StringField("path", file.toString(), Field.Store.YES));
                        doc.add(new LongPoint("modified", lastModified));
                        
            			//Store raw of each line for search
                		doc.add(new TextField("contents", line, Store.YES));
                		
                		List<String> docList = Arrays.asList(line.trim().split(",", 3));
                		
                		doc.add(new StringField("link", docList.get(0), Field.Store.YES));
                		
            			//doc.add(new StringField("about", docList.get(1), Field.Store.YES)); //empty lines return error as no 2nd element
            			
            			
						//	 Updates a document by first deleting the document(s) containing Term and then 
						//	 adding the new document.  The delete and then add are atomic as seen
						//	 by a reader on the same index.
						//	 writer.addDocument(doc); //Adds repetitive documents (not atomic)
                		writer.updateDocument(new Term("link", docList.get(0)), doc);
                		
                		/*
                		//For seeing the data processing
                		System.out.println("Content : " + line);
                		for (String docLine: docList) {
                			System.out.println("lines : " + docLine);
                		}
                		System.out.println();
                		*/
                		
            		}
            	}
            }
        }
    }

}
