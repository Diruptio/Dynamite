package diruptio.dynamite;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import diruptio.dynamite.project.ProjectCreateServlet;
import diruptio.dynamite.project.ProjectTagsServlet;
import diruptio.dynamite.project.version.VerisonCreateServlet;
import diruptio.dynamite.project.version.VersionDownloadServlet;
import diruptio.dynamite.project.version.VersionUploadServlet;
import diruptio.spikedog.Listener;
import diruptio.spikedog.Module;
import diruptio.spikedog.Spikedog;
import diruptio.spikedog.config.Config;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Dynamite implements Listener {
    private static final @NotNull Logger logger = Logger.getLogger("Dynamite");
    private static Config config;
    private static Path projectsPath;
    private static final @NotNull List<Project> projects = new ArrayList<>();

    @Override
    public void onLoad(@NotNull Module self) {
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

        Spikedog.addServlet("/projects", new ProjectsServlet());
        Spikedog.addServlet("/project", new ProjectServlet());
        Spikedog.addServlet("/project/create", new ProjectCreateServlet());
        Spikedog.addServlet("/project/tags", new ProjectTagsServlet());
        Spikedog.addServlet("/project/version/create", new VerisonCreateServlet());
        Spikedog.addServlet("/project/version/download", new VersionDownloadServlet());
        Spikedog.addServlet("/project/version/upload", new VersionUploadServlet());
    }

    private void load() {
        try {
            projects.clear();
            if (!Files.exists(projectsPath)) {
                Files.createDirectories(projectsPath);
            }
            Path projectsFile = projectsPath.resolve("projects.json");
            if (!Files.exists(projectsFile)) {
                Files.write(projectsFile, new JsonArray().toString().getBytes(), StandardOpenOption.CREATE_NEW);
            }
            BufferedReader reader = Files.newBufferedReader(projectsFile);
            JsonStreamParser parser = new JsonStreamParser(reader);
            JsonArray projects = parser.next().getAsJsonArray();
            reader.close();
            for (JsonElement project : projects) {
                if (project.isJsonPrimitive() && project.getAsJsonPrimitive().isString()) {
                    Path projectPath = projectsPath.resolve(project.getAsString());
                    Path projectFile = projectPath.resolve("project.json");
                    reader = Files.newBufferedReader(projectFile);
                    parser = new JsonStreamParser(reader);
                    JsonElement json = parser.next();
                    reader.close();
                    if (json.isJsonObject()) {
                        Dynamite.projects.add(Project.fromJson(json.getAsJsonObject()));
                    } else {
                        logger.warning("Invalid project: " + project);
                    }
                } else {
                    logger.warning("Invalid project name: " + project);
                }
            }
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Failed to load projects", exception);
        }
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

    public static @NotNull List<Project> getProjects() {
        return projects;
    }

    public static @Nullable List<String> getDownloads(@NotNull String project, @NotNull String version) {
        try (Stream<Path> children =
                Files.list(Dynamite.getProjectsPath().resolve(project).resolve(version))) {
            List<String> downloads = new ArrayList<>();
            for (Path child : children.toList()) {
                if (Files.isRegularFile(child)) {
                    downloads.add(child.getFileName().toString());
                }
            }
            downloads.sort(String::compareTo);
            return downloads;
        } catch (IOException e) {
            return null;
        }
    }

    public static void save() {
        try {
            Path projectsFile = projectsPath.resolve("projects.json");
            JsonArray projects = new JsonArray();
            Dynamite.projects.forEach(project -> projects.add(project.name()));
            Files.write(projectsFile, projects.toString().getBytes(), StandardOpenOption.CREATE);
            for (Project project : Dynamite.projects) {
                Path projectPath = projectsPath.resolve(project.name());
                if (!Files.exists(projectPath)) {
                    Files.createDirectories(projectPath);
                }
                Path projectFile = projectPath.resolve("project.json");
                Files.write(projectFile, project.toJson().toString().getBytes(), StandardOpenOption.CREATE);
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
