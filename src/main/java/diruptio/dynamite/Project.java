package diruptio.dynamite;

import static diruptio.dynamite.util.JsonUtil.GSON;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Project(
        @NotNull String id,
        @NotNull String name,
        long creationDate,
        @Nullable String gitUrl,
        @NotNull List<Version> versions) {
    /**
     * Creates a virtual copy of this project with the same id, name, creation date and gitUrl but filters the versions
     *
     * @param filter The filter
     * @return The new project with filtered versions
     */
    public @NotNull Project filterVersions(final @NotNull Predicate<Version> filter) {
        List<Version> versions = new ArrayList<>();
        this.versions.stream().filter(filter).forEach(versions::add);
        return new Project(id, name, creationDate, gitUrl, versions);
    }

    /**
     * Creates a virtual copy of this project with the same id, name, creation date and gitUrl but sorts the versions
     *
     * @param comparator The version comparator
     * @return The new project with filtered versions
     */
    public @NotNull Project sortVersions(final @NotNull Comparator<Version> comparator) {
        List<Version> versions = new ArrayList<>(this.versions);
        versions.sort(comparator);
        return new Project(id, name, creationDate, gitUrl, versions);
    }

    public @NotNull JsonObject withFiles() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("creationDate", creationDate);
        json.addProperty("gitUrl", gitUrl);
        JsonArray versions = new JsonArray();
        for (Project.Version version : this.versions) {
            versions.add(version.withFiles(id));
        }
        json.add("versions", versions);
        return json;
    }

    /**
     * Creates a JSON object with information about the project id, name, creation date and the git url.
     *
     * @return The JSON object
     */
    public @NotNull JsonObject withoutVersions() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("creationDate", creationDate);
        json.addProperty("gitUrl", gitUrl);
        return json;
    }

    public record Version(
            @NotNull String name, @NotNull Set<String> tags, long creationDate, @Nullable String gitCommit) {
        public @NotNull JsonObject withFiles(final @NotNull String project) {
            JsonObject json = GSON.toJsonTree(this).getAsJsonObject();
            json.add("files", GSON.toJsonTree(Dynamite.getFiles(project, name)));
            return json;
        }
    }
}
