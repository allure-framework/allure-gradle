tasks.withType<AbstractArchiveTask>().configureEach {
    // Ensure builds are reproducible
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
