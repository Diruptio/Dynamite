package diruptio.dynamite.endpoint.project.version;

import static diruptio.dynamite.util.JsonUtil.*;

import com.google.gson.JsonSyntaxException;
import diruptio.dynamite.Dynamite;
import diruptio.dynamite.Project;
import diruptio.spikedog.Endpoint;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VerisonCreateEndpoint {
    @Endpoint(path = "/project/version/create")
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        if (!Dynamite.authenticate(request, response)) {
            return;
        }

        VersionCreateRequest createRequest;
        try {
            createRequest =
                    Objects.requireNonNull(GSON.fromJson(request.contentAsString(), VersionCreateRequest.class));
        } catch (JsonSyntaxException | NullPointerException e) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(
                    jsonError(
                            "Please check json structure: {\"project\": string, \"name\": string, \"tags\": string[], \"gitCommit\": string | null}"));
            return;
        }

        // Check if the project exists
        Optional<Project> project = Dynamite.getProjects().stream()
                .filter(project2 -> project2.name().equals(createRequest.project))
                .findFirst();
        if (project.isEmpty()) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.NOT_FOUND);
            response.content(jsonError("Project not found"));
            return;
        }

        // Check if the version exists
        if (project.get().versions().stream()
                .anyMatch(version2 -> version2.name().equals(createRequest.name))) {
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Version already exists"));
            return;
        }

        project.get()
                .versions()
                .add(new Project.Version(
                        createRequest.name, createRequest.tags, System.currentTimeMillis(), createRequest.gitCommit));
        Dynamite.save();
        Dynamite.getLogger()
                .info("Created version %s for project %s (Git: %s)"
                        .formatted(createRequest.name, createRequest.project, createRequest.gitCommit));

        // Success
        response.status(HttpResponseStatus.CREATED);
    }

    private record VersionCreateRequest(
            @NotNull String project, @NotNull String name, @NotNull Set<String> tags, @Nullable String gitCommit) {
        VersionCreateRequest {
            Objects.requireNonNull(project);
            Objects.requireNonNull(name);
            Objects.requireNonNull(tags);
        }
    }
}
