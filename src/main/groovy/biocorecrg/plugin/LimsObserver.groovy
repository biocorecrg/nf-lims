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
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

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
        def statusKey = params.keySet().contains('lims_status_key') ? params.get('lims_status_key') : 'status'
        def urlStr = "${limsBase}/${pipeId}"
        def payloadJson = "{\"${statusKey}\": \"${status}\"}"

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

            // Check if there is a file to upload
            if (params.keySet().contains('lims_upload_file') && params.get('lims_upload_file')) {
                uploadFile(client, user, apikey, params, limsBase, pipeId)
            }
        }
        catch (Exception e) {
            log.warn("Error updating LIMS status: ${e.message}", e)
        }
    }

    private void uploadFile(HttpClient client, Object user, Object apikey, Map params, Object limsBase, Object pipeId) {
        try {
            def uploadFileParam = params.get('lims_upload_file')
            def filePath = Paths.get(uploadFileParam.toString())
            if (!filePath.isAbsolute()) {
                filePath = Paths.get('.').toAbsolutePath().resolve(filePath).normalize()
            }

            if (!Files.exists(filePath)) {
                log.warn("LIMS upload skipped: File does not exist at ${filePath.toAbsolutePath()}")
                return
            }

            def uploadUrl = params.keySet().contains('lims_upload_url') ? params.get('lims_upload_url') : "${limsBase}/${pipeId}/upload"
            def uploadMethod = params.keySet().contains('lims_upload_method') ? params.get('lims_upload_method') : 'POST'
            def uploadMode = params.keySet().contains('lims_upload_mode') ? params.get('lims_upload_mode') : 'multipart'
            def uploadFormField = params.keySet().contains('lims_upload_form_field') ? params.get('lims_upload_form_field') : 'file'

            log.info("Uploading file ${filePath.toAbsolutePath()} to ${uploadUrl} using ${uploadMethod} (${uploadMode}) ...")

            def reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl.toString()))
                .timeout(Duration.ofSeconds(60))

            if (user && apikey) {
                reqBuilder.header("Authorization", "ApiKey ${user}:${apikey}")
            }

            if (uploadMode.toString().equalsIgnoreCase('binary')) {
                def contentType = Files.probeContentType(filePath) ?: "application/octet-stream"
                reqBuilder.header("Content-Type", contentType)
                reqBuilder.method(uploadMethod.toString().toUpperCase(), HttpRequest.BodyPublishers.ofFile(filePath))
            } else {
                // Multipart upload
                def boundary = "NextflowLimsBoundary" + System.currentTimeMillis()
                def fileName = filePath.getFileName().toString()
                def contentType = Files.probeContentType(filePath) ?: "application/octet-stream"

                def header = "--${boundary}\r\n" +
                             "Content-Disposition: form-data; name=\"${uploadFormField}\"; filename=\"${fileName}\"\r\n" +
                             "Content-Type: ${contentType}\r\n\r\n"
                def footer = "\r\n--${boundary}--\r\n"

                def headerBytes = header.getBytes("UTF-8")
                def fileBytes = Files.readAllBytes(filePath)
                def footerBytes = footer.getBytes("UTF-8")

                def totalLength = headerBytes.length + fileBytes.length + footerBytes.length
                def bodyBytes = new byte[totalLength]
                System.arraycopy(headerBytes, 0, bodyBytes, 0, headerBytes.length)
                System.arraycopy(fileBytes, 0, bodyBytes, headerBytes.length, fileBytes.length)
                System.arraycopy(footerBytes, 0, bodyBytes, headerBytes.length + fileBytes.length, footerBytes.length)

                reqBuilder.header("Content-Type", "multipart/form-data; boundary=${boundary}")
                reqBuilder.method(uploadMethod.toString().toUpperCase(), HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
            }

            def request = reqBuilder.build()
            def response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                log.info("LIMS file upload response: SUCCESS (${response.statusCode()}) - ${response.body()}")
            } else {
                log.warn("LIMS file upload response: FAILED (${response.statusCode()}) - ${response.body()}")
            }
        } catch (Exception e) {
            log.warn("Error uploading file to LIMS: ${e.message}", e)
        }
    }
}
