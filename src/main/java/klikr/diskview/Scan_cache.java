package klikr.diskview;

import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence engine for the File_node tree.
 * Serializes the entire tree to a binary file using DataOutputStream,
 * stored in a hidden directory under the user's home folder.
 * Cache file is keyed by the scanned root path.
 */
//*******************************************************
public class Scan_cache
//*******************************************************
{

    private static final String CACHE_DIR_NAME = ".disksize_cache";
    private static final int MAGIC = 0x44534B53; // "DSKS"
    private static final int VERSION = 1;

    //*******************************************************
    private static Path get_cache_dir(Logger logger)
    //*******************************************************
    {
        String home = System.getProperty("user.home");
        if (home == null) return null;
        Path dir = Path.of(home, CACHE_DIR_NAME);
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return dir;
        } catch (IOException e) {
            logger.log("Scan_cache: cannot create cache dir " + dir + ": " + e);
            return null;
        }
    }

    //*******************************************************
    private static Path cacheFileFor(File rootFolder, Logger logger)
    //*******************************************************
    {
        Path dir = get_cache_dir(logger);
        if (dir == null) return null;
        String key = rootFolder.getAbsolutePath();
        String fileName = "scan_" + UUID.nameUUIDFromBytes(key.getBytes()) + ".bin";
        return dir.resolve(fileName);
    }

    // ────────────────────── Save ──────────────────────

    //*******************************************************
    public static void save_in_a_thread(File_node root, Logger logger)
    //*******************************************************
    {
        if (root == null) return;
        Actor_engine.execute(() -> save(root,logger),"diskview scan save",logger);
    }

    //*******************************************************
    public static void save(File_node root,Logger logger)
    //*******************************************************
    {
        if (root == null) return;
        Path cacheFile = cacheFileFor(root.get_file(),logger);
        if (cacheFile == null) return;

        long start = System.currentTimeMillis();
        Path tmpFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tmpFile), 256 * 1024))) {
            dos.writeInt(MAGIC);
            dos.writeInt(VERSION);
            dos.writeUTF(root.get_file().getAbsolutePath());
            int nodeCount = writeTree(dos, root,logger);
            dos.flush();

            Files.move(tmpFile, cacheFile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            long elapsed = System.currentTimeMillis() - start;
            long fileSize = Files.size(cacheFile);
            logger.log("Scan_cache: saved " + nodeCount + " nodes to "
                    + cacheFile.getFileName() + " (" + (fileSize / 1024) + " KB) in " + elapsed + " ms");
        } catch (IOException e) {
            logger.log("Scan_cache: save failed: " + e);
            try { Files.deleteIfExists(tmpFile); } catch (IOException ignored) {}
        }
    }

    //*******************************************************
    private static int writeTree(DataOutputStream dos, File_node root, Logger logger)
    //*******************************************************
    {
        Deque<File_node> stack = new ArrayDeque<>();
        stack.push(root);
        int count = 0;
        try {
            while (!stack.isEmpty()) {
                File_node node = stack.pop();
                dos.writeUTF(node.get_file().getAbsolutePath());
                dos.writeBoolean(node.is_this_a_directory());
                dos.writeLong(node.get_size());
                dos.writeLong(node.get_dir_mtime_ms());
                java.util.List<File_node> children = node.get_children();
                dos.writeInt(children.size());
                count++;
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }
        catch (IOException e) {
            logger.log(""+e);
        }
        return count;
    }

    /**
     * Load the entire tree in a single fast flat loop.
     * No filesystem calls — uses File_node(File, boolean) constructor.
     */
    //*******************************************************
    public static File_node load(File rootFolder, Logger logger)
    //*******************************************************
    {
        Path cacheFile = cacheFileFor(rootFolder, logger);
        if (cacheFile == null || !Files.exists(cacheFile)) return null;

        long start = System.currentTimeMillis();

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(cacheFile), 256 * 1024))) {

            int magic = dis.readInt();
            if (magic != MAGIC) {
                logger.log("Scan_cache: bad magic in " + cacheFile.getFileName());
                return null;
            }
            int version = dis.readInt();
            if (version != VERSION) {
                logger.log("Scan_cache: unsupported version " + version);
                return null;
            }
            String storedPath = dis.readUTF();
            if (!storedPath.equals(rootFolder.getAbsolutePath())) {
                logger.log("Scan_cache: path mismatch");
                return null;
            }

            // Read root
            Optional<File_node> op = read_one_node(dis, logger);
            if ( op.isEmpty()) return null;
            File_node root = op.get();
            int rootChildCount = dis.readInt();
            if (rootChildCount < 0 || rootChildCount > 10_000_000)
            {
                logger.log("Suspect root child count: " + rootChildCount);
                return null;
            }

            // Single flat loop — read all nodes
            Deque<File_node> node_stack = new ArrayDeque<>();
            Deque<int[]> count_stack = new ArrayDeque<>();

            if (rootChildCount > 0) {
                node_stack.push(root);
                count_stack.push(new int[]{rootChildCount});
            }

            while (!node_stack.isEmpty()) {
                int[] remaining = count_stack.peek();
                if (remaining[0] <= 0) {
                    node_stack.pop();
                    count_stack.pop();
                    continue;
                }

                File_node parent = node_stack.peek();
                File_node child = read_one_node(dis,logger).orElse(null);
                int cc = dis.readInt();
                if (cc < 0 || cc > 10_000_000) {
                    logger.log("Suspect child count: " + cc);
                    return null;
                }

                parent.add_child(child);
                remaining[0]--;

                if (cc > 0) {
                    node_stack.push(child);
                    count_stack.push(new int[]{cc});
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            long fileSize = Files.size(cacheFile);
            logger.log("Scan_cache: loaded from " + cacheFile.getFileName()
                    + " (" + (fileSize / 1024) + " KB) in " + elapsed + " ms");
            return root;

        } catch (IOException | RuntimeException e) {
            logger.log("Scan_cache: load failed: " + e);
            try { Files.deleteIfExists(cacheFile); } catch (IOException ignored) {}
            return null;
        }
    }

    //*******************************************************
    private static Optional<File_node> read_one_node(DataInputStream dis, Logger logger)
    //*******************************************************
    {
        try {
            String path = dis.readUTF();
            boolean isDirectory = dis.readBoolean();
            long size = dis.readLong();
            long dirMtime = dis.readLong();
            File_node node = new File_node(new File(path), isDirectory);
            node.set_size(size);
            node.set_dir_mtime_ms(dirMtime);
            node.set_scanned(true);
            return Optional.of(node);
        }
        catch (IOException e) {
            logger.log(""+e);
        }
        return Optional.empty();
    }
}
