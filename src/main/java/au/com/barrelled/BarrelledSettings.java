package au.com.barrelled;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "BarrelledSettings",
    storages = @Storage("barrelled.xml")
)
@Service(Service.Level.PROJECT)
public final class BarrelledSettings implements PersistentStateComponent<BarrelledSettings.State> {

    public static class State {
        public OutputMode outputMode = OutputMode.SAME_DIR;
        public String relativePath = "../";
        public String customPath = "";
        public String fileExtension = ".ts";
    }

    private State state = new State();

    public static BarrelledSettings getInstance(Project project) {
        return project.getService(BarrelledSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    public String resolveCustomPath(Project project) {
        String stored = state.customPath;
        if (stored == null || stored.isEmpty()) {
            return project.getBasePath();
        }

        String base = project.getBasePath();
        if (base == null) {
            return stored;
        }

        if (stored.startsWith("/")) {
            return stored;
        }

        return base + "/" + stored;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public OutputMode getOutputMode() {
        return state.outputMode;
    }

    public String getRelativePath() {
        return state.relativePath;
    }

    public String getCustomPath() {
        return state.customPath;
    }

    public String getFileExtension() {
        return state.fileExtension;
    }

    public void setOutputMode(OutputMode outputMode) {
        this.state.outputMode = outputMode;
    }

    public void setRelativePath(String relativePath) {
        this.state.relativePath = relativePath;
    }

    public void setCustomPath(String customPath) {
        this.state.customPath = customPath;
    }

    public void setFileExtension(String fileExtension) {
        this.state.fileExtension = fileExtension;
    }
}
