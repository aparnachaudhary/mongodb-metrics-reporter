package io.github.aparnachaudhary.metrics.model;

import com.codahale.metrics.Metered;

/**
 * Created by aparna on 11/17/15.
 */
public class MeteredEntity extends BaseEntity {

    private Long count;
    private Double m1Rate;
    private Double m5Rate;
    private Double m15Rate;
    private Double meanRate;


    public MeteredEntity(final Metered meter) {
        count = meter.getCount();
        m1Rate = meter.getOneMinuteRate();
        m5Rate = meter.getFiveMinuteRate();
        m15Rate = meter.getFifteenMinuteRate();
        meanRate = meter.getMeanRate();
        put("count", count);
        put("m1Rate", m1Rate);
        put("m5Rate", m5Rate);
        put("m15Rate", m15Rate);
        put("meanRate", meanRate);
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
}
