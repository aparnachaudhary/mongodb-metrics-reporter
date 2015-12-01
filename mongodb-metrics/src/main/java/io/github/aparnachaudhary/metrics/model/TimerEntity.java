package io.github.aparnachaudhary.metrics.model;

import com.codahale.metrics.Timer;

/**
 * @author aparna
 */
public class TimerEntity extends BaseEntity {

    private Long count;
    private Double m1Rate;
    private Double m5Rate;
    private Double m15Rate;
    private Double meanRate;
    private final HistogramEntity snapshot;

    public TimerEntity(final Timer timer) {
        count = timer.getCount();
        m1Rate = timer.getOneMinuteRate();
        m5Rate = timer.getFiveMinuteRate();
        m15Rate = timer.getFifteenMinuteRate();
        meanRate = timer.getMeanRate();
        snapshot = new HistogramEntity(timer.getSnapshot());
        put("count", count);
        put("m1Rate", m1Rate);
        put("m5Rate", m5Rate);
        put("m15Rate", m15Rate);
        put("meanRate", meanRate);
        put("snapshot", snapshot);
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getM1Rate() {
        return m1Rate;
    }

    public void setM1Rate(Double m1Rate) {
        this.m1Rate = m1Rate;
    }

    public Double getM5Rate() {
        return m5Rate;
    }

    public void setM5Rate(Double m5Rate) {
        this.m5Rate = m5Rate;
    }

    public Double getM15Rate() {
        return m15Rate;
    }

    public void setM15Rate(Double m15Rate) {
        this.m15Rate = m15Rate;
    }

    public Double getMeanRate() {
        return meanRate;
    }

    public void setMeanRate(Double meanRate) {
        this.meanRate = meanRate;
    }

    public HistogramEntity getSnapshot() {
        return snapshot;
    }
}
