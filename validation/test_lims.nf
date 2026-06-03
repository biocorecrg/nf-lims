nextflow.enable.dsl=2

process hello {
    output:
    stdout

    script:
    """
    echo "Running simple test pipeline to verify LIMS integration..."
    """
}

workflow {
    hello()
}
