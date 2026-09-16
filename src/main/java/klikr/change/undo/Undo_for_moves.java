// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.undo;

import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.change.active_list_stage.Active_list_stage;
import klikr.change.active_list_stage.Datetime_to_signature_source;
import klikr.util.files_and_paths.Moving_files;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.util.log.Logger;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Popups;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//**********************************************************
public class Undo_for_moves implements Datetime_to_signature_source
//**********************************************************
{
    private static final boolean dbg = false;
    private static Undo_for_moves instance =  null;
    private static Logger logger = null;
    public static List<Active_list_stage> undo_stages = new ArrayList<>();
    private final Undo_core core;
    public static final String UNDO_FILENAME = "undo_for_moves";



    //**********************************************************
    public static Undo_for_moves get_instance(Kontext context)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Undo_for_moves.class)
            {
                if (instance == null)
                {
                    instance = new Undo_for_moves(context);
                }
            }
        }
        return instance;
    }



    //**********************************************************
    public static void perform_undo(Undo_item item, Kontext context)
    //**********************************************************
    {
        get_instance(context).undo(item,context);
    }

    //**********************************************************
    public static boolean add(List<Old_and_new_Path> l, Kontext context)
    //**********************************************************
    {
        if (dbg) logger.log("Undo_for_moves::add"+l);
        if (l.isEmpty())
        {
            return false;
            // logger_.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN: Undo_for_moves::add, empty list"));
        }
        return get_instance(context).add_internal(l, context);
    }
    //**********************************************************
    public static boolean perform_last_undo_fx(Kontext context)
    //**********************************************************
    {
        return get_instance(context).undo_last(context);

    }
    //**********************************************************
    public static void remove_all_undo_items(Kontext context)
    //**********************************************************
    {
        get_instance(context).remove_all_undo_items_internal(context);
    }

    //**********************************************************
    public static boolean check_validity(Undo_item undo_item, Kontext context)
    //**********************************************************
    {
        return get_instance(context).core.check_validity_internal(undo_item);
    }

    //**********************************************************
    public static void erase_if_too_old(int max_count, int max_days, Kontext context)
    //**********************************************************
    {
        Map<LocalDateTime, String> map = get_instance(context).get_map_of_date_to_signature();
        if ( map.keySet().size() < max_count) return;
        LocalDateTime now = LocalDateTime.now();
        List<String> to_be_deleted = new ArrayList<>();
        for( Map.Entry<LocalDateTime, String> e: map.entrySet())
        {
            LocalDateTime date = e.getKey();
            Period p = Period.between(now.toLocalDate(), date.toLocalDate());
            if ( p.getDays() < max_days)
            {
                to_be_deleted.add(e.getValue());
            }
        }
        for ( String signature :to_be_deleted)
        {
            Undo_item ui = get_instance(context).get_undo_item_from_signature(signature, context);
            get_instance(context).core.remove_undo_item(ui,false);
            if ( dbg) logger.log("out of age undo item removed: "+ui.signature());
        }
        get_instance(context).core.save_to_disk();

    }

    //**********************************************************
    public static void remove_invalid_undo_item(Undo_item item,Kontext context)
    //**********************************************************
    {
        get_instance(context).core.remove_undo_item(item, true);
    }


    //**********************************************************
    void remove_all_undo_items_internal(Kontext context)
    //**********************************************************
    {
        core.remove_all_undo_items_from_property_file();
        refresh_UI(context);
    }

    //**********************************************************
    private static void refresh_UI(Kontext context)
    //**********************************************************
    {
        Jfx_batch_injector.inject(()-> {
            for (Active_list_stage s : undo_stages) {
                s.define();
            }
        }, context);
    }







    //**********************************************************
    @Override
    public Map<LocalDateTime, String> get_map_of_date_to_signature()
    //**********************************************************
    {
        return core.get_map_of_date_to_signature();
    }
    //**********************************************************
    Undo_item get_undo_item_from_signature(String signature, Kontext context)
    //**********************************************************
    {
        Map<String, Undo_item> signature_to_undo_item = Undo_for_moves.get_instance(context).get_signature_to_undo_item();
        return signature_to_undo_item.get(signature);
    }


    //**********************************************************
    private Undo_for_moves(Kontext context)
    //**********************************************************
    {
        if (this.logger == null) this.logger = logger;
        core = new Undo_core(UNDO_FILENAME,context);
    }
    //**********************************************************
    public Map<String, Undo_item> get_signature_to_undo_item()
    //**********************************************************
    {
        return core.get_signature_to_undo_item_map();
    }


    //**********************************************************
    boolean undo_last(Kontext context)
    //**********************************************************
    {
        Undo_item most_recent_undo_item = core.get_most_recent();
        if (most_recent_undo_item == null) {
            logger.log(Logger.warning+" nothing to undo");
            Popups.popup_warning( Logger.warning+" Nothing to undo", "The undo list is empty!", true, context);
            return false;
        }
        return undo(most_recent_undo_item, context);
    }


    //**********************************************************
    boolean undo(Undo_item undo_item, Kontext context)
    //**********************************************************
    {
        if (dbg) logger.log("Undo_for_moves performing: UNDO of "+undo_item.to_string());
        List<Old_and_new_Path> reverse_last_move = new ArrayList<>();
        for (Old_and_new_Path e : undo_item.oans)
        {
            Old_and_new_Path r = e.reverse_for_restore();
            if ( !Files.exists(r.old_Path))
            {
                logger.log("\n\n\n❗ IGNORED: this undo item is now invalid, as the source file is not where mentioned in the record... it was probably moved since?\n\n\n");
                Popups.popup_warning( Logger.warning+" Invalid undo item", "The file was probably moved since?", true, context);
            }
            else {
                reverse_last_move.add(r);
                logger.log("reversed action =" + r.to_string());
            }
        }

        Kontext k = new Kontext(context.owner(), Shared_services.aborter(), logger);
        Moving_files.perform_safe_moves_in_a_thread(reverse_last_move, false, k);

        core.remove_undo_item(undo_item,true);
        refresh_UI(context);
        return true;
    }


    //**********************************************************
    boolean add_internal(List<Old_and_new_Path> l, Kontext context)
    //**********************************************************
    {
        for(Old_and_new_Path oan : l)
        {
            if ( oan.is_a_restore )
            {
                if ( dbg) logger.log("not recording restore event: "+oan.to_string());
                return false;
            }
            else
            {
                if ( dbg)  logger.log("Adding event: "+oan.to_string());

            }
        }

        Undo_item ui = new Undo_item(l, LocalDateTime.now(), UUID.randomUUID(), context);
        if (dbg) logger.log("Undo_for_moves add:"+ui.to_string());
        core.add(ui);
        refresh_UI(context);
        return true;
    }


}
