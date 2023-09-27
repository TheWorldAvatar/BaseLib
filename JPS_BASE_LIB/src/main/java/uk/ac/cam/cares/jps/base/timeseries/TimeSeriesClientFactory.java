package uk.ac.cam.cares.jps.base.timeseries;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import uk.ac.cam.cares.jps.base.interfaces.TripleStoreClientInterface;

public class TimeSeriesClientFactory {
    /**
     * Factory method to get a TimeSeriesClient with the appropriate time class and
     * the RDB client class. This is only meant to be used for time series that are
     * already instantiated.
     * Queries the time and RDB client classes for the given data IRIs and
     * instantiates a TimeSeriesClient object.
     * 
     * @param storeClient
     * @param dataIriList
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static TimeSeriesClient<?> getInstance(TripleStoreClientInterface storeClient, List<String> dataIriList)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        TimeSeriesSparql timeSeriesSparql = new TimeSeriesSparql(storeClient);

        List<String> timeRdbUrl = timeSeriesSparql.getTimeClassRdbClassAndUrl(dataIriList);
        Class<?> timeClass = Class.forName(timeRdbUrl.get(0));
        Class<?> rdbClientClass = Class.forName(timeRdbUrl.get(1));
        String rdbUrl = timeRdbUrl.get(2);
        Constructor<?> constructor = rdbClientClass.getConstructor(Class.class);

        TimeSeriesRDBClientInterface<?> rdbClient = (TimeSeriesRDBClientInterface<?>) constructor
                .newInstance(timeClass);
        rdbClient.setRdbURL(rdbUrl);

        return new TimeSeriesClient<>(storeClient, rdbClient);
    }

    private TimeSeriesClientFactory() {
        throw new IllegalStateException(TimeSeriesClientFactory.class.getName());
    }
}
