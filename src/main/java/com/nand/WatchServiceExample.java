package com.nand;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

public class WatchServiceExample {

    private final WatchService watchService;
    private final Map<WatchKey, Path> keyMap;

    public WatchServiceExample(Path dir) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.keyMap = new HashMap();
        traverse(dir);
    }

    private void registerDir(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keyMap.put(key, dir);
    }

    private void traverse(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDir(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void process() {
        for (; ; ) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keyMap.get(key);
            if (dir == null) {
                System.out.println("Key not found!!");
                continue;
            }

            pollEvents(key, dir);

            if (resetKey(key)) break;
        }
    }

    private boolean resetKey(WatchKey key) {
        boolean valid = key.reset();
        if (!valid) {
            keyMap.remove(key);

            if (keyMap.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void pollEvents(WatchKey key, Path dir) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();

            Path name = ((WatchEvent<Path>) event).context();
            Path child = dir.resolve(name);

            System.out.format("%s: %s\n", event.kind().name(), child);

            if (kind == ENTRY_CREATE) {
                try {
                    if (Files.isDirectory(child)) {
                        traverse(child);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new WatchServiceExample(Paths.get("C:/dev")).process();
    }
}
