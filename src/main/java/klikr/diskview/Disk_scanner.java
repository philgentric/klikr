package klikr.diskview;

import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Disk scanner using one virtual thread per folder.
 * <p>
 * Two modes:
 * <ul>
 *   <li><b>Fresh scan</b> ({@link #scan(File)}): full recursive scan, no cache.</li>
 *   <li><b>Cached re-scan</b> ({@link #scan(File, File_node)}): reuses the previous
 *       tree. For each directory whose mtime hasn't changed, skips the expensive
 *       {@code listFiles()} call but <b>always re-reads every file's size</b> via NIO
 *       — so a 1 KB file that grew to 25 GB is never missed.</li>
 * </ul>
 */

//*******************************************************
public class Disk_scanner
//*******************************************************
{

    private final LongAdder pending_folders = new LongAdder();
    private final AtomicInteger scanned_folders = new AtomicInteger(0);
    private final AtomicInteger skipped_folders = new AtomicInteger(0);
    public final Logger logger;

    //*******************************************************
    public Disk_scanner(Logger logger)
    //*******************************************************
    {
        this.logger = logger;
    }


    /** Fresh scan — no cache. */
    //*******************************************************
    public File_node scan(File rootFolder)
    //*******************************************************
    {
        return scan(rootFolder, null);
    }

    /**
     * Scan with optional cache. If {@code previousRoot} is non-null and points to the
     * same path, directories whose mtime hasn't changed will reuse the cached child list
     * (but file sizes are always re-read).
     */

    //*******************************************************
    public File_node scan(File rootFolder, File_node previousRoot)
    //*******************************************************
    {
        if (!rootFolder.isDirectory()) {
            logger.log("Disk_scanner: not a directory: " + rootFolder);
            return new File_node(rootFolder);
        }
        if (Files.isSymbolicLink(rootFolder.toPath())) {
            logger.log("Disk_scanner: refusing to follow symlink root: " + rootFolder);
            return new File_node(rootFolder);
        }

        // If previous root doesn't match this path, ignore it
        File_node cache = null;
        if (previousRoot != null && previousRoot.get_file().equals(rootFolder)) {
            cache = previousRoot;
        }

        File_node rootNode = new File_node(rootFolder);
        launch_folder_scan(rootNode, cache);

        // Block until all folder scans complete
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        while (pending_folders.sum() > 0) {
            try { Thread.sleep(10); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        compute_sizes(rootNode);

        logger.log("Disk_scanner complete: " + scanned_folders.get() + " scanned, "
                + skipped_folders.get() + " cache-hits (structure reused, sizes refreshed), total size=" + rootNode.get_size());
        return rootNode;
    }

    public int getScanned_folders() { return scanned_folders.get(); }
    public int getFoldersSkipped() { return skipped_folders.get(); }


    //*******************************************************
    private void launch_folder_scan(File_node dirNode, File_node cachedDir)
    //*******************************************************
    {
        pending_folders.increment();
        Actor_engine.execute(() -> {
            try {
                scan_one_folder(dirNode, cachedDir);
            } catch (Exception e) {
                logger.log("Disk_scanner error scanning " + dirNode.get_file() + ": " + e);
            } finally {
                pending_folders.decrement();
            }
        },"diskview folder scan",logger);
    }

    /**
     * Scan one directory.
     *
     * @param dirNode   the new node being built
     * @param cachedDir the previous scan's node for the same path (may be null)
     */

    //*******************************************************
    private void scan_one_folder(File_node dirNode, File_node cachedDir)
    //*******************************************************
    {
        File dir = dirNode.get_file();
        Path dirPath = dir.toPath();

        if (Files.isSymbolicLink(dirPath)) {
            dirNode.set_scanned(true);
            return;
        }

        // Read current directory mtime
        long currentMtime = 0;
        try {
            currentMtime = Files.getLastModifiedTime(dirPath).toMillis();
        } catch (IOException e) {
            // can't read mtime — force full scan of this dir
        }

        // ── Can we use the cache for this directory? ──
        // Conditions: we have a cached node, its mtime matches, AND it was fully scanned.
        boolean structureUnchanged = cachedDir != null
                && cachedDir.get_dir_mtime_ms() > 0
                && cachedDir.get_dir_mtime_ms() == currentMtime;

        if (structureUnchanged) {
            // CACHE HIT — reuse child list structure, but re-stat every file size
            reuse_structure_refresh_sizes(dirNode, cachedDir, currentMtime);
        } else {
            // CACHE MISS or no cache — full listFiles()
            full_scan(dirNode, cachedDir, currentMtime);
        }
    }

    /**
     * Cache hit path: the directory's structure (entries) hasn't changed.
     * We skip the expensive listFiles() call. Instead we walk the cached children:
     * - For files: re-read size via NIO (catches the 1KB → 25GB case)
     * - For directories: create new node, recurse with cached child as hint
     */

    //*******************************************************
    private void reuse_structure_refresh_sizes(File_node dir_node, File_node cached_dir, long current_mtime)
    //*******************************************************
    {
        skipped_folders.incrementAndGet();
        dir_node.set_dir_mtime_ms(current_mtime);

        for (File_node cachedChild : cached_dir.get_children()) {
            File childFile = cachedChild.get_file();

            // Verify the entry still exists (belt & suspenders — mtime said it should)
            if (!childFile.exists()) {
                // Entry vanished despite same mtime — rare but possible on FAT.
                // Fall back to full scan for this directory.
                dir_node.clear_children();
                full_scan(dir_node, cached_dir, current_mtime);
                return;
            }

            if (Files.isSymbolicLink(childFile.toPath())) {
                continue;
            }

            File_node childNode = new File_node(childFile);
            dir_node.add_child(childNode);

            if (childFile.isDirectory()) {
                // Recurse — pass cached subtree as hint
                launch_folder_scan(childNode, cachedChild);
            } else {
                // FILE: always re-read size — this is the whole point
                childNode.set_size(get_real_size(childFile));
            }
        }
        dir_node.set_scanned(true);
    }

    /**
     * Full scan path: listFiles(), build children from scratch.
     * If we have a cache, use it to provide hints for subdirectories.
     */

    //*******************************************************
    private void full_scan(File_node dirNode, File_node cachedDir, long currentMtime)
    //*******************************************************
    {
        File dir = dirNode.get_file();
        File[] entries = dir.listFiles();
        if (entries == null) {
            dirNode.set_scanned(true);
            return;
        }

        scanned_folders.incrementAndGet();
        dirNode.set_dir_mtime_ms(currentMtime);

        // Build a lookup map from cached children (if any) for subdirectory hints
        Map<String, File_node> cachedChildMap = null;
        if (cachedDir != null && !cachedDir.get_children().isEmpty()) {
            cachedChildMap = new HashMap<>();
            for (File_node c : cachedDir.get_children()) {
                cachedChildMap.put(c.get_file().getName(), c);
            }
        }

        for (File entry : entries) {
            if (Files.isSymbolicLink(entry.toPath())) {
                continue;
            }

            File_node childNode = new File_node(entry);
            dirNode.add_child(childNode);

            if (entry.isDirectory()) {
                // Look up cached subtree for this child directory
                File_node cachedChild = cachedChildMap != null
                        ? cachedChildMap.get(entry.getName()) : null;
                launch_folder_scan(childNode, cachedChild);
            } else {
                childNode.set_size(get_real_size(entry));
            }
        }
        dirNode.set_scanned(true);
    }

    //*******************************************************
    private static long get_real_size(File file)
    //*******************************************************
    {
        try {
            return Files.readAttributes(file.toPath(), BasicFileAttributes.class).size();
        } catch (IOException e) {
            return file.length();
        }
    }

    //*******************************************************
    private void compute_sizes(File_node node)
    //*******************************************************
    {
        if (!node.is_this_a_directory()) return;
        long total = 0;
        for (File_node child : node.get_children()) {
            compute_sizes(child);
            total += child.get_size();
        }
        node.set_size(total);
    }
}

