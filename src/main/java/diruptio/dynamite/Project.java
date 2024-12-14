package diruptio.dynamite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Project(
        @NotNull String name, long creationDate, @Nullable String gitUrl, @NotNull List<Version> versions) {
    /**
     * Creates a virtual copy of this project with the original name and gitUrl but filters the versions
     *
     * @param filter The filter
     * @return The new project with filtered versions
     */
    public @NotNull Project filterVersions(final @NotNull Predicate<Version> filter) {
        List<Version> versions = new ArrayList<>();
        this.versions.stream().filter(filter).forEach(versions::add);
        return new Project(name, creationDate, gitUrl, versions);
    }

    public record Version(
            @NotNull String name, @NotNull Set<String> tags, long creationDate, @Nullable String gitCommit) {}
}
