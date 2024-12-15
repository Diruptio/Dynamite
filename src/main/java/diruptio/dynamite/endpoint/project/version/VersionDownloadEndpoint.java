package diruptio.dynamite.endpoint.project.version;

import static diruptio.dynamite.util.JsonUtil.*;

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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public class VersionDownloadEndpoint {
    @Endpoint(
            path = "/project/version/download",
            methods = {"GET"})
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        // Get the project id
        String projectId = request.parameter("project");
        if (projectId == null) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"project\" (project id) is missing"));
            return;
        }

        // Check if the project exists
        Optional<Project> project = Dynamite.getProjects().stream()
                .filter(project2 -> project2.id().equals(projectId))
                .findFirst();
        if (project.isEmpty()) {
            response.status(HttpResponseStatus.NOT_FOUND);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.content(jsonError("Project not found"));
            return;
        }
        Path projectPath = Dynamite.getProjectsPath().resolve(projectId);

        // Get the version name
        String versionName = request.parameter("version");
        if (versionName == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.content(jsonError("Parameter \"version\" (version name) is missing"));
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
        Set<String> files = Dynamite.getFiles(projectId, versionName);
        String file;
        if (files == null || files.isEmpty()) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.NOT_FOUND);
            response.content(jsonError("No files found"));
            return;
        } else if (files.size() == 1) {
            file = files.stream().findFirst().get();
        } else {
            file = request.parameter("file");
            if (file == null) {
                response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                response.status(HttpResponseStatus.BAD_REQUEST);
                response.content(jsonError("Multiple downloads found and parameter \"download\" is missing"));
                return;
            }
            if (!files.contains(file)) {
                response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
                response.status(HttpResponseStatus.NOT_FOUND);
                response.content(jsonError("File not found"));
                return;
            }
        }

        // Read the file
        try {
            response.content(Unpooled.wrappedBuffer(Files.readAllBytes(versionPath.resolve(file))));
        } catch (IOException exception) {
            Dynamite.getLogger().log(Level.SEVERE, "Failed to read file", exception);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.content(jsonError("Failed to read file"));
            return;
        }

        // Success
        response.header(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"");
        response.status(HttpResponseStatus.OK);
    }
}
