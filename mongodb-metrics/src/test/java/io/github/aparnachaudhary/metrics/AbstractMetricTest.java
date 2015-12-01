package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.Before;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

/**
 * Created by Aparna on 11/27/15.
 *
 * @author Aparna
 */
public class AbstractMetricTest {

    protected static final String databaseName = "test";


    protected final MetricRegistry registry = mock(MetricRegistry.class);

    private MongodExecutable mongodExecutable = null;
    MongoDBReporter reporter = null;

    @Before
    public void setUp() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        final int port = 27017;
        final IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.V3_0)
                .net(new Net(port, Network.localhostIsIPv6()))
                .build();

        mongodExecutable = starter.prepare(mongodConfig);
        final MongodProcess mongod = mongodExecutable.start();

        reporter = MongoDBReporter.forRegistry(registry)
                .withClock(new Clock.UserTimeClock())
                .prefixedWith("junit")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .withDatabaseName(databaseName)
                .serverAddresses(new ServerAddress[]{new ServerAddress("localhost", port)})
                .build();
    }

    @After
    public void tearDown() {
        if (mongodExecutable != null)
            mongodExecutable.stop();
    }


    protected <T> SortedMap<String, T> map() {
        return new TreeMap<String, T>();
    }


    protected <T> SortedMap<String, T> map(final String name, final T metric) {
        final TreeMap<String, T> map = new TreeMap<String, T>();
        map.put(name, metric);
        return map;
    }

    protected MongoCollection getMongoCollection(String collectionName) {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        return database.getCollection(collectionName);
    }
}
