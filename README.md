# nf-lims Nextflow Plugin

A Nextflow plugin that automatically intercepts pipeline completion events and updates a target Laboratory Information Management System (LIMS) or API endpoint with the pipeline status (`SU` for Success, `FA` for Failure).

---

## Configuration

To enable the plugin, add it to your pipeline's `nextflow.config`:

```groovy
plugins {
    id 'nf-lims@0.1.0'
}

params {
    lims_api_base_url         = 'http://your-lims-base-url.com/api'
    lims_pipeline_execution_id = '12345'
    lims_username             = 'your-username'
    lims_api_key              = 'your-api-key'
}
```

### Action Logic

When the pipeline finishes, the plugin fires a native `PATCH` request to:
`{lims_api_base_url}/{lims_pipeline_execution_id}`

With the following JSON payload:
```json
{
  "status": "SU" // or "FA" on failure
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
