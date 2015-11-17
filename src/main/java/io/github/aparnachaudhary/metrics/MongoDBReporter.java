package io.github.aparnachaudhary.metrics;

import com.codahale.metrics.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * Created by aparna on 11/15/15.
 *
 * @author aparna
 */
public class MongoDBReporter extends ScheduledReporter {

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private String prefix;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private ServerAddress[] serverAddresses = new ServerAddress[]{new ServerAddress("localhost", 27017)};
        private String databaseName = "metricstore";
        private int timeout = 1000;
        private Map<String, Object> additionalFields;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.prefix = null;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Inject your custom definition of how time passes. Usually the default clock is sufficient
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Configure a prefix for each metric name. Optional, but useful to identify single hosts
         */
        public Builder prefixedWith(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Convert all the rates to a certain timeunit, defaults to seconds
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert all the durations to a certain timeunit, defaults to milliseconds
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Allows to configure a special MetricFilter, which defines what metrics are reported
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder withDatabaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder serverAddresses(ServerAddress[] serverAddresses) {
            this.serverAddresses = serverAddresses;
            return this;
        }

        /**
         * The timeout to wait for until a connection attempt is and the next host is tried
         */
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Additional fields to be included for each metric
         *
         * @param additionalFields
         * @return
         */
        public Builder additionalFields(Map<String, Object> additionalFields) {
            this.additionalFields = additionalFields;
            return this;
        }

        public MongoDBReporter build() {
            return new MongoDBReporter(registry,
                    databaseName,
                    serverAddresses,
                    timeout,
                    clock,
                    prefix,
                    rateUnit,
                    durationUnit,
                    filter,
                    additionalFields);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBReporter.class);

    private final ServerAddress[] serverAddresses;
    private final String databaseName;
    private final Clock clock;
    private final String prefix;
    private final int timeout;
    private final MongoDatabase mongoDatabase;

    public MongoDBReporter(MetricRegistry registry, String databaseName, ServerAddress[] serverAddresses, int timeout,
                           Clock clock, String prefix, TimeUnit rateUnit, TimeUnit durationUnit,
                           MetricFilter filter, Map<String, Object> additionalFields) {
        super(registry, "mongodb-reporter", filter, rateUnit, durationUnit);
        this.databaseName = databaseName;
        this.serverAddresses = serverAddresses;
        this.clock = clock;
        this.prefix = prefix;
        this.timeout = timeout;
        mongoDatabase = getDB();
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        // nothing to do if we don't have any metrics to report
        if (gauges.isEmpty() && counters.isEmpty() && histograms.isEmpty() && meters.isEmpty() && timers.isEmpty()) {
            LOGGER.info("All metrics empty, nothing to report");
            return;
        }

        final long timestamp = clock.getTime() / 1000;
        final Date reportingTime = new Date(timestamp);

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            if (entry.getValue().getValue() != null) {
                reportGauge(entry.getKey(), entry.getValue(), reportingTime);
            }
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            reportCounter(entry.getKey(), entry.getValue(), reportingTime);
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            reportHistogram(entry.getKey(), entry.getValue(), reportingTime);
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            reportMetered(entry.getKey(), entry.getValue(), reportingTime);
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            reportTimer(entry.getKey(), entry.getValue(), reportingTime);
        }

    }

    private void reportGauge(final String name, final Gauge gauge, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("gauge");
        final Object value = gauge.getValue();

        final GaugeEntity entity = new GaugeEntity();
        entity.setName(name(prefix, name));
        entity.setTimestamp(timestamp);
        entity.setValue(value);

        try {
            coll.insertOne(new Document(entity));
        } catch (MongoException e) {
            LOGGER.warn("Unable to report gauge {}", name, e);
        }
    }

    private void reportCounter(final String name, final Counter counter, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("counter");

        final CounterEntity entity = new CounterEntity();
        entity.setName(name(prefix, name));
        entity.setCount(counter.getCount());
        entity.setTimestamp(timestamp);

        try {
            coll.insertOne(new Document(entity));
        } catch (MongoException e) {
            LOGGER.warn("Unable to report counter {}", name, e);
        }
    }

    private void reportHistogram(final String name, final Histogram histogram, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("histogram");

        final Snapshot snapshot = histogram.getSnapshot();
        final HistogramEntity entity = new HistogramEntity(snapshot);
        entity.setName(name(prefix, name));
        entity.setCount(histogram.getCount());
        entity.setTimestamp(timestamp);

        try {
            coll.insertOne(new Document(entity));
        } catch (MongoException e) {
            LOGGER.warn("Unable to report histogram {}", name, e);
        }
    }

    private void reportMetered(final String name, final Metered meter, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("metered");
        final MeteredEntity entity = new MeteredEntity(meter);
        entity.setName(name(prefix, name));
        entity.setTimestamp(timestamp);

        try {
            coll.insertOne(new Document(entity));
        } catch (MongoException e) {
            LOGGER.warn("Unable to report meter {}", name, e);
        }
    }

    private void reportTimer(final String name, final Timer timer, final Date timestamp) {
        final MongoCollection coll = mongoDatabase.getCollection("timer");
        final TimerEntity entity = new TimerEntity(timer);
        entity.setName(name(prefix, name));
        entity.setTimestamp(timestamp);

        try {
            coll.insertOne(new Document(entity));
        } catch (MongoException e) {
            LOGGER.warn("Unable to report timer {}", name, e);
        }
    }

    private MongoDatabase getDB() {
        MongoClient mongoClient = new MongoClient(Arrays.asList(serverAddresses));
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        return database;
    }
}
