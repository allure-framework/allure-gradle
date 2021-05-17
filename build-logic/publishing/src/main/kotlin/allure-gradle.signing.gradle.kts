plugins.withId("maven-publish") {
    apply(plugin = "signing")
}

// Developers do not always have PGP configured,
// so activate signing for release versions only
// Just in case Maven Central rejects signed snapshots for some reason
plugins.withId("signing") {
    if (!version.toString().endsWith("-SNAPSHOT")) {
        apply(plugin = "signing")
        configure<SigningExtension> {
            sign(the<PublishingExtension>().publications)
        }
    }
}
