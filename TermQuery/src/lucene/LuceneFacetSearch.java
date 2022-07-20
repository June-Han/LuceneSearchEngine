package lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

//This is added with faceting on top of the termQueryReadFile
public class LuceneFacetSearch {

	//directory contains the lucene indexes
    private static final String INDEX_DIR = "facetindex";
    private static final String TAXO_DIR = "taxonomy";
    //Facets config
    static final FacetsConfig facetconfig = new FacetsConfig();
 
    public static void main(String[] args) throws Exception
    {
        //Create lucene searcher. It search over a single IndexReader.
        IndexSearcher searcher = createSearcher();
        
        //Taxonomy reader to read facets
        Directory TaxoDir = FSDirectory.open(Paths.get(TAXO_DIR));
        
        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(TaxoDir);
        
        //FacetsCollector fc = new FacetsCollector();
        
        //Let's just browse facets :D
        //FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, fc);
         
        //Search in facets
        List<FacetResult> facetresults = facetSearch("alfred the great", searcher, taxoReader);
        //Search indexed contents using search term
        TopDocs foundDocs = searchInContent("alfred the great", searcher);
         
        //Total found documents
        System.out.println("Total Results :: " + foundDocs.totalHits);
         
        //Let's print out the path of files which have searched term
        for (ScoreDoc sd : foundDocs.scoreDocs)
        {
            Document d = searcher.doc(sd.doc);
            System.out.println("Path : "+ d.get("path") + ", Score : " + sd.score);
            System.out.println("link : " + d.get("link"));
            System.out.println("Contents: " + d.get("contents")); //get the contents from the index
            System.out.println();
        }
        
        System.out.println("Facet Results");
        System.out.println("Link: " + facetresults.get(0));
        //System.out.println("Contents: " + facetresults.get(1));
    }
    
    private static List<FacetResult> facetSearch(String textToFind,IndexSearcher searcher, TaxonomyReader taxoReader) throws Exception
    {
    	FacetsCollector fc = new FacetsCollector();
    	//Create search query
        QueryParser qp = new QueryParser("contents", new EnglishAnalyzer());
        Query query = qp.parse(textToFind);
        
        //search the facets
        FacetsCollector.search(searcher, query, 10, fc);
        
        // Retrieve results
        List<FacetResult> results = new ArrayList<>();
        
        // Count both "Publish Date" and "Author" dimensions
        Facets facets = new FastTaxonomyFacetCounts(taxoReader, facetconfig, fc);
        results.add(facets.getTopChildren(10, "Link"));
        //results.add(facets.getTopChildren(10, "Contents"));
        
        taxoReader.close();
        return results;
    }
     
    private static TopDocs searchInContent(String textToFind, IndexSearcher searcher) throws Exception
    {
        //Create search query
        QueryParser qp = new QueryParser("contents", new EnglishAnalyzer());
        Query query = qp.parse(textToFind);
         
        //search the index
        TopDocs hits = searcher.search(query, 10);
        return hits;
    }
 
    private static IndexSearcher createSearcher() throws IOException
    {
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
         
        //It is an interface for accessing a point-in-time view of a lucene index
        IndexReader reader = DirectoryReader.open(dir);
         
        //Index searcher
        IndexSearcher searcher = new IndexSearcher(reader);
        return searcher;
    }

}