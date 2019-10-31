package io.qameta.allure.gradle.task

import groovy.json.JsonSlurper
import io.qameta.allure.gradle.AllureExtension
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AllureUpload extends DefaultTask {

    private static OkHttpClient client = new OkHttpClient()
    private String jwtToken
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8")
    static final String NAME = "allureUpload"

    AllureUpload() {
        configureDefaults()
    }

    @Input
    String launchName

    @Input
    String endpoint

    @Input
    String token

    @Input
    String projectId

    @Input
    File resultsDir

    String achieveFileName

    @TaskAction
    uploadResults() {
        zipFiles()
        logIn()
        uploadArchive()
    }

    zipFiles() {
        achieveFileName = project.buildDir + "/allure-results.zip"
        ZipOutputStream zipFile = new ZipOutputStream(
                new FileOutputStream(achieveFileName)
        )
        resultsDir.eachFile { file ->
            if (file.isFile()){
                zipFile.putNextEntry(new ZipEntry(file.name))
                def buffer = new byte[file.size()]
                file.withInputStream {
                    zipFile.write(buffer, 0, it.read(buffer))
                }
                zipFile.closeEntry()
            }
        }
        zipFile.close()
    }

    logIn() {
        MediaType CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded")
        RequestBody body = new MultipartBody.Builder()
                .setType(CONTENT_TYPE)
                .addFormDataPart("grant_type", "apitoken")
                .addFormDataPart("scope", "openid")
                .addFormDataPart("token", token).build()
        String credentials = Credentials.basic("acme", "acmesecret")
        Request request = new Request.Builder()
                .url(endpoint + "/api/uaa/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", credentials)
                .post(body).build()
        Response response = client.newCall(request).execute()
        def parsed = new JsonSlurper().parseText(response.body().string())
        jwtToken = parsed.access_token
    }

    private String createLaunch() {
        RequestBody body = RequestBody.create(
                JSON,
                String.format("{'name': '%s', 'projectId': 'projectId'}", launchName, projectId)
        )
        Request request = new Request.Builder()
                .url(endpoint + "/api/rs/launch/new")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .post(body).build()
        Response response = client.newCall(request).execute()
        def parsed = new JsonSlurper().parseText(response.body.string())
        return parsed.id
    }

    uploadArchive() {
        MediaType CONTENT_TYPE = MediaType.parse("application/octet-stream")
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        "allure-results.zip",
                        RequestBody.create(CONTENT_TYPE, new File(project.buildDir + "/allure-results.zip")
                        )).build()
        Request request = new Request.Builder()
                .url(String.format("%s/api/rs/launch/%s/upload/archive", endpoint, createLaunch()))
                .header("Authorization", "Bearer " + jwtToken)
                .post(body).build()
        client.newCall(request).execute()
    }

    private void configureDefaults() {
        AllureExtension extension = project.extensions.getByType(AllureExtension)
        if (Objects.nonNull(extensions)) {
            launchName = extension.launchName
            endpoint = extension.endpoint
            token = extension.token
            projectId = extension.projectId
            resultsDir = extension.resultsDir
        }
    }

}
