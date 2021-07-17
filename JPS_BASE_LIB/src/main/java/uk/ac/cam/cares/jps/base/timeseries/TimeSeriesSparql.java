package uk.ac.cam.cares.jps.base.timeseries;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignment;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ModifyQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.SubSelect;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.json.JSONArray;

import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

/**
 * This class contains a collection of methods to interact with kb.
 * @author Kok Foong Lee
 *
 */

public class TimeSeriesSparql {
	// kbClient with the endpoint (triplestore/owl file) specified
	private KnowledgeBaseClientInterface kbClient = null; 
	
	// Namespaces for ontology and kb
	public static final String ns_ontology = "http://www.theworldavatar.com/ontology/ontotimeseries/OntoTimeSeries.owl#";
	public static final String ns_kb = "http://www.theworldavatar.com/kb/ontotimeseries/";
	
	// Prefixes
	private static final Prefix prefix_ontology = SparqlBuilder.prefix(iri(ns_ontology));
	private static final Prefix prefix_kb = SparqlBuilder.prefix(iri(ns_kb));
	
	// RDF type
	private static final String TimeSeriesString = ns_ontology + "TimeSeries";
    private static final Iri TimeSeries = iri(TimeSeriesString);
    
    // Relationships
    private static final Iri hasTimeSeries = prefix_ontology.iri("hasTimeSeries");
    private static final Iri hasRDB = prefix_ontology.iri("hasRDB");
    private static final Iri hasTimeUnit = prefix_ontology.iri("hasTimeUnit");
    
    /**
     * Standard constructor
     * @param kbClient
     */
    public TimeSeriesSparql(KnowledgeBaseClientInterface kbClient) {
    	this.kbClient = kbClient;
    }
    
	public void setKBClient(KnowledgeBaseClientInterface kbClient) {
        this.kbClient = kbClient;
	}
    
	/**
	 * Check whether a particular time series (i.e. tsIRI) exists
	 * @param timeSeriesIRI
	 * @return
	 */
    public boolean checkTimeSeriesExists(String timeSeriesIRI) {
    	String query = String.format("ask {<%s> a <%s>}", timeSeriesIRI, TimeSeriesString);
    	kbClient.setQuery(query);
    	boolean timeSeriesExists = kbClient.executeQuery().getJSONObject(0).getBoolean("ASK");
    	return timeSeriesExists;
    }
    
    /**
     * Instantiate the time series instance (time unit is optional)
     * @param timeSeriesIRI
     * @param dataIRI
     * @param dbURL
     * @param timeUnit
     */    
    public void initTS(String timeSeriesIRI, List<String> dataIRI, String dbURL, String timeUnit) {
        Iri tsIRI = iri(timeSeriesIRI);
    	
    	ModifyQuery modify = Queries.MODIFY();

    	// set prefix
    	modify.prefix(prefix_ontology);
    	// define type
    	modify.insert(tsIRI.isA(TimeSeries));
    	// relational database URL
    	modify.insert(tsIRI.has(hasRDB, dbURL));
    	
    	// link each data to time series
    	for (String data : dataIRI) {
    		TriplePattern ts_tp = iri(data).has(hasTimeSeries, tsIRI);
    		modify.insert(ts_tp);
    	}

    	// optional: define time unit
    	if (timeUnit != null) {
    		modify.insert(tsIRI.has(hasTimeUnit, iri(timeUnit)));
    	}

    	kbClient.executeUpdate(modify.getQueryString());
    }
    
    /**
     * Count number of time series IRIs in kb
     * <p>Previously used to generate a new unique time series IRI
     * @return
     */
	public int countTS() {
		SelectQuery query = Queries.SELECT();
    	String queryKey = "numtimeseries";
    	Variable ts = query.var();
    	Variable numtimeseries = SparqlBuilder.var(queryKey);
    	GraphPattern querypattern = ts.isA(TimeSeries);
    	Assignment count = Expressions.count(ts).as(numtimeseries);
    	
    	query.select(count).where(querypattern);
    	kbClient.setQuery(query.getQueryString());
    	
    	int queryresult = kbClient.executeQuery().getJSONObject(0).getInt(queryKey);
    	
    	return queryresult;
	}
	
    /**
     * Remove time series and all associated connections from kb
     * @param tsIRI
     */
	public void removeTimeSeries(String tsIRI) {
		
		// mh807: Necessary to check whether tsIRI (still) exists in kb?
		if (checkTimeSeriesExists(tsIRI)) {
			
			// sub query to search for all triples with tsIRI as the subject/object
			SubSelect sub = GraphPatterns.select();
			Variable predicate1 = SparqlBuilder.var("a");
			Variable predicate2 = SparqlBuilder.var("b");
			Variable subject = SparqlBuilder.var("c");
			Variable object = SparqlBuilder.var("d");
			
			TriplePattern delete_tp1 = iri(tsIRI).has(predicate1, object);
			TriplePattern delete_tp2 = subject.has(predicate2, iri(tsIRI));		
			sub.select(predicate1, predicate2, subject, object).where(delete_tp1, delete_tp2);
			
			// insert subquery into main sparql update
			ModifyQuery modify = Queries.MODIFY();
			modify.delete(delete_tp1, delete_tp2).where(sub);
			
			kbClient.setQuery(modify.getQueryString());
			kbClient.executeUpdate();
		}
	}
	
	/**
	 * Remove all time series from kb
	 */
	public void removeAllTimeSeries() {
		// Get all time series in kb
		List<String> tsIRI = getAllTimeSeries();
		
		// Remove all time series
		if (!tsIRI.isEmpty()) {
			for (String ts : tsIRI) {
				removeTimeSeries(ts);
			}
		}
	}
	
    /**
     * Extract all time series IRIs from kb
     * @return
     */
	public List<String> getAllTimeSeries() {
		String queryString = "ts";
		SelectQuery query = Queries.SELECT();
		
		Variable ts = SparqlBuilder.var(queryString);
		TriplePattern queryPattern = ts.isA(TimeSeries);
		
		query.select(ts).where(queryPattern).prefix(prefix_kb);
		
		kbClient.setQuery(query.getQueryString());
		JSONArray queryResult = kbClient.executeQuery();
		
		List<String> tsIRI = new ArrayList<String>();
		for (int i = 0; i < queryResult.length(); i++) {
			tsIRI.add(queryResult.getJSONObject(i).getString(queryString));
		}
		
		return tsIRI;
	}
}
