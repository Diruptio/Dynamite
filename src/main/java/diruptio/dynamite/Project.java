package diruptio.dynamite;

import com.google.gson.JsonObject;
import java.util.ArrayList;
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
     * Creates a virtual copy of this project with the original name and gitUrl but filters the versions
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
            @NotNull String name, @NotNull Set<String> tags, long creationDate, @Nullable String gitCommit) {}
}
