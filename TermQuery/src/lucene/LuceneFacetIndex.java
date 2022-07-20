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
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

//This is added with faceting on top the termQueryIndexFile
public class LuceneFacetIndex {
	//Facets config
    static final FacetsConfig facetconfig = new FacetsConfig();
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Input folder
	    String docsPath = "inputFiles";
	     
	    //Output folder
	    String indexPath = "facetindex";
	 
        //Input Path Variable
        final Path docDir = Paths.get(docsPath);
        
        String taxoPath = "taxonomy";
    	
    	
 
        try
        {
            //org.apache.lucene.store.Directory instance
	        Directory dir = FSDirectory.open( Paths.get(indexPath) );
	         
	        //English analyzer with the default stop words and Porter stemming
	        Analyzer analyzer = new EnglishAnalyzer();
	         
	        //IndexWriter Configuration
	        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
	        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
	        
	        //org.apache.lucene.store.Directory instance
	        Directory taxoDir = FSDirectory.open( Paths.get(taxoPath) );
	        
	    	//Create and open a taxonomy writer
	    	TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir, OpenMode.CREATE_OR_APPEND);
	         
	        //IndexWriter writes new index files to the directory
	        IndexWriter writer = new IndexWriter(dir, iwc);
         
	        //Its recursive method to iterate all files and directories
            indexDocs(writer, docDir, taxoWriter);
            taxoWriter.close();
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
	}
	
	static void indexDocs(final IndexWriter writer, Path path, TaxonomyWriter taxoWriter) throws IOException
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
                        indexDoc(writer, file,taxoWriter, attrs.lastModifiedTime().toMillis());
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
            indexDoc(writer, path, taxoWriter, Files.getLastModifiedTime(path).toMillis());
        }
    }

    static void indexDoc(IndexWriter writer, Path file, TaxonomyWriter taxoWriter, long lastModified) throws IOException
    {   
    	//Specify the facets for the current document

        try (InputStream stream = Files.newInputStream(file))
        {
            
        	//read the input file line by line
            try (BufferedReader bufferReader = new BufferedReader(new InputStreamReader(stream))) {
            	for (String line; (line = bufferReader.readLine()) != null;) {
            		
            		//Store for each document to return to interface
            		line = line.replaceAll("\"", "");
            		if (line != null && !line.isEmpty()) //There is still empty lines going in (extra indexes)
            		{
            			//Create lucene Document
                        Document doc = new Document();
                         
                        doc.add(new StringField("path", file.toString(), Field.Store.YES));
                        doc.add(new LongPoint("modified", lastModified));
                        
            			//Store raw of each line for search
                		doc.add(new TextField("contents", line, Store.NO));
                		
                		List<String> docList = Arrays.asList(line.trim().split(",", 3));
                		
                		doc.add(new StringField("link", docList.get(0), Field.Store.YES));
                		
            			//doc.add(new StringField("about", docList.get(1), Field.Store.YES)); //empty lines return error as no 2nd element
            			
                		//Adding FacetFields 
                		//if (!line.isEmpty()) {
                		doc.add(new FacetField("Link", docList.get(0)));
                		//}
                		
            			
						//	 Updates a document by first deleting the document(s) containing Term and then 
						//	 adding the new document.  The delete and then add are atomic as seen
						//	 by a reader on the same index.
						//	 writer.addDocument(doc); //Adds repetitive documents (not atomic)
                		writer.updateDocument(new Term("link", docList.get(0)), facetconfig.build(taxoWriter, doc));
                		
                		
                		//For seeing the data processing
                		System.out.println("Content : " + line);
                		for (String docLine: docList) {
                			System.out.println("lines : " + docLine);
                		}
                		System.out.println();
                		
                		
            		}
            	}
            }
        }
    }
}
