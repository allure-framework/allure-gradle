package buildlogic

import org.gradle.api.publish.maven.MavenPom

fun MavenPom.cleanupMavenPom() {
    withXml {
        val sb = asString()
        var s = sb.toString()
        // <scope>compile</scope> is Maven default, so delete it
        s = s.replace("<scope>compile</scope>", "")
        // Cut <dependencyManagement> because all dependencies have the resolved versions
        s = s.replace(
            Regex(
                "<dependencyManagement>.*?</dependencyManagement>",
                RegexOption.DOT_MATCHES_ALL
            ),
            ""
        )
        sb.setLength(0)
        sb.append(s)
        // Re-format the XML
        asNode()
    }
}
