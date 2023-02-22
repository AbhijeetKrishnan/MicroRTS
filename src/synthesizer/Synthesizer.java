package synthesizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ai.synthesis.dslForScriptGenerator.DslAI;

public abstract class Synthesizer {
    public String name;
    public abstract DslAI generate(String mapPath);

    public String getMap(String mapPath) {
        String map = null;
        Path inputMapPath = Paths.get(mapPath);
        try {
            map = Files.readString(inputMapPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public String getMapName(String mapPath) {
        File map = new File(mapPath);
        String mapName = map.getName();
        return mapName;
    }
}
