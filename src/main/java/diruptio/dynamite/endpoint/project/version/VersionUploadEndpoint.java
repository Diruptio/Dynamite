package diruptio.dynamite.endpoint.project.version;

import static diruptio.dynamite.util.JsonUtil.*;

import diruptio.dynamite.Dynamite;
import diruptio.dynamite.Project;
import diruptio.spikedog.Endpoint;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public class VersionUploadEndpoint {
    @Endpoint(
            path = "/project/version/upload",
            methods = {"POST"})
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        if (!Dynamite.authenticate(request, response)) {
            return;
        }

        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        // Get the project id
        String projectId = request.parameter("project");
        if (projectId == null) {
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
            response.content(jsonError("Project not found"));
            return;
        }
        Path projectPath = Dynamite.getProjectsPath().resolve(projectId);

        // Get the version name
        String versionName = request.parameter("version");
        if (versionName == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"version\" (version name) is missing"));
            return;
        }

        // Check if the version exists
        if (project.get().versions().stream()
                .noneMatch(version2 -> version2.name().equals(versionName))) {
            response.status(HttpResponseStatus.NOT_FOUND);
            response.content(jsonError("Version not found"));
            return;
        }
        Path versionPath = projectPath.resolve(versionName);

        // Get the file name parameter
        String fileName = request.parameter("file");
        if (fileName == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"file\" is missing"));
            return;
        }

        try {
            // Write the file
            if (!Files.exists(versionPath)) {
                Files.createDirectories(versionPath);
            }
            byte[] bytes = new byte[request.content().readableBytes()];
            request.content().readBytes(bytes);
            Files.write(versionPath.resolve(fileName), bytes, StandardOpenOption.CREATE);
            Dynamite.getLogger()
                    .info("Uploaded file %s for version %s of project %s".formatted(fileName, versionName, projectId));
        } catch (IOException exception) {
            Dynamite.getLogger().log(Level.SEVERE, "Failed to write file " + versionPath.resolve(fileName), exception);
            response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.content(jsonError("Failed to write file"));
        }

        // Success
        response.status(HttpResponseStatus.OK);
    }
}
