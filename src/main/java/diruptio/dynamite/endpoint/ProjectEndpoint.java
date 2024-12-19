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
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public class ProjectEndpoint {
    @Endpoint(
            path = "/project",
            methods = {"GET"})
    public void handle(final @NotNull HttpRequest request, final @NotNull HttpResponse response) {
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

        // Create filter
        Predicate<Project.Version> filter = version -> true;

        // Get the tags for filtering
        String tagsParam = request.parameter("tags");
        if (tagsParam != null) {
            Set<String> tags = Set.of(tagsParam.split(";"));
            filter = filter.and(version -> version.tags().containsAll(tags));
        }

        // Success
        response.status(HttpResponseStatus.OK);
        response.content(project.get()
                .filterVersions(filter)
                .sortVersions(new VersionComparator())
                .withDownloads()
                .toString());
    }

    private static class VersionComparator implements Comparator<Project.Version> {
        @Override
        public int compare(Project.Version version1, Project.Version version2) {
            return Long.compare(version2.creationDate(), version1.creationDate());
        }
    }
}
