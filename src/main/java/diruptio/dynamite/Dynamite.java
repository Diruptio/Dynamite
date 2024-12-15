package diruptio.dynamite;

import static diruptio.dynamite.util.JsonUtil.*;

import com.google.gson.*;
import diruptio.dynamite.endpoint.ProjectEndpoint;
import diruptio.dynamite.endpoint.ProjectsEndpoint;
import diruptio.dynamite.endpoint.project.ProjectCreateEndpoint;
import diruptio.dynamite.endpoint.project.ProjectTagsEndpoint;
import diruptio.dynamite.endpoint.project.version.VerisonCreateEndpoint;
import diruptio.dynamite.endpoint.project.version.VersionDownloadEndpoint;
import diruptio.dynamite.endpoint.project.version.VersionUploadEndpoint;
import diruptio.spikedog.*;
import diruptio.spikedog.Module;
import diruptio.spikedog.config.Config;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class Dynamite implements Listener {
    private static final List<Project> projects = new ArrayList<>();
    private static Logger logger;
    private static Config config;
    private static Path projectsPath;

    @Override
    public void onLoad(@NotNull Module self) {
        logger = Logger.getLogger("Dynamite");
        logger.setParent(Spikedog.LOGGER);

        Path configFile = self.file().resolveSibling("Dynamite").resolve("config.yml");
        config = new Config(configFile, Config.Type.YAML);
        if (!config.contains("password")) {
            config.set("password", "YOUR_PASSWORD");
            config.save();
        }
        if (!config.contains("projects_path")) {
            config.set("projects_path", "projects");
            config.save();
        }
        projectsPath = Path.of(Objects.requireNonNull(config.getString("projects_path")));

        load();

        Spikedog.register(new ProjectEndpoint());
        Spikedog.register(new ProjectsEndpoint());
        Spikedog.register(new ProjectCreateEndpoint());
        Spikedog.register(new ProjectTagsEndpoint());
        Spikedog.register(new VerisonCreateEndpoint());
        Spikedog.register(new VersionDownloadEndpoint());
        Spikedog.register(new VersionUploadEndpoint());
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try {
            projects.clear();
            if (!Files.exists(projectsPath)) {
                Files.createDirectories(projectsPath);
            }

            Path projectIdsFile = projectsPath.resolve("projects.json");
            if (!Files.exists(projectIdsFile)) {
                Files.writeString(projectIdsFile, GSON.toJson(List.<String>of()), StandardOpenOption.CREATE_NEW);
            }
            List<String> projectIds = GSON.fromJson(Files.readString(projectIdsFile), List.class);

            for (String projectId : projectIds) {
                Path projectDirectory = projectsPath.resolve(projectId);
                if (!Files.exists(projectDirectory)) {
                    Files.createDirectories(projectDirectory);
                }
                Path projectFile = projectDirectory.resolve("project.json");
                if (!Files.exists(projectFile)) {
                    String str =
                            GSON.toJson(new Project(projectId, projectId, System.currentTimeMillis(), null, List.of()));
                    Files.writeString(projectFile, str, StandardOpenOption.CREATE_NEW);
                }
                try {
                    projects.add(GSON.fromJson(Files.readString(projectFile), Project.class));
                } catch (JsonSyntaxException exception) {
                    logger.log(Level.SEVERE, "Failed to load project " + projectId, exception);
                }
            }
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Failed to load projects", exception);
        }
    }

    public static void save() {
        try {
            Path projectsFile = projectsPath.resolve("projects.json");
            JsonArray projects = new JsonArray();
            Dynamite.projects.forEach(project -> projects.add(project.name()));
            Files.writeString(projectsFile, projects.toString(), StandardOpenOption.CREATE);

            for (Project project : Dynamite.projects) {
                Path projectDirectory = projectsPath.resolve(project.name());
                if (!Files.exists(projectDirectory)) {
                    Files.createDirectories(projectDirectory);
                }
                Path projectFile = projectDirectory.resolve("project.json");
                Files.writeString(projectFile, GSON.toJson(project), StandardOpenOption.CREATE);
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }

    public static @NotNull List<Path> getFiles(final @NotNull String projectId, final @NotNull String version) {
        Path versionPath = Dynamite.getProjectsPath().resolve(projectId).resolve(version);
        try (Stream<Path> pathStream = Files.list(versionPath).filter(Files::isRegularFile)) {
            return pathStream.toList();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to get the file list", e);
            return List.of();
        }
    }

    /**
     * Checks if a request has the correct authentication header. If it does not, a 401 Unauthorized error will be
     * returned.
     *
     * @param request The http request
     * @param response The http response
     * @return Whether the correct authentication header
     */
    public static boolean authenticate(final @NotNull HttpRequest request, final @NotNull HttpResponse response) {
        String password = ":" + config.getString("password");
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        String auth = "Basic " + Base64.getEncoder().encodeToString(bytes);

        CharSequence authorization = request.header(HttpHeaderNames.AUTHORIZATION);
        if (authorization != null && auth.contentEquals(authorization)) {
            // Correct password
            return true;
        } else {
            // Incorrect password
            response.status(HttpResponseStatus.UNAUTHORIZED);
            response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            response.header(HttpHeaderNames.WWW_AUTHENTICATE, "Basic charset=\"UTF-8\"");
            response.content(jsonError("Unauthorized"));
            return false;
        }
    }

    public static @NotNull List<Project> getProjects() {
        return projects;
    }

    public static @NotNull Logger getLogger() {
        return logger;
    }

    public static @NotNull Config getConfig() {
        return config;
    }

    public static @NotNull Path getProjectsPath() {
        return projectsPath;
    }
}
