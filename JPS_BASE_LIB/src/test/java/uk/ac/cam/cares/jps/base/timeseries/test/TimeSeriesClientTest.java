package uk.ac.cam.cares.jps.base.timeseries.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesClient;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesRDBClient;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesSparql;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TimeSeriesClientTest {

    // Instance of the class to test
    private TimeSeriesClient<Instant> testClient;
    // Instance of the class to test with mocked sub-clients
    private TimeSeriesClient<Instant> testClientWithMocks;
    // Time series test data
    private List<String> dataIRIs;
    private List<Class<?>> dataClasses;
    private String timeUnit = "http://s";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private TimeSeriesSparql mockSparqlClient;
    @Mock private TimeSeriesRDBClient<Instant> mockRDBClient;
    private AutoCloseable closeMocks;

    @Before
    public void setUpClient() throws URISyntaxException, IOException {
        testClient = new TimeSeriesClient<>(Instant.class,
                Paths.get(Objects.requireNonNull(getClass().getResource("/timeseries.properties")).toURI()).toString());
        testClientWithMocks = new TimeSeriesClient<>(Instant.class,
                Paths.get(Objects.requireNonNull(getClass().getResource("/timeseries.properties")).toURI()).toString());
    }

    @Before
    public void setUpTestData() {
         // Initialise time series with 3 associated data series
        dataIRIs = Arrays.asList("http://data1", "http://data2", "http://data3");
        // Specify type of data for each column (most data will be in doubles, but one can specify different data types)
        dataClasses = Arrays.asList(Double.class, String.class, Integer.class);
    }

    @Test
    public void testConstructorWithKBClient() throws IOException, NoSuchFieldException, IllegalAccessException, URISyntaxException {
        RemoteStoreClient kbClient = new RemoteStoreClient();
        kbClient.setQueryEndpoint("sparql_query");
        kbClient.setUpdateEndpoint("sparql_update");
        TimeSeriesClient<Instant> client = new TimeSeriesClient<>(kbClient, Instant.class,
                Paths.get(Objects.requireNonNull(getClass().getResource("/timeseries.properties")).toURI()).toString());

        // Retrieve the rdf client to test whether it is set correctly
        TimeSeriesSparql rdfClient = client.getRdfClient();
        Field kbClientField = TimeSeriesSparql.class.getDeclaredField("kbClient");
        kbClientField.setAccessible(true);
        StoreClientInterface setKBClient = (StoreClientInterface) kbClientField.get(rdfClient);
        Assert.assertEquals(kbClient.getQueryEndpoint(), setKBClient.getQueryEndpoint());
        Assert.assertEquals(kbClient.getUpdateEndpoint(), setKBClient.getUpdateEndpoint());
        // Retrieve the rdb client to test whether it is set correctly
        TimeSeriesRDBClient<Instant> rdbClient = client.getRdbClient();
        Assert.assertEquals("jdbc:postgresql:timeseries", rdbClient.getRdbURL());
        Assert.assertEquals("postgres", rdbClient.getRdbUser());
    }

    @Before
    public void openMocks() {
        closeMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void releaseMocks() throws Exception {
        closeMocks.close();
    }

    @Test
    public void testConstructorWithOnlyPropertiesFile() throws NoSuchFieldException, IllegalAccessException {
        // Retrieve the rdf client to test whether it is set correctly
        TimeSeriesSparql rdfClient = testClient.getRdfClient();
        Field kbClientField = TimeSeriesSparql.class.getDeclaredField("kbClient");
        kbClientField.setAccessible(true);
        StoreClientInterface setKBClient = (StoreClientInterface) kbClientField.get(rdfClient);
        Assert.assertEquals("http://localhost:9999/blazegraph/namespace/timeseries/sparql", setKBClient.getQueryEndpoint());
        Assert.assertEquals("http://localhost:9999/blazegraph/namespace/timeseries/sparql", setKBClient.getUpdateEndpoint());
        // Retrieve the rdb client to test whether it is set correctly
        TimeSeriesRDBClient<Instant> rdbClient = testClient.getRdbClient();
        Assert.assertEquals("jdbc:postgresql:timeseries", rdbClient.getRdbURL());
        Assert.assertEquals("postgres", rdbClient.getRdbUser());
    }

    @Test
    public void testSetKBClient() throws NoSuchFieldException, IllegalAccessException {
        RemoteStoreClient kbClient = new RemoteStoreClient();
        kbClient.setQueryEndpoint("sparql_query");
        kbClient.setUpdateEndpoint("sparql_update");
        testClient.setKBClient(kbClient);
        // Retrieve the rdf client to test whether it is set correctly
        TimeSeriesSparql rdfClient = testClient.getRdfClient();
        Field kbClientField = TimeSeriesSparql.class.getDeclaredField("kbClient");
        kbClientField.setAccessible(true);
        StoreClientInterface setKBClient = (StoreClientInterface) kbClientField.get(rdfClient);
        Assert.assertEquals(kbClient.getQueryEndpoint(), setKBClient.getQueryEndpoint());
        Assert.assertEquals(kbClient.getUpdateEndpoint(), setKBClient.getUpdateEndpoint());
    }

    @Test
    public void testSetRDBClient() throws NoSuchFieldException, IllegalAccessException {
        testClient.setRDBClient("testURL", "user", "password");
        // Retrieve the rdb client to test whether it is set correctly
        TimeSeriesRDBClient<Instant> rdbClient = testClient.getRdbClient();
        Assert.assertEquals("testURL", rdbClient.getRdbURL());
        Assert.assertEquals("user", rdbClient.getRdbUser());
        Field passwordField = TimeSeriesRDBClient.class.getDeclaredField("rdbPassword");
        passwordField.setAccessible(true);
        Assert.assertEquals("password", passwordField.get(rdbClient));
    }

    @Test
    public void testInitTimeSeriesExceptionAfterStep1() throws NoSuchFieldException, IllegalAccessException {
        // Set-up stubbing
        Mockito.doThrow(new JPSRuntimeException("KG down")).when(mockSparqlClient).
                initTS(Mockito.anyString(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
        setRDFMock();
        try {
            testClientWithMocks.initTimeSeries(dataIRIs, dataClasses, timeUnit);
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Timeseries was not created"));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
            Assert.assertEquals("KG down", e.getCause().getMessage());
            Assert.assertEquals(JPSRuntimeException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testInitTimeSeriesExceptionAfterStep2() throws NoSuchFieldException, IllegalAccessException {
        // KG reversion works //
        // Set-up stubbing
        Mockito.doNothing().when(mockSparqlClient).
                initTS(Mockito.anyString(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
        setRDFMock();
        Mockito.doThrow(new JPSRuntimeException("RDB down")).when(mockRDBClient).
                initTimeSeriesTable(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
        setRDBMock();
        // Set private fields accessible to insert the mock
        Field rdfClientField = TimeSeriesClient.class.getDeclaredField("rdfClient");
        rdfClientField.setAccessible(true);
        rdfClientField.set(testClientWithMocks, mockSparqlClient);
        Field rdbClientField = TimeSeriesClient.class.getDeclaredField("rdbClient");
        rdbClientField.setAccessible(true);
        rdbClientField.set(testClientWithMocks, mockRDBClient);
        try {
            testClientWithMocks.initTimeSeries(dataIRIs, dataClasses, timeUnit);
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Timeseries was not created"));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
            Assert.assertEquals("RDB down", e.getCause().getMessage());
            Assert.assertEquals(JPSRuntimeException.class, e.getCause().getClass());
        }
        // KG reversion does not work //
        // Set-up stubbing
        Mockito.doNothing().when(mockSparqlClient).
                initTS(Mockito.anyString(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
        ArgumentCaptor<String> tsIRI = ArgumentCaptor.forClass(String.class);
        Mockito.doThrow(new JPSRuntimeException("KG down")).when(mockSparqlClient).
                removeTimeSeries(tsIRI.capture());
        Mockito.doThrow(new JPSRuntimeException("RDB down")).when(mockRDBClient).
                initTimeSeriesTable(Mockito.anyList(), Mockito.anyList(), Mockito.anyString());
        // Set private fields accessible to insert the mock
        rdbClientField.setAccessible(true);
        rdbClientField.set(testClientWithMocks, mockRDBClient);
        try {
            testClientWithMocks.initTimeSeries(dataIRIs, dataClasses, timeUnit);
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Inconsistent state created when initialising time series"));
            Assert.assertTrue(e.getMessage().contains(tsIRI.getValue()));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
        }
    }

    @Test
    public void testDeleteIndividualTimeSeriesNoTSIRI() throws NoSuchFieldException, IllegalAccessException {
        // Set-up stubbing
        Mockito.when(mockSparqlClient.getTimeSeries(dataIRIs.get(0))).thenReturn(null);
        setRDFMock();
        try {
            testClientWithMocks.deleteIndividualTimeSeries(dataIRIs.get(0));
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("not associated with any timeseries"));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
            Assert.assertTrue(e.getMessage().contains(dataIRIs.get(0)));
        }
    }

    @Test
    public void testDeleteIndividualTimeSeriesExceptionAfterStep1() throws NoSuchFieldException, IllegalAccessException {
        String dataIRI = dataIRIs.get(0);
        // Set-up stubbing
        Mockito.when(mockSparqlClient.getTimeSeries(dataIRI)).thenReturn("tsIRI");
        Mockito.when(mockSparqlClient.getAssociatedData(dataIRI).size()).thenReturn(2);
        Mockito.doThrow(new JPSRuntimeException("KG down")).when(mockSparqlClient).removeTimeSeriesAssociation(dataIRI);
        setRDFMock();
        try {
            testClientWithMocks.deleteIndividualTimeSeries(dataIRI);
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("was not deleted"));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
            Assert.assertTrue(e.getMessage().contains(dataIRI));
            Assert.assertEquals("KG down", e.getCause().getMessage());
            Assert.assertEquals(JPSRuntimeException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testDeleteIndividualTimeSeriesExceptionAfterStep2() throws NoSuchFieldException, IllegalAccessException {
        String dataIRI = dataIRIs.get(0);
        // KG reversion works //
        // Set-up stubbing
        Mockito.when(mockSparqlClient.getTimeSeries(dataIRI)).thenReturn("tsIRI");
        Mockito.when(mockSparqlClient.getAssociatedData(dataIRI).size()).thenReturn(2);
        Mockito.doNothing().when(mockSparqlClient).removeTimeSeriesAssociation(dataIRI);
        setRDFMock();
        Mockito.doThrow(new JPSRuntimeException("RDB down")).when(mockRDBClient).deleteTimeSeries(dataIRI);
        setRDBMock();
        try {
            testClientWithMocks.deleteIndividualTimeSeries(dataIRI);
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("was not deleted"));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
            Assert.assertTrue(e.getMessage().contains(dataIRI));
            Assert.assertEquals("RDB down", e.getCause().getMessage());
            Assert.assertEquals(JPSRuntimeException.class, e.getCause().getClass());
        }
        // KG reversion does not work //
        // Set-up stubbing
        Mockito.when(mockSparqlClient.getTimeSeries(dataIRI)).thenReturn("tsIRI");
        Mockito.when(mockSparqlClient.getAssociatedData(dataIRI).size()).thenReturn(2);
        Mockito.doThrow(new JPSRuntimeException("KG down")).when(mockSparqlClient)
                .insertTimeSeriesAssociation(Mockito.anyString(), Mockito.anyString());
        Mockito.doThrow(new JPSRuntimeException("RDB down")).when(mockRDBClient).deleteTimeSeries(dataIRI);
        try {
            testClientWithMocks.deleteIndividualTimeSeries(dataIRI);
            Assert.fail();
        }
        catch (JPSRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Inconsistent state created when deleting time series"));
            Assert.assertTrue(e.getMessage().contains(testClientWithMocks.getClass().getSimpleName()));
            Assert.assertTrue(e.getMessage().contains(dataIRI));
        }
    }

    private void setRDFMock() throws NoSuchFieldException, IllegalAccessException {
        // Set private fields accessible to insert the mock
        Field rdfClientField = TimeSeriesClient.class.getDeclaredField("rdfClient");
        rdfClientField.setAccessible(true);
        rdfClientField.set(testClientWithMocks, mockSparqlClient);
    }

    private void setRDBMock() throws NoSuchFieldException, IllegalAccessException {
        Field rdbClientField = TimeSeriesClient.class.getDeclaredField("rdbClient");
        rdbClientField.setAccessible(true);
        rdbClientField.set(testClientWithMocks, mockRDBClient);
    }

}
