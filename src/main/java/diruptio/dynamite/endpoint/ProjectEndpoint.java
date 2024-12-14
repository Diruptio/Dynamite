package diruptio.dynamite.endpoint;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectEndpoint {
    @Endpoint(path = "/project")
    public void handle(final @NotNull HttpRequest request, final @NotNull HttpResponse response) {
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        ProjectRequest projectRequest;
        try {
            projectRequest = GSON.fromJson(request.contentAsString(), ProjectRequest.class);
        } catch (JsonSyntaxException e) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(
                    jsonError(
                            "Please check json structure: {\"project\": string, \"filter\": {\"tags\": string[] | null} | null}"));
            return;
        }

        // Check if the project exists
        Optional<Project> project = Dynamite.getProjects().stream()
                .filter(project2 -> project2.name().equals(projectRequest.project))
                .findFirst();
        if (project.isEmpty()) {
            response.status(HttpResponseStatus.NOT_FOUND);
            response.content(jsonError("Project not found"));
            return;
        }

        // Create filter
        Predicate<Project.Version> filter = version -> true;
        if (projectRequest.filter != null) {
            if (projectRequest.filter.tags != null) {
                filter = filter.and(version -> version.tags().containsAll(projectRequest.filter.tags));
            }
        }

        // Success
        response.status(HttpResponseStatus.OK);
        response.content(GSON.toJson(project.get().filterVersions(filter)));
    }

    private record ProjectRequestFilter(@Nullable Set<String> tags) {}

    private record ProjectRequest(@NotNull String project, @Nullable ProjectRequestFilter filter) {
        ProjectRequest {
            Objects.requireNonNull(project);
        }
    }
}
