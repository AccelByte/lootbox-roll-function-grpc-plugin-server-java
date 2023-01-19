package net.accelbyte.grpc;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.stereotype.Component;

@Component
public class GrpcServerCallMeterFilter implements MeterFilter {
    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().equals("grpc.server.calls")) {
            return DistributionStatisticConfig.builder()
                    .percentiles(.95, .99)
                    .build()
                    .merge(config);
        }
        return config;
    }
}