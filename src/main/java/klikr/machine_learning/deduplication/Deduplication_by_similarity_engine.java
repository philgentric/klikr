// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../experimental/deduplicate/manual/Stage_with_2_images.java
//SOURCES ../../experimental/deduplicate/console/Deduplication_console_window.java
//SOURCES ../../experimental/deduplicate/manual/Againor.java
//SOURCES ./Runnable_for_finding_duplicate_file_pairs_similarity.java
//SOURCES ./Similarity_file_pair.java

package klikr.machine_learning.deduplication;

import javafx.application.Application;
import klikr.browsers.browser_core.icons.image_properties_cache.Image_properties;
import klikr.util.Kontext;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browsers.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.util.deduplicate.Abortable;
import klikr.util.deduplicate.console.Deduplication_console_window;
import klikr.util.deduplicate.manual.Againor;
import klikr.util.ui.Stage_with_2_images;
import klikr.machine_learning.feature_vector.Feature_vector_cache;
import klikr.machine_learning.similarity.Similarity_file_pair;
import klikr.util.files_and_paths.*;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Popups;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

//**********************************************************
public class Deduplication_by_similarity_engine implements Againor, Abortable
//**********************************************************
{
    private final Supplier<Feature_vector_cache> fv_cache_supplier;
    BlockingQueue<Similarity_file_pair> same_file_pairs_input_queue = new LinkedBlockingQueue<>();
    LongAdder threads_in_flight = new LongAdder();
    LongAdder duplicates_found = new LongAdder();
    File target_dir;
    Deduplication_console_window console_window;
    boolean end_reported = false;

    private final Kontext context;
    Stage_with_2_images stage_with_2_images;
    private final Klikr_cache<Path, Image_properties> image_properties_cache; // may be null, if not only image of same length are considered

    private final boolean looking_for_images;
    private final double too_far_away;
    private final Path_list_provider path_list_provider;
    private final Path_comparator_source path_comparator_source;
    private final Application application;
    //**********************************************************
    public Deduplication_by_similarity_engine(
            Application application,
            boolean looking_for_images, // if false, we look for songs
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            double too_far_away,
            File target_dir_,
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Supplier<Feature_vector_cache> fv_cache_supplier,
            Kontext k)
    //**********************************************************
    {
        this.application = application;
        this.looking_for_images = looking_for_images;
        this.path_list_provider = path_list_provider;
        this.path_comparator_source = path_comparator_source;
        this.too_far_away = too_far_away;
        this.image_properties_cache = image_properties_cache;
        this.fv_cache_supplier = fv_cache_supplier;
        Aborter context = new Aborter("Deduplication_engine",k.logger());
        this.context = new Kontext(k.owner(), context,k.logger());
        target_dir = target_dir_;
    }


    //**********************************************************
    public void do_your_job()
    //**********************************************************
    {
        context.log("Deduplication::look_for_all_files()");


        console_window = new Deduplication_console_window(
                this,
                "Looking for similar pictures in:" + target_dir.getAbsolutePath(),
                800,
                800,
                false,
                context);

        Runnable r = this::runnable_deduplication;
        Actor_engine.execute(r,"Deduplicate by similarity", context.logger());
        context.log("Deduplication::look_for_all_files() runnable_deduplication thread launched");
    }


    //**********************************************************
    @Override // Abortable
    public void abort(String reason)
    //**********************************************************
    {
        context.log("Deduplication::abort() Reason: " + reason);
        console_window.set_end_deleted();
        context.abort("Deduplication::abort() Reason: " + reason);
        if ( stage_with_2_images!=null) stage_with_2_images.close();
    }

    //**********************************************************
    private void runnable_deduplication()
    //**********************************************************
    {
        find_duplicate_pairs();

        if (!wait_for_finder_to_find_something())
        {
            context.log("wait_for_finder_to_find_something returns false");
            return;
        }
        again();
    }

    //**********************************************************
    private void find_duplicate_pairs()
    //**********************************************************
    {
        List<File_with_a_few_bytes> files = null;
        if ( looking_for_images)
        {
            files =  get_all_images();

        }
        else
        {
            files = get_all_songs();
        }
        //for(File_with_a_few_bytes mf : files) context.log(mf.file.getAbsolutePath());
        context.log("Deduplication::runnable_deduplication found a total of "+files.size()+ " files");

        console_window.set_status_text("Found " + files.size() + " files ... comparison by similarity started...");
        console_window.total_files_to_be_examined.add(files.size());

        long pairs = (long)files.size()*((long)files.size()-1L);
        pairs /= 2L;
        console_window.total_pairs_to_be_examined.add(pairs);

        // launch actor (feeder) in another tread
        Runnable_for_finding_duplicate_file_pairs_similarity duplicate_finder =
                new Runnable_for_finding_duplicate_file_pairs_similarity(
                        File_with_a_few_bytes.convert_to_paths(files),
                        too_far_away,
                        image_properties_cache, // maybe null
                        fv_cache_supplier,
                        path_comparator_source,
                        this,
                        files,
                        same_file_pairs_input_queue,
                        context);
        Actor_engine.execute(duplicate_finder,"Deduplicate by similarity (2)", context.logger());

        context.log("Deduplication::runnable_deduplication thread launched");
    }

    //**********************************************************
    private boolean are_threaded_finders_finished()
    //**********************************************************
    {
        if ( threads_in_flight.doubleValue() == 0) return true;
        return false;
    }



    //**********************************************************
    private boolean wait_for_finder_to_find_something()
    //**********************************************************
    {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            context.log("deduplicate ALL: sleep interrupted");
        }
        // wait for feeder to find something, initially
        // so that the pump does not early stop
        // max 200 seconds
        for (int i = 0; i < 2000; i++)
        {
            if (context.should_abort()) {
                context.log("Deduplicator::deduplicate_all abort");
                return false;
            }
            Similarity_file_pair p = same_file_pairs_input_queue.peek();
            if (p != null) return true;

            if ( are_threaded_finders_finished())
            {
                context.log("wait_for_finder_to_find_something: FINISHED, there was nothing to find ");

                abort("deduplication by similarity : all search threads finished");
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                context.log("deduplicate ALL: sleep interrupted");
            }

        }
        context.log("wait_for_finder_to_find_something: done ");
        return true;
    }


    //**********************************************************
    private void ask_user_about_a_duplicate_pair(Similarity_file_pair sim_file_pair)
    //**********************************************************
    {
        context.log("Similar:" + sim_file_pair.file_pair().f1().getAbsolutePath() + "-" + sim_file_pair.file_pair().f2().getAbsolutePath());

        Againor local_againor = this;
        Jfx_batch_injector.inject(() -> {
            String similarity = ""+sim_file_pair.similarity();
            if ( stage_with_2_images == null) stage_with_2_images = new Stage_with_2_images(application,similarity,sim_file_pair.file_pair(), local_againor, console_window.count_deleted, path_list_provider, path_comparator_source,context);
            else stage_with_2_images.set_pair(similarity,sim_file_pair.file_pair(),path_list_provider, path_comparator_source, context.aborter());
        },context);
    }


    //**********************************************************
    @Override
    public void again()
    //**********************************************************
    {
        if ( context.should_abort()) return;

        context.log("manual deduplicator: again called !");
        Runnable r = () -> {
            // this loop is only to manage the 3 second timeout on the queue
            for(;;)
            {
                if ( context.should_abort()) return;
                Similarity_file_pair p;
                try
                {
                    p = same_file_pairs_input_queue.poll(3, TimeUnit.SECONDS);
                }
                catch (InterruptedException e)
                {
                    context.log("" + e);
                    return;
                }
                if (p != null)
                {
                    context.log("manual deduplicator: ask_user_about_a_duplicate_pair called !");
                    if (!p.file_pair().f1().exists())
                    {
                        context.log("skipping search result because " + p.file_pair().f1().getAbsolutePath() + " does not exist anymore");
                        continue;
                    }
                    if (!p.file_pair().f2().exists()) {
                        context.log("skipping search result because " + p.file_pair().f2().getAbsolutePath() + " does not exist anymore");
                        continue;
                    }

                    ask_user_about_a_duplicate_pair(p);
                    return;
                }
                // p == null means timeout
                if (are_threaded_finders_finished())
                {
                    context.log("\nduplicate finder is finished !!");
                    if (!end_reported) {
                        Popups.popup_warning( "Search for duplicates ENDED", "(no duplicates found)", true,context);
                        end_reported = true;
                    }
                    console_window.set_end_examined();
                    return;
                }
                context.log("manual deduplicator: nothing to do at this time but finder threads are still running");
            }
        };
        Actor_engine.execute(r,"Deduplicate by similarity (3)", context.logger());

    }


    //**********************************************************
    private List<File_with_a_few_bytes> get_all_images()
    //**********************************************************
    {
        List<File_with_a_few_bytes> returned = new ArrayList<>();
        File[] files = target_dir.listFiles();
        if ( files == null) return returned;
        for (File f : files)
        {
            if ( !Guess_file_type.is_this_file_extension_an_image(f,context)) continue;
            File_with_a_few_bytes mf = new File_with_a_few_bytes(f, context.logger());
            returned.add(mf);
        }
        return returned;
    }

    //**********************************************************
    private List<File_with_a_few_bytes> get_all_songs()
    //**********************************************************
    {
        List<File_with_a_few_bytes> returned = new ArrayList<>();
        File[] files = target_dir.listFiles();
        if ( files == null) return returned;
        for (File f : files)
        {
            //if ( !Guess_file_type.is_this_a_song(f.toPath(),owner,logger)) continue; too expensive
            if ( !Guess_file_type.is_this_path_extension_a_music(f.toPath(), context.logger())) continue;
            File_with_a_few_bytes mf = new File_with_a_few_bytes(f, context.logger());
            returned.add(mf);
        }
        return returned;
    }

}
