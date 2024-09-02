package diruptio.dynamite.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import diruptio.dynamite.Dynamite;
import diruptio.dynamite.Project;
import diruptio.spikedog.HttpRequest;
import diruptio.spikedog.HttpResponse;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class ProjectTagsServlet implements BiConsumer<HttpRequest, HttpResponse> {
    public void accept(HttpRequest request, HttpResponse response) {
        response.setHeader("Content-Type", "application/json");

        // Get project parameter
        String projectParam = request.getParameter("project");
        if (projectParam == null) {
            response.setStatus(400, "Bad Request");
            JsonObject content = new JsonObject();
            content.addProperty("error", "Parameter 'project' was not provided");
            response.setContent(content.toString());
            return;
        }

        // Check if project exists
        Optional<Project> project = Dynamite.getProjects().stream()
                .filter(project2 -> project2.name().equals(projectParam))
                .findFirst();
        if (project.isEmpty()) {
            response.setStatus(404, "Not Found");
            JsonObject content = new JsonObject();
            content.addProperty("error", "Project not found");
            response.setContent(content.toString());
            return;
        }

        // Success
        Set<String> tagsSet = new HashSet<>();
        project.get().versions().forEach(version -> tagsSet.addAll(version.tags()));
        JsonObject content = new JsonObject();
        JsonArray tags = new JsonArray();
        tagsSet.forEach(tags::add);
        content.add("tags", tags);
        response.setContent(content.toString());
    }
}
