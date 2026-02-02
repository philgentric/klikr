package klikr.util.mmap;

import javafx.stage.Window;
import javafx.scene.image.Image;
import klikr.util.cache.Cache_folder;
import klikr.util.cache.Size_;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

//**********************************************************
public class Mmap
//**********************************************************
{
    public static volatile Mmap instance;

    private final Map<Integer,Piece> pieces = new ConcurrentHashMap<>();
    private final Map<String, Meta> main_index = new ConcurrentHashMap<>();
    private final Logger logger;
    private final int piece_size_in_megabytes;
    private final Path cache_folder;
    private final Path main_index_file;
    private final ArrayBlockingQueue<Boolean> save_queue = new ArrayBlockingQueue<>(1);


    private final static boolean stats_dbg = false;
    private final static boolean ultra_dbg = false;

    private final Map<String, Integer> usage = new ConcurrentHashMap<>();

    //**********************************************************
    public static Mmap get_instance(int piece_size_in_megabytes, Window owner, Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Mmap.class)
            {
                if (instance == null)
                {
                    instance = new Mmap( piece_size_in_megabytes, owner, logger);
                }
            }
        }
        return instance;
    }

    //**********************************************************
    private Mmap(int piece_size_in_megabytes, Window owner,Logger logger)
    //**********************************************************
    {
        this.piece_size_in_megabytes = piece_size_in_megabytes;
        this.logger = logger;
        cache_folder = Static_files_and_paths_utilities.get_cache_folder(Cache_folder.icon_cache, owner, logger);
        main_index_file = cache_folder.resolve("main_index");
        load_index();

        Runnable save = new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        Object marker = save_queue.poll(1, TimeUnit.SECONDS);
                        if (marker != null) {
                            // Take snapshot to avoid Issue #5
                            Map<String, Meta> snapshot = Map.copyOf(main_index);
                            util_save_index(snapshot, main_index_file, logger);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.log("Save failed: " + e.getMessage());
                    }
                }
            }
        };
        Actor_engine.execute(save,"mmap save",logger);

        if ( stats_dbg) {
            Runnable stats = new Runnable() {
                @Override
                public void run() {
                    for (; ; ) {
                        try {
                            Thread.sleep(20_000);
                            StringBuilder sb = new StringBuilder();
                            sb.append("****************************************\n");
                            for (Map.Entry<String, Meta> e : main_index.entrySet()) {
                                sb.append(e.getKey()).append(" type: ").append(e.getValue().getClass().getName()).append("\n");
                            }
                            sb.append("****************************************\n");
                            for (Map.Entry<String, Integer> e : usage.entrySet()) {
                                sb.append(e.getKey()).append(" used: ").append(e.getValue()).append("\n");
                            }
                            sb.append("****************************************\n");
                            logger.log(sb.toString());

                        } catch (InterruptedException e) {
                            logger.log("" + e);
                        }
                    }
                }
            };
            Actor_engine.execute(stats, "mmap stats", logger);
        }
    }


    //**********************************************************
    public void write_bytes(String tag, byte[] bytes, boolean and_save)
    //**********************************************************
    {
        if (ultra_dbg) logger.log("mmap write_bytes "+tag);
        Simple_metadata meta = find_room_for_bytes(tag,bytes);
        if ( meta == null )
        {
            logger.log("no room found for "+tag);
            return;
        }
        meta.piece().write_bytes(meta,tag,bytes);
        String key = tag;
        record_index(key, meta,meta.piece());
        if (ultra_dbg) logger.log("mmap write_file_internal WROTE: "+key);
        if (and_save)
        {
            save_index();
        }
    }


    //**********************************************************
    public void write_file(Path path, boolean and_save)
    //**********************************************************
    {
        if (ultra_dbg) logger.log("mmap write_file "+path);

        Simple_metadata meta = find_room_for_file(path);
        if ( meta == null )
        {
            logger.log("no room found for "+path);
            return;
        }
        meta.piece().write_file(meta,path);
        String key = path.toAbsolutePath().toString();
        record_index(key, meta,meta.piece());
        if (ultra_dbg) logger.log("mmap write_file_internal WROTE: "+key);
        if (and_save)
        {
            save_index();
        }
    }


    //**********************************************************
    private void record_index(String key, Meta meta, Piece piece)
    //**********************************************************
    {
        main_index.put(key, meta);
    }

    //**********************************************************
    public byte[] read_bytes(String tag)
    //**********************************************************
    {
        Simple_metadata sm = (Simple_metadata) main_index.get(tag);
        if ( sm == null)
        {
            logger.log("read_file failed: no metadata found for "+tag);
            return null;
        }
        Piece piece = sm.piece();
        if ( piece == null)
        {
            logger.log("read_file failed: no piece for "+tag);
            return null;
        }
        if ( stats_dbg)
        {
            usage.merge(tag, 1, Integer::sum);;
        }
        return piece.read_bytes(sm);
    }

    //**********************************************************
    public byte[] read_file(Path p)
    //**********************************************************
    {
        Simple_metadata sm = (Simple_metadata) main_index.get(p.toAbsolutePath().toString());
        if ( sm == null)
        {
            logger.log("read_file failed: no metadata found for "+p);
            return null;
        }
        Piece piece = sm.piece();
        if ( piece == null)
        {
            logger.log("read_file failed: no piece for "+p);
            return null;
        }
        if ( stats_dbg)
        {
            usage.merge(p.toAbsolutePath().toString(), 1, Integer::sum);;
        }
        return piece.read_bytes(sm);
    }



    // for animated gifs, javafx does not have a PixelReader... so we cache the FILE


    //**********************************************************
    public void write_image_as_file(Path path,  boolean and_save, Runnable on_end)
    //**********************************************************
    {
        Image_as_file_metadata meta = find_room_for_image_as_file(path);
        if ( meta == null ) return;
        String key = path.toAbsolutePath().toString();
        meta.piece().write_image_as_file(meta,path);
        record_index(key, meta, meta.piece());
        if (ultra_dbg) logger.log("mmap image as file: "+key);
        if (and_save)
        {
            save_index();
        }
        if ( on_end != null )
        {
            on_end.run();
        }
    }


    // takes more file space but faster to reload
    //**********************************************************
    public void write_image_as_pixels(String tag, Image image, boolean and_save, Runnable on_end)
    //**********************************************************
    {
        Image_as_pixel_metadata meta = find_room_for_image_as_pixel(image, tag);
        if ( meta == null ) return;
        meta.piece().write_image_as_pixels(meta.offset(),image);
        record_index(tag, meta, meta.piece());
        if (ultra_dbg) logger.log("mmap image as pixel: "+tag);
        if (and_save)
        {
            save_index();
        }
        if ( on_end != null )
        {
            on_end.run();
        }
    }

    private static ConcurrentLinkedQueue<Long> elapseds = new ConcurrentLinkedQueue<>();
    private static int counter = 0;
    //**********************************************************
    public Image read_image_as_pixels(String tag)
    //**********************************************************
    {
        long start = System.currentTimeMillis();
        Image_as_pixel_metadata meta = (Image_as_pixel_metadata) main_index.get(tag);
        if ( meta == null ) return null;
        //logger.log("mmap reading image: "+tag+" is pixels=yes");
        Piece p = meta.piece();
        if (p == null) return null;
        if ( stats_dbg)
        {
            usage.merge(tag, 1, Integer::sum);;
        }
        Image returned = p.read_image_as_pixels(meta);
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        elapseds.add(elapsed);
        counter++;
        if ( counter%10 == 0)
        {
            double tot = 0;
            for ( long l : elapseds ) tot += l;
            logger.log(" average READ "+tot/(double)counter+" ms");
        }
        return returned;
    }
    //**********************************************************
    public Image read_image_as_file(Path path)
    //**********************************************************
    {
        String tag = path.toAbsolutePath().toString();
        Image_as_file_metadata meta = (Image_as_file_metadata) main_index.get(tag);
        if ( meta == null ) return null;
        if (ultra_dbg) logger.log("mmap reading image: "+tag+" is pixels=no");
        Piece p = meta.piece();
        if (p == null) return null;
        if ( stats_dbg)
        {
            usage.merge(tag, 1, Integer::sum);;
        }
        return p.read_image_as_file(tag, meta);
    }




    //**********************************************************
    public synchronized double clear_cache()
    //**********************************************************
    {
        double d = Size_.of_Map(main_index,Size_.of_String_F(),meta -> 8L);
        main_index.clear();
        if( stats_dbg) usage.clear();
        for ( Piece piece : pieces.values() ) piece.clear_cache();
        save_index();
        return d;
    }

    //**********************************************************
    public MemorySegment get_MemorySegment(String tag)
    //**********************************************************
    {
        Simple_metadata sm = (Simple_metadata) main_index.get(tag);
        Piece p = sm.piece();
        if (p == null) return null;
        return p.get_MemorySegment(sm);
    }



    static final byte SIMPLE_META = 0x01;
    static final byte IMAGE_PIXEL_META = 0x02;
    static final byte IMAGE_FILE_META = 0x03;
    //**********************************************************
    private static void util_save_index(Map<String,Meta> local, Path index_file, Logger logger)
    //**********************************************************
    {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(index_file.toFile())))
        {
            logger.log("index size ="+local.size());
            dos.writeInt(local.size());
            for (Map.Entry<String, Meta> entry : local.entrySet())
            {
                Meta meta = entry.getValue();
                if (meta instanceof Simple_metadata simple)
                {
                    if ( ultra_dbg) logger.log("writing Simple_metadata ="+entry.getKey());

                    dos.writeInt(simple.piece().who_are_you);
                    dos.write(SIMPLE_META);
                    dos.writeUTF(entry.getKey());
                    dos.writeLong(simple.offset());
                    dos.writeLong(simple.length());
                }
                else if (meta instanceof Image_as_pixel_metadata iapm)
                {
                    if ( ultra_dbg) logger.log("writing Image_as_pixel_metadata ="+entry.getKey());

                    dos.writeInt(iapm.piece().who_are_you);
                    dos.write(IMAGE_PIXEL_META);
                    dos.writeUTF(entry.getKey());
                    dos.writeLong(iapm.offset());
                    dos.writeInt(iapm.width());
                    dos.writeInt(iapm.height());
                }
                else if (meta instanceof Image_as_file_metadata isfm)
                {
                    logger.log("writing Image_as_file_metadata ="+entry.getKey());

                    dos.writeInt(isfm.piece().who_are_you);
                    dos.write(IMAGE_FILE_META);
                    dos.writeUTF(entry.getKey());
                    dos.writeLong(isfm.offset());
                    dos.writeLong(isfm.length());
                }
            }
            dos.flush();
            logger.log("Index saved with " + local.size() + " entries.");
        }
        catch (FileNotFoundException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
    }
    //**********************************************************
    public void load_index()
    //**********************************************************
    {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(main_index_file.toFile())))
        {
            int size = dis.readInt();
            for (int i = 0; i < size; i++)
            {
                int piece_ID = dis.readInt();
                Piece local = pieces.get(piece_ID);
                if(  local == null )
                {
                    local = new Piece(piece_ID,cache_folder,logger);
                    pieces.put(piece_ID,local);
                }
                byte type = dis.readByte();
                String key = dis.readUTF();
                long offset = dis.readLong();
                if ( type == SIMPLE_META )
                {
                    long length = dis.readLong();
                    Meta m = new Simple_metadata(pieces.get(piece_ID),key,offset,length);
                    main_index.put(key,m);
                    if (ultra_dbg) logger.log("cached item reloaded from file: "+key);
                }
                else if ( type == IMAGE_PIXEL_META )
                {
                    int width = dis.readInt();
                    int height = dis.readInt();
                    Meta m = new Image_as_pixel_metadata(pieces.get(piece_ID),key,offset,width,height);
                    main_index.put(key,m);
                    if (ultra_dbg) logger.log("cached item reloaded from file: "+key);
                }
                else if ( type == IMAGE_FILE_META )
                {
                    long length = dis.readLong();
                    Meta m = new Image_as_file_metadata(pieces.get(piece_ID),key,offset,length);
                    main_index.put(key,m);
                    if (ultra_dbg) logger.log("cached item reloaded from file: "+key);
                }
            }
            logger.log("Index local with " + main_index.size() + " entries.");
        }
        catch (FileNotFoundException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        catch (IOException e)
        {
            // if cache is corrupted, clear the index
            main_index.clear();
            pieces.clear();
            try {
                Files.deleteIfExists(main_index_file);
            } catch (IOException ee) {
                logger.log("Could not delete index file: " + ee.getMessage());
            }
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        // we can init only after everything is reloaded
        for (Piece piece : pieces.values())
        {
            Map<String,Meta> local = new HashMap<>();
            for ( Map.Entry<String, Meta> entry : main_index.entrySet() )
            {
                Meta m = entry.getValue();
                Piece p = m.piece();
                if ( p == piece ) local.put(entry.getKey(),m);
            }
            piece.init(local, piece_size_in_megabytes);
        }
    }



    //**********************************************************
    public void save_index()
    //**********************************************************
    {
        save_queue.offer(true );
    }

    //**********************************************************
    private Image_as_pixel_metadata find_room_for_image_as_pixel(Image image, String tag)
    //**********************************************************
    {
        long size= (long) (image.getWidth() * image.getHeight() * 4);
        Room room = find_room(size);
        if ( room == null) return null;
        return new Image_as_pixel_metadata(room.piece(), tag, room.offset(), (int) image.getWidth(), (int) image.getHeight());
    }

    //**********************************************************
    private Image_as_file_metadata find_room_for_image_as_file(Path path)
    //**********************************************************
    {
        long length = path.toFile().length();
        Room room = find_room(length);
        if ( room == null) return null;
        return new Image_as_file_metadata(room.piece(), path.toAbsolutePath().toString(), room.offset(), length);
    }

    //**********************************************************
    private Simple_metadata find_room_for_bytes(String tag, byte[] bytes)
    //**********************************************************
    {
        long size = bytes.length;
        Room room = find_room(size);
        if ( room == null)
        {
            logger.log("find_room_for_file failed for "+tag);
            return null;
        }
        return new Simple_metadata(room.piece(), tag, room.offset(), size);
    }


    //**********************************************************
    private Simple_metadata find_room_for_file(Path path)
    //**********************************************************
    {
        long size = path.toFile().length();
        Room room = find_room(size);
        String tag = path.toAbsolutePath().toString();
        if ( room == null)
        {
            logger.log("find_room_for_file failed for "+tag);
            return null;
        }
        return new Simple_metadata(room.piece(), tag, room.offset(), size);
    }

    record Room(Piece piece, long offset){}

    //**********************************************************
    private Room find_room(long length)
    //**********************************************************
    {
        for ( Piece piece : pieces.values() )
        {
            long offset = piece.has_room(length);
            if ( offset>= 0 )
            {
                return new Room(piece,offset);
            }
        }


        int next_index = pieces.size();
        for(;;)
        {
            Piece piece = pieces.computeIfAbsent(next_index, index -> {
                Piece p = new Piece(index, cache_folder, logger);
                p.init(new HashMap<>(), piece_size_in_megabytes);
                return p;
            });
            long offset = piece.has_room(length);
            if (offset >= 0)
            {
                return new Room(piece, offset);
            }
            if (length > (long) piece_size_in_megabytes * 1024 * 1024)
            {
                logger.log("Item too large for any piece: " + length + " bytes (limit: " + piece_size_in_megabytes + " MB)");
                return null;
            }
            next_index++;
        }
    }


}
