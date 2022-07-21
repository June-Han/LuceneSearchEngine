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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;
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

public class LuceneFacetedIndex {
	
	//Facets Config
	static final FacetsConfig facetconfig = new FacetsConfig();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Input folder
	    String docsPath = "inputFiles";
	     
	    //Output folder
	    String indexPath = "facetedIndex";
	    
	    String taxoPath = "taxonomy";
	 
        //Input Path Variable
        final Path docDir = Paths.get(docsPath);
 
        try
        {
            //org.apache.lucene.store.Directory instance
	        Directory dir = FSDirectory.open( Paths.get(indexPath) );
	         
	        //analyzer with the default stop words (default stop words,tokenising and stemming 
	        //Analyzer analyzer = new StandardAnalyzer();
	        //EnglishPossessiveFilter, KeywordMarkerFilter, PorterStemFilter
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
                        indexDoc(writer, file, taxoWriter, attrs.lastModifiedTime().toMillis());
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
    	facetconfig.setHierarchical("Entry Date", true);
    	
        try (InputStream stream = Files.newInputStream(file))
        {
        	//read the input file line by line
            //int counter = 0;
            try (BufferedReader bufferReader = new BufferedReader(new InputStreamReader(stream))) {
            	for (String line; (line = bufferReader.readLine()) != null;) {
	            	System.out.println(line);
	            	
	            	//Split back into format from text file
	            	List<String> docList = Arrays.asList(line.trim().split("	", 5));
	            	
	            	for (String docLine: docList) {
	            		System.out.println("lines : "+ docLine);
	            	}
	            	//Format the type to check book or author
	            	List<String> aboutType = Arrays.asList(docList.get(1).trim().split("/")); //2 backslashes, have space at front
	            	
	            	for (String aboutLine: aboutType) {
	            		System.out.println(aboutLine);
	            	}
	            	
	            	//Formatting the details
	            	String details = docList.get(4);
	            	
	            	//JSONIFY the details
	            	System.out.println("JSON Object");
	            	boolean jsonCheck = true;
	            	JSONObject JSONDetails = null;
	            	try {
		            	JSONDetails = new JSONObject(details);
		            	System.out.println(JSONDetails.toString());
	            	}
	            	catch (Exception ignored) {
	            		jsonCheck = false;
	            	}
	            	
	            	//Converting dates
	            	LocalDate entryDate = LocalDate.parse(docList.get(3).substring(0,10), DateTimeFormatter.ISO_DATE);
	            	System.out.println(entryDate);
	            	
	            	/*
	            	 * Simple Date Format
	            	try {
						Date simpledate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(docList.get(3).substring(0,22));
						String outputStr = new SimpleDateFormat("dd-MM HH:mm").format(simpledate);
						System.out.println(outputStr);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            	System.out.println();
	            	*/
	            	
	            	details = details.replaceAll("\"", "").replaceAll("\\}", "").replaceAll("\\{", ""); //remove apostrophes and brackets
	            	System.out.println(details);
	            	
	            	//Create details List
	            	List<String> detailsList = Arrays.asList(details.trim().split(", "));
	            	/*for (String detailLine: detailsList) {
	            		System.out.println(detailLine);
	            	}*/
	            	
	            	//Create lucene document
	            	Document doc = new Document();
	            	
	            	doc.add(new StringField("path", file.toString(), Field.Store.YES));
	            	doc.add(new LongPoint("modified", lastModified));
	            	
	            	//Store raw of each line for search
            		doc.add(new TextField("contents", line, Store.NO));
            		
            		//Store document type
            		doc.add(new StringField("datatype", docList.get(1), Field.Store.YES));
            		
            		//Store 3 details for display
            		doc.add(new TextField("detail1", detailsList.get(0), Field.Store.YES));
            		doc.add(new TextField("detail2", detailsList.get(1), Field.Store.YES));
            		doc.add(new TextField("detail3", detailsList.get(2), Field.Store.YES));
            		
            		//Store original detail for duplicates
            		doc.add(new TextField("details", docList.get(4), Field.Store.NO));
            		
            		//Adding in Facets for faceted search
            		//Add in Author Facets
            		if (aboutType.size() > 1 && jsonCheck == true) {
	            		if (aboutType.get(1).equalsIgnoreCase("authors")) {
	            			if (JSONDetails.has("name")) {
	            				String nameString = JSONDetails.get("name").toString();
	            				
	            				//Checking for null entry
	            				if (nameString == null || nameString.isEmpty() || nameString.trim().isEmpty()){
	            					//Add in detail 4 index author
	    	            			doc.add(new TextField("detail4", "No Author Specified", Field.Store.YES));
	    	
	    		            		doc.add(new FacetField("Author/Title", "No Author Specified"));
	            					
	            				}
	            				else {
	            					//Add in detail 4 index author
	    	            			doc.add(new TextField("detail4", JSONDetails.get("name").toString(), Field.Store.YES));
	    	
	    		            		doc.add(new FacetField("Author/Title", JSONDetails.get("name").toString()+""));
	            				}
		            			
	            			}
	            			else {
	            				//Add in detail 4 index author
		            			doc.add(new TextField("detail4", "No Author Specified", Field.Store.YES));
		
			            		doc.add(new FacetField("Author/Title", "No Author Specified"));
	            			}
		            		//Add in Data Entry Date 
		            		doc.add(new FacetField("Entry Date", String.format("%d",entryDate.getYear()), 
		            				String.format("%d",entryDate.getMonthValue()), String.format("%d",entryDate.getDayOfMonth())));
		            	}
	            		//Add in book title facets
	            		else if (aboutType.get(1).equalsIgnoreCase("books")) {
	            			
	            			//Add in facets
	            			if (JSONDetails.has("title")) {
	            				
	            				String titleString = JSONDetails.get("title").toString();
	            				
	            				//Checking for null entries
	                    		if (titleString == null || titleString.isEmpty() || titleString.trim().isEmpty()){
	                    			//Add in detail 4 index title
	                    			doc.add(new TextField("detail4", "Book Edition", Field.Store.YES));
	                    			
	                				doc.add(new FacetField("Author/Title", "Book Edition"));
		            				
	                    		}
	                    		else {
	                    			//Add in detail 4 index title
		                			doc.add(new TextField("detail4", JSONDetails.get("title").toString(), Field.Store.YES));
		                			
		            				doc.add(new FacetField("Author/Title", JSONDetails.get("title").toString()));
	                    		}
	            			}
	            			else {
	            				//Add in detail 4 index title
	                			doc.add(new TextField("detail4", "Book Edition", Field.Store.YES));
	                			
	            				doc.add(new FacetField("Author/Title", "Book Edition"));
	            			}
	            			
	            			/*
	            			if (JSONDetails.has("publish_date")) {
	            				//LocalDate publishDate = LocalDate.parse(JSONDetails.get("publish_date").toString().substring(0,4), DateTimeFormatter.ISO_DATE);
	        	            	.aboutType..out.println(publishDate);
	            				doc.add(new FacetField("Entry Date", String.format("%d",JSONDetails.get("publish_date").toString()), 
	            						String.format("%d",1) ,String.format("%d",1)));
	            			}
	            			else {
	            				doc.add(new FacetField("Entry Date", String.format("%d",entryDate.getYear()), 
	    	            				String.format("%d",entryDate.getMonthValue()), String.format("%d",entryDate.getDayOfMonth())));
	            			}*/
	            			doc.add(new FacetField("Entry Date", String.format("%d",entryDate.getYear()), 
		            				String.format("%d",entryDate.getMonthValue()), String.format("%d",entryDate.getDayOfMonth())));
	            			
	            		}
            		}
            		else {
            			doc.add(new TextField("detail4", "null", Field.Store.YES));
            			
        				doc.add(new FacetField("Author/Title", "null"));
        				
        				doc.add(new FacetField("Entry Date", String.format("%d",entryDate.getYear()), 
	            				String.format("%d",entryDate.getMonthValue()), String.format("%d",entryDate.getDayOfMonth())));
            		}
            		
            		
            		writer.updateDocument(new Term("details", docList.get(4)), facetconfig.build(taxoWriter, doc));
            	
	            	System.out.println();
	            	//counter ++;
            	}
            }
        }
        catch(Exception e) {
        	//Closing the writers for index wrapping and usage
        	writer.close();
        	taxoWriter.close();
        	e.printStackTrace();
        }
    }

}
