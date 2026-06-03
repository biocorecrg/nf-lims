package biocorecrg.plugin

import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceObserverFactory
import org.pf4j.Extension

@Extension
class LimsFactory implements TraceObserverFactory {
    @Override
    Collection<TraceObserver> create(Session session) {
        return [new LimsObserver(session)]
    }
}
