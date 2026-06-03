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
* `params.lims_status_success` (optional): The custom status value sent on success (defaults to `'SU'`).
* `params.lims_status_failure` (optional): The custom status value sent on failure (defaults to `'FA'`).

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
    lims_status_success        = 'SU' // Optional: Custom status value for success
    lims_status_failure        = 'FA' // Optional: Custom status value for failure
}
```

### Action Logic

When the pipeline finishes, the plugin fires a native `PATCH` request to:
`{lims_api_base_url}/{lims_pipeline_execution_id}`

With the following JSON payload containing the dynamically determined status code:
```json
{
  "status": "SU" // or the value of lims_status_success / lims_status_failure
}
```

And includes the authentication header:
`Authorization: ApiKey {lims_username}:{lims_api_key}`

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
