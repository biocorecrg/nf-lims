package biocorecrg.plugin

import nextflow.Session
import nextflow.trace.TraceObserver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration

class LimsObserver implements TraceObserver {
    private static final Logger log = LoggerFactory.getLogger(LimsObserver)
    private Session session

    LimsObserver(Session session) {
        this.session = session
    }

    @Override
    void onFlowComplete() {
        def params = session.binding.getVariable('params') as Map ?: [:]

        def limsBase = params.keySet().contains('lims_api_base_url') ? params.get('lims_api_base_url') : null
        def pipeId = params.keySet().contains('lims_pipeline_execution_id') ? params.get('lims_pipeline_execution_id') : null
        def user = params.keySet().contains('lims_username') ? params.get('lims_username') : null
        def apikey = params.keySet().contains('lims_api_key') ? params.get('lims_api_key') : null

        if (!limsBase || !pipeId) {
            log.info("LIMS status update skipped: base URL or execution ID not configured.")
            return
        }

        def successStatus = params.keySet().contains('lims_status_success') ? params.get('lims_status_success') : 'SU'
        def failureStatus = params.keySet().contains('lims_status_failure') ? params.get('lims_status_failure') : 'FA'
        def status = session.isSuccess() ? successStatus : failureStatus
        def urlStr = "${limsBase}/${pipeId}"
        def payloadJson = "{\"status\": \"${status}\"}"

        log.info("Updating LIMS status to ${status} for execution ID: ${pipeId} ...")

        try {
            def client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()

            def reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payloadJson))

            if (user && apikey) {
                reqBuilder.header("Authorization", "ApiKey ${user}:${apikey}")
            }

            def request = reqBuilder.build()
            def response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                log.info("LIMS response: SUCCESS (${response.statusCode()}) - ${response.body()}")
            } else {
                log.warn("LIMS response: FAILED (${response.statusCode()}) - ${response.body()}")
            }
        }
        catch (Exception e) {
            log.warn("Error updating LIMS status: ${e.message}")
        }
    }
}
