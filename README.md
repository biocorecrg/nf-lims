# nf-lims Nextflow Plugin

A Nextflow plugin that automatically intercepts pipeline completion events and updates a target Laboratory Information Management System (LIMS) or API endpoint with the pipeline status (`SU` for Success, `FA` for Failure).

## Generic REST API Compatibility

This plugin is designed to be **fully generic** and can be used to update any LIMS, database, or external service that exposes a REST API endpoint compatible with its request format. 

All connection details, including base URLs and credentials, are resolved dynamically at runtime using Nextflow parameter values.

### Dynamic Resolution Parameters

The plugin checks the Nextflow session parameters for the following keys:
* `params.lims_api_base_url`: The base URL of the target REST API.
* `params.lims_pipeline_execution_id`: The execution or run ID to append to the base URL.
* `params.lims_username` (optional): The username for API authentication.
* `params.lims_api_key` (optional): The API key/token for API authentication.
* `params.lims_status_key` (optional): The JSON key name for the status parameter (defaults to `'status'`).
* `params.lims_status_success` (optional): The custom status value sent on success (defaults to `'SU'`).
* `params.lims_status_failure` (optional): The custom status value sent on failure (defaults to `'FA'`).
* `params.lims_upload_file` (optional): Local file path to upload (e.g. `'results/multiqc_report.html'`). If not defined, file upload is skipped.
* `params.lims_upload_url` (optional): Endpoint URL for the file upload (defaults to `{lims_api_base_url}/{lims_pipeline_execution_id}/upload`).
* `params.lims_upload_method` (optional): HTTP method to use for file upload (e.g. `'POST'`, `'PUT'`. Defaults to `'POST'`).
* `params.lims_upload_mode` (optional): Body format for the file upload, either `'multipart'` (for standard `multipart/form-data`) or `'binary'` (for raw binary upload). Defaults to `'multipart'`.
* `params.lims_upload_form_field` (optional): The name of the form field parameter for the file when `lims_upload_mode` is `'multipart'` (defaults to `'file'`).

When a pipeline completes, the plugin issues a `PATCH` request to:
```
{params.lims_api_base_url}/{params.lims_pipeline_execution_id}
```

If both `params.lims_username` and `params.lims_api_key` are provided, it automatically adds the standard HTTP header:
```
Authorization: ApiKey {username}:{api_key}
```

---

## Configuration

To enable the plugin, add it to your pipeline's `nextflow.config`:

```groovy
plugins {
    id 'nf-lims@0.1.0'
}

params {
    lims_api_base_url          = 'http://your-lims-base-url.com/api'
    lims_pipeline_execution_id = '12345'
    lims_username              = 'your-username'
    lims_api_key               = 'your-api-key'
    lims_status_key            = 'status' // Optional: Custom JSON key for status parameter
    lims_status_success        = 'SU'     // Optional: Custom status value for success
    lims_status_failure        = 'FA'     // Optional: Custom status value for failure
    lims_upload_file           = 'results/multiqc_report.html' // Optional: File to upload on completion
    lims_upload_mode           = 'multipart' // Optional: 'multipart' or 'binary'
}
```

### Action Logic & Request Payload

When the pipeline finishes, the plugin fires a native `PATCH` request to update status:
`{lims_api_base_url}/{lims_pipeline_execution_id}`

#### HTTP Request Details:
* **Method**: `PATCH`
* **Headers**:
  * `Content-Type: application/json`
  * `Authorization: ApiKey {lims_username}:{lims_api_key}` (Only sent if both `lims_username` and `lims_api_key` are provided)
* **Payload Format**: JSON
* **Payload Schema**:
  ```json
  {
    "<lims_status_key>": "<lims_status_success/lims_status_failure>"
  }
  ```
  * The status key defaults to `"status"` but is customizable via `lims_status_key`.
  * The status value defaults to `"SU"` on success and `"FA"` on failure, but is customizable via `lims_status_success` and `lims_status_failure`.

### File Upload Logic

If `params.lims_upload_file` is defined, the plugin automatically uploads the specified file upon completion.

#### HTTP Request Details:
* **Method**: Customizable via `lims_upload_method` (defaults to `POST`).
* **Upload URL**: Defaults to `{lims_api_base_url}/{lims_pipeline_execution_id}/upload` but can be overridden with `lims_upload_url`.
* **Headers**:
  * `Authorization: ApiKey {lims_username}:{lims_api_key}` (if credentials provided)
  * `Content-Type`: Automatically set based on the `lims_upload_mode`:
    * `'multipart'`: `multipart/form-data; boundary=...` (sends the file within form field name specified by `lims_upload_form_field`, defaulting to `'file'`).
    * `'binary'`: The parsed content type of the file (e.g. `text/html`, `application/octet-stream`). Sends the file as raw request body.

---

## Development & Local Testing

### Prerequites
* Java 21 or later
* Nextflow installed

### Build the Plugin
To compile the plugin code and package it:
```bash
make assemble
```

### Install Locally
To compile and install the plugin to your local Nextflow plugins directory (`~/.nextflow/plugins/`):
```bash
make install
```

### Run Validation Tests
A mock LIMS server and integration pipeline are included in the `validation/` directory. Run the test suite:
```bash
./validation/run.sh
```
This script will build the plugin, spin up a Python mock server, run a mock Nextflow pipeline, and verify the console logs of the LIMS receiver.
