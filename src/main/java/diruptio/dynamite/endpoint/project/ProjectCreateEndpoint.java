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
import java.util.ArrayList;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectCreateEndpoint {
    @Endpoint(path = "/project/create")
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        if (!Dynamite.authenticate(request, response)) {
            return;
        }

        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

        ProjectCreateRequest createRequest;
        try {
            createRequest =
                    Objects.requireNonNull(GSON.fromJson(request.contentAsString(), ProjectCreateRequest.class));
        } catch (JsonSyntaxException | NullPointerException e) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Please check json structure: {\"name\": string, \"gitUrl\": string | null}"));
            return;
        }

        // Check if the project already exists
        if (Dynamite.getProjects().stream().anyMatch(project2 -> project2.name().equals(createRequest.name))) {
            response.status(HttpResponseStatus.BAD_REQUEST);
            response.content(jsonError("Project already exists"));
            return;
        }

        Dynamite.getProjects()
                .add(new Project(
                        createRequest.name, System.currentTimeMillis(), createRequest.gitUrl, new ArrayList<>()));
        Dynamite.save();
        Dynamite.getLogger().info("Created project %s (Git: %s)".formatted(createRequest.name, createRequest.gitUrl));

        // Success
        response.status(HttpResponseStatus.CREATED);
    }

    private record ProjectCreateRequest(@NotNull String name, @Nullable String gitUrl) {
        ProjectCreateRequest {
            Objects.requireNonNull(name);
        }
    }
}
