package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.*;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author aparna
 */
@RunWith(MockitoJUnitRunner.class)
public class MongoDBReporterTest {

    private final static long timestamp = 1000198;
    private final MetricRegistry registry = mock(MetricRegistry.class);
    private static final String databaseName = "test";

    private MongodExecutable mongodExecutable = null;
    MongoDBReporter reporter = null;

    @Before
    public void setUp() throws Exception {
        MongodConfig mongodConfig = new MongodConfig(Version.Main.V2_0, 27017, false);
        MongodStarter runtime = MongodStarter.getDefaultInstance();
        mongodExecutable = runtime.prepare(mongodConfig);
        MongodProcess mongod = mongodExecutable.start();

        reporter = MongoDBReporter.forRegistry(registry)
                .withClock(new Clock.UserTimeClock())
                .prefixedWith("junit")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .withDatabaseName(databaseName)
                .serverAddresses(new ServerAddress[]{new ServerAddress("localhost", 27017)})
                .build();
    }

    @After
    public void tearDown() {
        if (mongodExecutable != null)
            mongodExecutable.stop();
    }

    @Test
    public void reportStringGaugeValues() throws Exception {

        reporter.report(map("server.status", gauge("OK")), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("gauge");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertEquals("Does not save correct value", "OK", value.get("value"));
        assertEquals("Does not save correct name", "junit.server.status", value.get("name"));
    }

    @Test
    public void byteGaugeValuesNotSupported() throws Exception {
        reporter.report(map("gauge", gauge((byte) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        MongoCollection mongoCollection = getMongoCollection("gauge");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertNotNull("Does not set value", value);
        assertNotEquals("Unexpected type", Byte.class, value.get("value").getClass());
        assertEquals("Does not save correct name", "junit.gauge", value.get("name"));
    }

    @Test
    public void shortGaugeValuesNotSupported() throws Exception {
        reporter.report(map("gauge", gauge((short) 1)), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());
        MongoCollection mongoCollection = getMongoCollection("gauge");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertNotEquals("Unexpected type", Short.class, value.get("value").getClass());
        assertEquals("Does not save correct name", "junit.gauge", value.get("name"));
    }

    // TODO: Add tests for Integer, Float, Double, Long

    @Test
    public void reportsCounters() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        reporter.report(this.<Gauge>map(), this.<Counter>map("counter", counter), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("counter");
        assertEquals(1, mongoCollection.count());

        final Document value = (Document) mongoCollection.find().first();
        assertEquals("Does not save correct name", "junit.counter", value.get("name"));
        assertEquals("Does not save correct count", 100L, value.get("count"));
    }


    @Test
    public void reportsHistograms() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(7.0);
        when(snapshot.get95thPercentile()).thenReturn(8.0);
        when(snapshot.get98thPercentile()).thenReturn(9.0);
        when(snapshot.get99thPercentile()).thenReturn(10.0);
        when(snapshot.get999thPercentile()).thenReturn(11.0);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map("histogram", histogram), this.<Meter>map(), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("histogram");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();

        assertEquals("Does not save correct name", "junit.histogram", value.get("name"));

        assertEquals("Does not have correct count", 1L, value.get("count"));
        assertEquals("Does not have correct max", 2L, value.get("max"));
        assertEquals("Does not have correct mean", 3.0, value.get("mean"));
        assertEquals("Does not have correct min", 4L, value.get("min"));
        assertEquals("Does not have correct stdDev", 5.0, value.get("stdDev"));
        assertEquals("Does not have correct median", 6.0, value.get("median"));
        assertEquals("Does not have correct p75", 7.0, value.get("p75"));
        assertEquals("Does not have correct p95", 8.0, value.get("p95"));
        assertEquals("Does not have correct p98", 9.0, value.get("p98"));
        assertEquals("Does not have correct p99", 10.0, value.get("p99"));
        assertEquals("Does not have correct p999", 11.0, value.get("p999"));

    }


    @Test
    public void reportsMeters() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getOneMinuteRate()).thenReturn(2.0);
        when(meter.getFiveMinuteRate()).thenReturn(3.0);
        when(meter.getFifteenMinuteRate()).thenReturn(4.0);
        when(meter.getMeanRate()).thenReturn(5.0);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map("meter", meter), this.<Timer>map());

        MongoCollection mongoCollection = getMongoCollection("metered");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();
        assertEquals("Does not save correct name", "junit.meter", value.get("name"));

        assertEquals("Does not have correct count", 1L, value.get("count"));
        assertEquals("Does not have correct one minute rate", 2.0, value.get("m1Rate"));
        assertEquals("Does not have correct five minute rate", 3.0, value.get("m5Rate"));
        assertEquals("Does not have correct fifteen minute rate", 4.0, value.get("m15Rate"));
        assertEquals("Does not have correct mean rate", 5.0, value.get("meanRate"));

    }

    @Test
    public void reportsTimers() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
        when(snapshot.getMean()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
        when(snapshot.getMin()).thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
        when(snapshot.getStdDev()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
        when(snapshot.getMedian()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
        when(snapshot.get75thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
        when(snapshot.get95thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
        when(snapshot.get98thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
        when(snapshot.get99thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
        when(snapshot.get999thPercentile()).thenReturn((double) TimeUnit.MILLISECONDS.toNanos(1000));

        when(timer.getSnapshot()).thenReturn(snapshot);

        reporter.report(this.<Gauge>map(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), map("timer", timer));

        MongoCollection mongoCollection = getMongoCollection("timer");
        assertEquals(1, mongoCollection.count());
        final Document value = (Document) mongoCollection.find().first();


        assertEquals("Does not save correct name", "junit.timer", value.get("name"));

        assertEquals("Does not have correct count", 1L, value.get("count"));
        assertEquals("Does not have correct one minute rate", 3.0, value.get("m1Rate"));
        assertEquals("Does not have correct five minute rate", 4.0, value.get("m5Rate"));
        assertEquals("Does not have correct fifteen minute rate", 5.0, value.get("m15Rate"));
        assertEquals("Does not have correct mean rate", 2.0, value.get("meanRate"));

        final Document reportedSnaphot = (Document) value.get("snapshot");

        assertEquals("Does not have correct max", 100000000L, reportedSnaphot.get("max"));
        assertEquals("Does not have correct mean", 2.0E8, reportedSnaphot.get("mean"));
        assertEquals("Does not have correct min", 300000000L, reportedSnaphot.get("min"));
        assertEquals("Does not have correct stdDev", 4.0E8, reportedSnaphot.get("stdDev"));
        assertEquals("Does not have correct median", 5.0E8, reportedSnaphot.get("median"));
        assertEquals("Does not have correct p75", 6.0E8, reportedSnaphot.get("p75"));
        assertEquals("Does not have correct p95", 7.0E8, reportedSnaphot.get("p95"));
        assertEquals("Does not have correct p98", 8.0E8, reportedSnaphot.get("p98"));
        assertEquals("Does not have correct p99", 9.0E8, reportedSnaphot.get("p99"));
        assertEquals("Does not have correct p999", 1.0E9, reportedSnaphot.get("p999"));

    }

    private <T> SortedMap<String, T> map() {
        return new TreeMap<String, T>();
    }


    private <T> SortedMap<String, T> map(final String name, final T metric) {
        final TreeMap<String, T> map = new TreeMap<String, T>();
        map.put(name, metric);
        return map;
    }

    private <T> Gauge gauge(final T value) {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(value);
        return gauge;
    }

    private MongoCollection getMongoCollection(String collectionName) {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        return database.getCollection(collectionName);
    }
}
