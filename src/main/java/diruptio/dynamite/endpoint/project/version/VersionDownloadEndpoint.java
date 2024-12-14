package diruptio.dynamite.endpoint.project.version;

import static diruptio.dynamite.util.JsonUtil.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import diruptio.dynamite.Dynamite;
import diruptio.dynamite.Project;
import diruptio.spikedog.Endpoint;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public class VersionDownloadEndpoint {
    @Endpoint(
            path = "/project/version/download",
            methods = {"GET"})
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        // Get the project name
        String projectName = request.parameter("project");
        if (projectName == null) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"project\" is missing"));
            return;
        }

        // Check if the project exists
        Optional<Project> project = Dynamite.getProjects().stream()
                .filter(project2 -> project2.name().equals(projectName))
                .findFirst();
        if (project.isEmpty()) {
            response.status(HttpResponseStatus.NOT_FOUND);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.content(jsonError("Project not found"));
            return;
        }
        Path projectPath = Dynamite.getProjectsPath().resolve(projectName);

        // Get the version name
        String versionName = request.parameter("version");
        if (versionName == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.content(jsonError("Parameter \"version\" is missing"));
            return;
        }
        Path versionPath = projectPath.resolve(versionName);

        // Check if the version exists
        Optional<Project.Version> version = project.get().versions().stream()
                .filter(version2 -> version2.name().equals(versionName))
                .findFirst();
        if (version.isEmpty()) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.NOT_FOUND);
            response.content(jsonError("Version not found"));
            return;
        }

        // Get downloads
        List<Path> files = Dynamite.getFiles(projectName, versionName);
        Path file;
        if (files.isEmpty()) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.NOT_FOUND);
            response.content(jsonError("No files found"));
            return;
        } else if (files.size() == 1) {
            file = files.getFirst();
        } else {
            String fileName = request.parameter("file");
            if (fileName == null) {
                response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                response.status(HttpResponseStatus.BAD_REQUEST);
                JsonObject json = new JsonObject();
                json.addProperty("error", "Multiple downloads found and parameter \"download\" is missing");
                JsonArray downloadsJson = new JsonArray();
                for (Path file2 : files) {
                    downloadsJson.add(file2.getFileName().toString());
                }
                json.add("downloads", downloadsJson);
                response.content(json.toString());
                return;
            } else {
                file = versionPath.resolve(fileName);
                if (!Files.exists(file)) {
                    response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                    response.status(HttpResponseStatus.NOT_FOUND);
                    response.content(jsonError("File not found"));
                    return;
                }
            }
        }

        // Read file
        try {
            response.content(Unpooled.wrappedBuffer(Files.readAllBytes(file)));
        } catch (IOException exception) {
            Dynamite.getLogger().log(Level.SEVERE, "Failed to read file", exception);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.content(jsonError("Failed to read file"));
            return;
        }

        // Success
        response.header(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"");
        response.status(HttpResponseStatus.OK);
    }
}
