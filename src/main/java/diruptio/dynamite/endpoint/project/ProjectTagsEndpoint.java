package diruptio.dynamite.endpoint.project;

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
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class ProjectTagsEndpoint {
    @Endpoint(
            path = "/project/tags",
            methods = {"GET"})
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        ProjectTagsRequest tagsRequest;
        try {
            tagsRequest = Objects.requireNonNull(GSON.fromJson(request.contentAsString(), ProjectTagsRequest.class));
        } catch (JsonSyntaxException | NullPointerException e) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Please check json structure: {\"project\": string}"));
            return;
        }

        // Check if the project exists
        Optional<Project> project = Dynamite.getProjects().stream()
                .filter(project2 -> project2.name().equals(tagsRequest.project))
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

    private record ProjectTagsRequest(@NotNull String project) {
        ProjectTagsRequest {
            Objects.requireNonNull(project);
        }
    }
}
