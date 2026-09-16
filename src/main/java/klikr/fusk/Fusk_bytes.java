// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Pin_code_client.java
//SOURCES ./Pin_code_getter_stage.java
package klikr.fusk;

import javafx.application.Platform;
import klikr.settings.String_constants;
import klikr.util.Kontext;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Fusk_bytes implements Pin_code_client
//**********************************************************
{
    private static volatile Fusk_bytes instance = null;
    private static final String default_signature_text = "Don't_pay_the_FerryWoman_until_she_brings_you_to_the_other_side";
    private static String actual_signature_text;
    private static byte[] signature_clear;
    static byte[] signature_fusk;
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static AtomicBoolean pincode_popup = new AtomicBoolean(false);
    private static String pin_code = null;
    public final Kontext context;

    //**********************************************************
    private Fusk_bytes(Kontext context)
    //**********************************************************
    {
        this.context= context;
    }

    //**********************************************************
    public static boolean is_initialized()
    //**********************************************************
    {
        return initialized.get();
    }


    /*
    initialization is super complicated:
    0. someone needs to fusk or defusk something, but there is no instance
    1. creates an instance, and ask the user for the pin
    ... while the user is entering the pin (which can take forever) ... still not usable
    2. user is finished entering the pin, set_code_pin is called asynchronously, we have the pin!
    3. someone tries again, init() is called and -this time- succeeds

    to get a new instance one must call RESET
     */
    //**********************************************************
    public static boolean initialize(Kontext context)
    //**********************************************************
    {
        if ( is_initialized())
        {
            context.log("already initialized");
            return true;
        }
        if ( !Platform.isFxApplicationThread())
        {
            context.log("HAPPENS1 initialize");
            Platform.runLater(()->initialize(context));
            return false;
        }
        if ( instance == null)
        {
            context.log(Stack_trace_getter.get_stack_trace("creating instance"));
            instance = new Fusk_bytes(context);
        }
        return instance.init(context);
    }

    //**********************************************************
    public static void reset(Logger logger)
    //**********************************************************
    {
        initialized.set(false);
        instance = null;
        pin_code = null;
        logger.log("fusk reset done");

    }


    //**********************************************************
    @Override
    public void set_pin_code(String new_pin_code)
    //**********************************************************
    {
        pin_code = new_pin_code;
        context.log("set_pin_code->"+new_pin_code+"<-");
        init(context);
    }
    //**********************************************************
    private boolean init(Kontext context)
    //**********************************************************
    {
        if ( !Platform.isFxApplicationThread())
        {
            context.log("HAPPENS1 init");
            Platform.runLater(()->init(context));
            return false;
        }
        if ( is_initialized())
        {
            context.log("Fusk already initialized");
            return true;
        }
        if ( pin_code == null)
        {
            if ( pincode_popup.compareAndSet(false, true) )
            {
                context.log("Fusk: getting pin code from user");

                Platform.runLater(()->{
                    Pin_code_getter_stage pin_code_getter_stage = new Pin_code_getter_stage( context);
                    pin_code_getter_stage.ask_pin_code_in_a_thread(this);
                });
            }
            return false; // not ready yet
        }

        // look in .klik for a .passphrase.txt file
        actual_signature_text = default_signature_text;
        String home = System.getProperty(String_constants.USER_HOME);
        File passphrase_folder = new File (home, String_constants.CONF_DIR);
        File passphrase_file = new File (passphrase_folder, ".passphrase.txt");
        if ( passphrase_file.exists()) {
            try {
                List<String> lines = Files.readAllLines(passphrase_file.toPath(),StandardCharsets.UTF_8);
                if ( lines.size() > 0) {
                    actual_signature_text = lines.get(0);
                    context.log("Fusk: acquired this passphrase from file: ->"+actual_signature_text+"<-");
                } else {
                    context.log("Fusk: using default passphrase");
                }
            } catch (IOException e)
            {
                context.log("Fusk: could not read passphrase file"+e);
                return false;
            }
        }

        //logger.log(Stack_trace_getter.get_stack_trace("fusk signature initialized as:->"+signature_text+"<-"));
        String local = pin_code+actual_signature_text;
        signature_clear = local.getBytes(StandardCharsets.UTF_8);
        signature_fusk = fusk(signature_clear);
        initialized.set(true);
        context.log("fusk signature initialized: "+signature_clear.length+" bytes string ="+local);
        return true;
    }



    private static final boolean shorter = true; // when true, maybe a bit faster
    private static final int LIMIT = 200;

    //**********************************************************
    static byte[] fusk(byte[] in)
    //**********************************************************
    {

        byte[] out = new byte[in.length];
         if ( shorter & in.length>LIMIT )
         {
             int j = 0;
             for (int i = 0; i < LIMIT; i++) {
                 out[i] = (byte) (in[i] ^ signature_clear[j]);
                 j++;
                 if (j >= signature_clear.length) j = 0;
             }
             System.arraycopy(in,LIMIT,out,LIMIT,in.length-LIMIT);
         }
         else
         {
             int j = 0;
             for (int i = 0; i < in.length; i++) {
                 out[i] = (byte) (in[i] ^ signature_clear[j]);
                 j++;
                 if (j >= signature_clear.length) j = 0;
             }
         }
         return out;
    }

    //**********************************************************
    static byte[] obfusk_and_add_signature(byte[] clear, Kontext context)
    //**********************************************************
    {
        if (check(context)) return null;

        byte[] obfuscated = new byte[clear.length+signature_fusk.length];
        System.arraycopy(signature_fusk,0,obfuscated,0,signature_fusk.length);
        if ( context.should_abort()) return null;
        byte[] fusk = Fusk_bytes.fusk(clear);
        if ( context.should_abort()) return null;
        System.arraycopy(fusk,0,obfuscated,signature_fusk.length,fusk.length);
        return obfuscated;
    }


    //**********************************************************
    static byte[] defusk_bytes_and_remove_signature(byte[] obfuscated, Kontext context)
    //**********************************************************
    {
        if (check(context)) return null;

        byte[] fusk = new byte[obfuscated.length-signature_fusk.length];
        // skip signature
        System.arraycopy(obfuscated,signature_fusk.length,fusk,0,fusk.length);
        if ( context.should_abort()) return null;
        return fusk(fusk);
    }

    //**********************************************************
    public static boolean check_signature(byte[] obfuscated, Kontext  context)
    //**********************************************************
    {
        if (check(context)) return false;

        if (Arrays.mismatch(obfuscated,signature_fusk) == signature_fusk.length) return true;
        return false;
    }

    //**********************************************************
    private static boolean check(Kontext  context)
    //**********************************************************
    {
        if ( instance == null)
        {
            initialize(context);
            return true;
        }
        if( !is_initialized()) return true;
        return false;
    }

}
