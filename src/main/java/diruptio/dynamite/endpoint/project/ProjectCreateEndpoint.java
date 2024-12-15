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
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public class ProjectCreateEndpoint {
    @Endpoint(
            path = "/project/create",
            methods = {"POST"})
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        if (!Dynamite.authenticate(request, response)) {
            return;
        }

        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        // Get the project id
        String projectId = request.parameter("id");
        if (projectId == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"id\" (project id) is missing"));
            return;
        }

        // Get the project name
        String projectName = request.parameter("name");
        if (projectName == null) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Parameter \"name\" (project name) is missing"));
            return;
        }

        // Check if the project already exists
        if (Dynamite.getProjects().stream().anyMatch(project2 -> project2.id().equals(projectName))) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Project already exists"));
            return;
        }

        // Get the git url
        String gitUrl = request.parameter("git_url");

        Dynamite.getProjects()
                .add(new Project(projectId, projectName, System.currentTimeMillis(), gitUrl, new ArrayList<>()));
        Dynamite.save();
        Dynamite.getLogger().info("Created project %s (Git: %s)".formatted(projectName, gitUrl));

        // Success
        response.status(HttpResponseStatus.CREATED);
    }
}
