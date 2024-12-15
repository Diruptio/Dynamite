package diruptio.dynamite.endpoint.project;

import static diruptio.dynamite.util.JsonUtil.*;

import diruptio.dynamite.Dynamite;
import diruptio.dynamite.Project;
import diruptio.spikedog.Endpoint;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class ProjectTagsEndpoint {
    @Endpoint(
            path = "/project/tags",
            methods = {"GET"})
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        // Get the project id
        String projectId = request.parameter("id");
        if (projectId == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"id\" (project id) is missing"));
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

        // Success
        Set<String> tags = new HashSet<>();
        project.get().versions().forEach(version -> tags.addAll(version.tags()));
        response.status(HttpResponseStatus.OK);
        response.content(GSON.toJson(tags));
    }
}
