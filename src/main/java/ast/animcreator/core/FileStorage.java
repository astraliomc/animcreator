package ast.animcreator.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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
    public static String animFilesStorageDirName = "animations";
    public static String seqFilesStorageDirName = "sequences";
    public static Path modPath;
    public static Path animFilesStoragePath;
    public static Path seqFilesStoragePath;

    public final static String AnimFileExtension = ".anim";
    public final static String AnimTmpFileExtension = ".tmp";
    public final static String SequenceFileExtension = ".seq";

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

        animFilesStoragePath = Path.of(modPath + "/" + animFilesStorageDirName);
        if (!Files.exists(animFilesStoragePath)) {
            try {
                Files.createDirectory(animFilesStoragePath);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        seqFilesStoragePath = Path.of(modPath + "/" + seqFilesStorageDirName);
        if (!Files.exists(seqFilesStoragePath)) {
            try {
                Files.createDirectory(seqFilesStoragePath);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean animExistsOnDisk(String animName) {
        return Files.exists(Path.of(animFilesStoragePath + "/" + animName + AnimFileExtension)) ||
                Files.exists(Path.of(animFilesStoragePath + "/" + animName + AnimFileExtension + AnimTmpFileExtension));
    }

    public static Path getAnimationPathFromName(String animName) {
        Path animPath = Path.of(animFilesStoragePath + "/" + animName + AnimFileExtension);
        if (Files.exists(animPath)) {
            return animPath;
        }
        Path tmpAnimPath = Path.of(animPath + AnimTmpFileExtension);
        if (Files.exists(tmpAnimPath)) {
            return tmpAnimPath;
        }
        return null;
    }

    public static Path getSequencePathFromName(String seqName) {
        Path animPath = Path.of(seqFilesStoragePath + "/" + seqName + SequenceFileExtension);
        if (Files.exists(animPath)) {
            return animPath;
        }
        return null;
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

                String blockStateStr = frameBlock.blockState.getBlock().getStateWithProperties(frameBlock.blockState).toString();
                StringBuilder fileBlockStateStr = new StringBuilder(blockStateStr.length());
                fileBlockStateStr.append(frameBlock.blockState.getRegistryEntry().getIdAsString());
                int stateStartIndex = blockStateStr.indexOf("[");
                if (stateStartIndex != -1) {
                    fileBlockStateStr.append(blockStateStr.substring(blockStateStr.indexOf("[")));
                }
                fileContent.append(fileBlockStateStr);
                fileContent.append('\n');
            }
        }

        FileWriter fr;
        Path animFilePath = Path.of(animFilesStoragePath + "/" + animation.name + AnimFileExtension + (isTemporary ? AnimTmpFileExtension : ""));
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
            Path tmpAnimFilePath = Path.of(animFilesStoragePath + "/" + animation.name + AnimFileExtension + AnimTmpFileExtension);
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
            Stream<Path> stream = Files.list(animFilesStoragePath);
            System.out.println("STREAM DECL");
            stream.forEach((path) -> {
                String fullFileName = path.getFileName().toString();
                System.out.println("LOAD FILE " + fullFileName);
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
        Path animFilepath = getAnimationPathFromName(animName);
        if (animFilepath == null) {
            errors.add("Animation " + animName + "does not exist on disk.");
            return false;
        }
        return FileStorage.loadAnimFile(animFilepath, errors);
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
        // Reload existing animation, except the one that is being created/edited if there is one
        List<Animation> animationsToRemove = new ArrayList<>();
        for (Animation existingAnimation : GlobalManager.animations) {
            if (existingAnimation.name.equals(filename) && existingAnimation != GlobalManager.curAnimation) {
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
            errors.add("Failed to read directory where animations are stored (" + animFilesStoragePath.toString() + ")");
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
                    String blockStateStr = parts[3];

                    BlockStateArgumentType argType = new BlockStateArgumentType(GlobalManager.commandRegistryAccess);
                    try {
                        BlockStateArgument arg =  argType.parse(new StringReader(blockStateStr));
                        curFrame.blocks.add(new FrameBlock(arg.getBlockState(), new BlockPos(x,y,z)));
                    }
                    catch(CommandSyntaxException e) {
                        System.out.println("Invalid block state in animation file " + filename + " at line " + lineNumber);
                    }

                }
                catch(NumberFormatException e) {
                    errors.add("Error while loading animation file " + filename + " : wrong coordinate value at line " + lineNumber);
                }
            }
            else {
                errors.add("Syntax error in animation file " + filename + " at line " + lineNumber + " : wrong number of elements.");
                return false;
            }
        }
        if (curFrame != null) {
            animation.addFrame(curFrame);
        }
        scanner.close();

        GlobalManager.addAnimation(animation);
        return true;
    }

    public static boolean deleteAnimFile(String animName, List<String> errors) {
        Path animFilePath = FileStorage.getAnimationPathFromName(animName);
        if (animFilePath == null) {
            errors.add("Animation " + animName + "does not exist on disk.");
            return false;
        }

        try {
            Files.delete(animFilePath);
        }
        catch(IOException e) {
            e.printStackTrace();
            errors.add("Internal error while removing animation file.");
            return false;
        }
        return true;
    }

    public static boolean renameAnimFile(String oldName, String newName, List<String> errors) {
        Path oldAnimFilePath = FileStorage.getAnimationPathFromName(oldName);
        if (oldAnimFilePath == null) {
            errors.add("Animation " + oldName + "does not exist on disk.");
            return false;
        }

        String newPathStr = animFilesStoragePath + "/" + newName + AnimFileExtension;
        if (oldAnimFilePath.getFileName().toString().endsWith(AnimTmpFileExtension)) {
            newPathStr += AnimTmpFileExtension;
        }

        Path newAnimFilePath = Path.of(newPathStr);
        try {
            Files.move(oldAnimFilePath, newAnimFilePath);
        }
        catch(IOException e) {
            e.printStackTrace();
            errors.add("Internal error while renaming animation file.");
            return false;
        }
        return true;
    }

    public static int loadAllSeqFiles(List<String> errors) {
        errors.clear();
        try {
            Stream<Path> stream = Files.list(seqFilesStoragePath);
            stream.forEach((path) -> {
                String fullFileName = path.getFileName().toString();
                if (fullFileName.endsWith(SequenceFileExtension)) {
                    Sequence seq = loadSeqFile(path, errors);
                    if (seq != null) {
                        SeqPlayer.sequencestoPlay.add(seq);
                    }
                    else {
                        errors.add("Failed to load sequence " + path.getFileName().toString());
                    }
                }
            });
        } catch (IOException e) {
            return -1;
        }
        return 0;
    }

    public static Sequence loadSeqFile(String seqName, List<String> errors) {
        Path seqFilepath = getSequencePathFromName(seqName);
        if (seqFilepath == null) {
            errors.add("Sequence " + seqName + "does not exist on disk.");
            return null;
        }
        return FileStorage.loadSeqFile(seqFilepath, errors);
    }

    private static Sequence loadSeqFile(Path filePath, List<String> errors) {
        String fullFilename = filePath.getFileName().toString();
        String filename;
        if (fullFilename.endsWith(SequenceFileExtension)) {
            filename = fullFilename.substring(0, fullFilename.length() - (SequenceFileExtension.length()));
        }
        else {
            errors.add("File " + fullFilename + " is not an sequence file (wrong extension)");
            return null;
        }

        FileInputStream fi;
        BufferedReader in;
        Scanner scanner;
        try {
            fi = new FileInputStream(filePath.toFile());
            in = new BufferedReader(new InputStreamReader(fi, StandardCharsets.UTF_8));
            scanner = new Scanner(in);
        } catch (IOException e) {
            errors.add("Failed to read directory where animations are stored (" + animFilesStoragePath.toString() + ")");
            return null;
        }

        Sequence seq = new Sequence(filename);
        int lineNumber = 1;
        while(scanner.hasNextLine()) {
            ++lineNumber;
            String line = scanner.nextLine();
            if (line.isEmpty()) continue;
            String[] parts = line.split(" ");
            if (parts.length != 2 && parts.length != 3) {
                errors.add("Syntax error in sequence file " + filename + " at line " + lineNumber + " : wrong number of elements.");
                return null;
            }
            String animName = parts[0];
            Animation animation = GlobalManager.getAnimationFromName(animName);
            if (animation == null) {
                errors.add("Animation " + animName + " does not exist. Error at line " + lineNumber);
                return null;
            }
            int tick;
            try {
                tick = Integer.parseUnsignedInt(parts[1]);
            }
            catch(NumberFormatException e) {
                errors.add("Error while loading sequence file " + filename + " : wrong tick at line " + lineNumber);
                return null;
            }

            boolean loop = false;
            if (parts.length == 3) {
                loop = Boolean.parseBoolean(parts[2]);
            }
            if (!seq.addAnimToSequence(animation, tick, loop)) {
                errors.add("Animation " + animName + " is invalid. Error at line " + lineNumber);
                return null;
            }
        }
        scanner.close();
        System.out.println("FINAL SEQ");
        System.out.println(seq);
        return seq;
    }
}

