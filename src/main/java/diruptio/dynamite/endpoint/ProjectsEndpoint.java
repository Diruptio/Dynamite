package diruptio.dynamite.endpoint;

import static diruptio.dynamite.util.JsonUtil.*;

import diruptio.dynamite.Dynamite;
import diruptio.dynamite.Project;
import diruptio.spikedog.Endpoint;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public class ProjectsEndpoint {
    @Endpoint(path = "/projects")
    public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response) {
        response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.status(HttpResponseStatus.OK);
        response.content(
                GSON.toJson(Dynamite.getProjects().stream().map(Project::name).toList()));
    }
}
