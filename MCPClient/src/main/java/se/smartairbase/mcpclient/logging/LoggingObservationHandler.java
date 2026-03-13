package se.smartairbase.mcpclient.logging;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
/**
 * Logs observation lifecycle events for easier local tracing.
 */
public class LoggingObservationHandler implements ObservationHandler<Observation.Context>{
    private static final Logger LOG =
            LoggerFactory.getLogger(LoggingObservationHandler.class);

    @Override
    public void onStart(Observation.Context context) {
        LOG.debug("Starting observation: {}", context.getName());
        LOG.debug("{}",context.getHighCardinalityKeyValues());
        LOG.debug("Starting observation: {}", context.getAllKeyValues());
        //ObservationHandler.super.onStart(context);
    }


    @Override
    public void onStop(Observation.Context context) {
        LOG.debug("Completed observation: {}", context.getName());
        LOG.debug("{}",context.getHighCardinalityKeyValues());
        LOG.debug("Completed observation: {}", context.getAllKeyValues());
        //ObservationHandler.super.onStop(context);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}
