package klikr.diskview;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//*******************************************************
public class File_node
//*******************************************************
{
    private final File file;
    private volatile long size;
    private final List<File_node> children;
    private final boolean is_directory;
    private volatile boolean scanned = false;

    // Cache metadata: directory mtime at the time of scanning.
    // Used to detect structural changes (files added/removed/renamed).
    private volatile long dir_mtime_ms = 0;

    public File_node(File file) {
        this(file, file.isDirectory());
    }

    /**
     * Constructor that accepts isDirectory directly — avoids the stat() syscall.
     * Used by Scan_cache deserialization for fast loading.
     */
    //*******************************************************
    public File_node(File file, boolean isDirectory)
    //*******************************************************
    {
        this.file = file;
        this.is_directory = isDirectory;
        this.children = isDirectory ? Collections.synchronizedList(new ArrayList<>()) : Collections.emptyList();
        this.size = 0;
        if (!isDirectory) {
            this.scanned = true;
        }
    }

    //*******************************************************
    public void add_child(File_node child)
    //*******************************************************
    {
        if (is_directory) {
            children.add(child);
        }
    }

    public void set_size(long size) {
        this.size = size;
    }

    public long get_size() {
        return size;
    }

    public File get_file() {
        return file;
    }

    public boolean is_this_a_directory() {
        return is_directory;
    }

    //*******************************************************
    public List<File_node> get_children()
    //*******************************************************
    {
        if (!is_directory) return Collections.emptyList();
        synchronized (children) {
            return new ArrayList<>(children);
        }
    }

    public void set_scanned(boolean scanned) {
        this.scanned = scanned;
    }

    public long get_dir_mtime_ms() {
        return dir_mtime_ms;
    }

    public void set_dir_mtime_ms(long millis) {
        this.dir_mtime_ms = millis;
    }

    /**
     * Find a child node by filename. Used by cached re-scan to match
     * existing children against the current directory listing.
     */
    //*******************************************************
    public File_node find_child(String name)
    //*******************************************************
    {
        for (File_node child : children) {
            if (child.get_file().getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    //*******************************************************
    public void clear_children()
    //*******************************************************
    {
        if (is_directory) {
            children.clear();
        }
    }
}
