package ast.animcreator.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import org.apache.commons.io.input.BOMInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class FileStorage {
    public static String modDirName = "animcreator";
    public static String internalStorageDirName = "internal";
    public static Path modPath;
    public static Path internalStoragePath;

    public final static String AnimFileExtension = ".anim";
    public final static String AnimTmpFileExtension = ".tmp";

    public static void initModStorageDir() {

        Path gamePath = FabricLoader.getInstance().getGameDir();

        modPath = Path.of(gamePath + "/" + modDirName);
        if (!Files.exists(modPath)) {
            try {
                Files.createDirectory(modPath);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        internalStoragePath = Path.of(modPath + "/" + internalStorageDirName);
        if (!Files.exists(internalStoragePath)) {
            try {
                Files.createDirectory(internalStoragePath);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean animExistsOnDisk(String animName) {
        return Files.exists(Path.of(internalStoragePath + "/" + animName + AnimFileExtension)) ||
                Files.exists(Path.of(internalStoragePath + "/" + animName + AnimFileExtension + AnimTmpFileExtension));
    }

    public static Path getAnimationPathFromName(String animName) {
        return Path.of(internalStoragePath + "/" + animName + AnimFileExtension);
    }

    public static Path getTmpAnimationPathFromName(String animName) {
        return Path.of(internalStoragePath + "/" + animName + AnimFileExtension + AnimTmpFileExtension);
    }

    public static int saveAnimToFile(Animation animation, boolean isTemporary, List<String> errors) {
        int nbLines = 0;
        for (Frame frame : animation.getFrames()) {
            nbLines += frame.blocks.size();
        }

        StringBuilder fileContent = new StringBuilder(nbLines * 100);

        if (animation.world.getDimensionEntry().getKey().isPresent()) {
            fileContent.append(animation.world.getDimensionEntry().getKey().get().getValue().toString()).append('\n');
        }
        else {
            errors.add("Can not save file : Unknown dimension");
            return -1;
        }
        for (Frame frame : animation.getFrames()) {
            if (frame.blocks.isEmpty()) {
                errors.add("Frame at tick " + frame.tick + " in animation " + animation.name + " has no blocks.");
                return -1;
            }
            fileContent.append(frame.tick).append('\n');
            for (FrameBlock frameBlock : frame.blocks) {
                fileContent.append(frameBlock.blockPos.getX()).append(" ")
                        .append(frameBlock.blockPos.getY()).append(" ")
                        .append(frameBlock.blockPos.getZ()).append(" ");
                fileContent.append(frameBlock.blockState.getRegistryEntry().getIdAsString());
                fileContent.append('\n');
            }
        }

        FileWriter fr;
        Path animFilePath = Path.of(internalStoragePath + "/" + animation.name + AnimFileExtension + (isTemporary ? AnimTmpFileExtension : ""));
        try {
            fr = new FileWriter(animFilePath.toFile());
            fr.write(fileContent.toString());
        } catch (IOException e) {
            errors.add("Unexpected error while trying to save animation " + animation.name);
            return -1;
        }

        try {
            fr.close();
        } catch (IOException e) {
            errors.add("Unexpected error while trying to save animation " + animation.name);
            return -1;
        }

        // If there is a tmp anim file with the same name as the one just saved, delete it
        if (!isTemporary) {
            Path tmpAnimFilePath = Path.of(internalStoragePath + "/" + animation.name + AnimFileExtension + AnimTmpFileExtension);
            if (Files.exists(tmpAnimFilePath)) {
                try {
                    Files.delete(tmpAnimFilePath);
                }
                catch(IOException e) {
                    e.printStackTrace();
                    errors.add("Internal error while removing temporary animation file.");
                    return -1;
                }
            }
        }

        return 0;
    }

    public static void saveTmpAnimFile(List<String> errors) {
        saveAnimToFile(GlobalManager.curAnimation, true, errors);
    }

    public static int loadAllAnimFiles(List<String> errors) {
        errors.clear();
        List<Path> tmpAnimFilesToLoad = new ArrayList<>();
        try {
            Stream<Path> stream = Files.list(internalStoragePath);
            stream.forEach((path) -> {
                String fullFileName = path.getFileName().toString();
                if (fullFileName.endsWith(AnimTmpFileExtension)) {
                    tmpAnimFilesToLoad.add(path);
                }
                else if (fullFileName.endsWith(AnimFileExtension) ){
                    boolean res = loadAnimFile(path, errors);
                    if (!res) {
                        errors.add("Failed to load anim " + path.getFileName().toString());
                    }
                }
            });
        }
        catch(IOException e) {
            return -1;
        }
        // Load tmp files after normal ones. If there are normal and tmp files with the same name,
        // normal ones will be overriden by tmp ones.
        for (Path tmpAnimFilePath : tmpAnimFilesToLoad) {
            boolean res = loadAnimFile(tmpAnimFilePath, errors);
            if (!res) {
                errors.add("Failed to load anim " + tmpAnimFilePath.getFileName().toString());
            }
        }
        return 0;
    }

    public static boolean loadAnimFile(String animName, List<String> errors) {
        Path animFilepath = FileStorage.getAnimationPathFromName(animName);
        if (!FileStorage.loadAnimFile(animFilepath, errors)) {
            Path tmpAnimFilepath = FileStorage.getTmpAnimationPathFromName(animName);
            return FileStorage.loadAnimFile(tmpAnimFilepath, errors);
        }
        return true;
    }

    private static boolean loadAnimFile(Path filePath, List<String> errors) {
        String fullFilename = filePath.getFileName().toString();
        String filename;
        if (fullFilename.endsWith(AnimTmpFileExtension)) {
            filename = fullFilename.substring(0, fullFilename.length() - (AnimFileExtension.length() + AnimTmpFileExtension.length()));
        }
        else if (fullFilename.endsWith(AnimFileExtension)) {
            filename = fullFilename.substring(0, fullFilename.length() - AnimFileExtension.length());
        }
        else {
            errors.add("File " + fullFilename + " is not an animation file (wrong extension)");
            return false;
        }
        System.out.println(filename);
        // Reload existing animation, except the one that is being created/edited if there is one
        List<Animation> animationsToRemove = new ArrayList<>();
        for (Animation existingAnimation : GlobalManager.animations) {
            if (existingAnimation.name.equals(filename) && existingAnimation != GlobalManager.curAnimation) {
                System.out.println("removing existing animation");
                animationsToRemove.add(existingAnimation);
            }
        }
        GlobalManager.animations.removeAll(animationsToRemove);

        FileInputStream fi;
        BufferedReader in;
        Scanner scanner;
        try {
            fi = new FileInputStream(filePath.toFile());
            in = new BufferedReader(new InputStreamReader(fi, StandardCharsets.UTF_8));
            scanner = new Scanner(in);
        } catch (IOException e) {
            errors.add("Failed to read directory where animations are stored (" + internalStoragePath.toString() + ")");
            return false;
        }

        String dimension;
        if (scanner.hasNextLine()) {
            dimension = scanner.nextLine();
        }
        else {
            errors.add("Empty animation file " + filename);
            return false;
        }

        ServerWorld animWorld = null;
        for (ServerWorld world : GlobalManager.server.getWorlds()) {
            System.out.println("SERVER WORLD " + world.getDimensionEntry().getKey().get().getValue().toString());
            if (world.getDimensionEntry().getKey().get().getValue().toString().equals(dimension)) {
                animWorld = world;
                break;
            }
        }
        if (animWorld == null) {
            errors.add("Unknown dimension " + dimension + " in file " + filename);
            return false;
        }
        Animation animation = new Animation(filename, animWorld);

        int lineNumber = 1;
        Frame curFrame = null;
        while(scanner.hasNextLine()) {
            ++lineNumber;
            String line = scanner.nextLine();
            if (line.isEmpty()) continue;
            String[] parts = line.split(" ");
            if (parts.length == 1) {
                try {
                    if (curFrame != null) {
                        animation.addFrame(curFrame);
                    }
                    curFrame = new Frame(animation, Integer.parseInt(parts[0]));
                }
                catch(NumberFormatException e) {
                    errors.add("Error while loading animation file " + filename + " : wrong tick value at line " + lineNumber);
                    return false;
                }
            }
            else if (parts.length == 4) {
                if (curFrame == null) {
                    errors.add("Error while loading animation file " + filename + " : file is corrupted.");
                    return false;
                }
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    String blockId = parts[3];
                    String[] idParts = blockId.split(":");
                    if (idParts.length != 2) {
                        errors.add("Wrong block identifier in animation file " + filename + " at line " + lineNumber);
                        return false;
                    }

                    Block block = Registries.BLOCK.get(Identifier.of(idParts[0], idParts[1]));
                    curFrame.blocks.add(new FrameBlock(block.getDefaultState(), new BlockPos(x,y,z)));
                }
                catch(NumberFormatException e) {
                    errors.add("Error while loading animation file " + filename + " : wrong coordinate value at line " + lineNumber);
                }
            }
        }
        if (curFrame != null) {
            animation.addFrame(curFrame);
        }
        scanner.close();

        System.out.println(animation);
        GlobalManager.addAnimation(animation);
        return true;
    }
}
