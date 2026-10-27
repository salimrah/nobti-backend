package ma.noba.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final String PRIMARY = "primary";
    private static final String REPLICA = "replica";

    public RoutingDataSource(DataSource primary, DataSource replica) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(PRIMARY, primary);
        if (replica != null) {
            targetDataSources.put(REPLICA, replica);
        }
        setDefaultTargetDataSource(primary);
        setTargetDataSources(targetDataSources);
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return REPLICA;
        }
        return PRIMARY;
    }
}
