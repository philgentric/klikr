// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui;

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.util.Duration;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.util.Kontext;
import klikr.util.log.Stack_trace_getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Popups
//**********************************************************
{

    //**********************************************************
    public static void popup_Exception(Exception e, double icon_size, String title, Kontext context)
    //**********************************************************
    {
        context.log(Stack_trace_getter.get_stack_trace("Going to popup exception(1): " + e));
        Jfx_batch_injector.inject(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(context.owner());
            Look_and_feel_manager.set_dialog_look(alert, context.logger());
            alert.getDialogPane().setMinWidth(1000);
            alert.setTitle(title);
            alert.setHeaderText("Operation was denied");
            if ( e==null)
            {
                alert.setContentText(title);
            }
            else
            {
                alert.setContentText("The error was: \n" + e);
            }

            context.log("Going to popup exception(2): " + e);
            alert.setGraphic(new ImageView(Look_and_feel_manager.get_denied_icon(icon_size,context.logger())));
            alert.showAndWait();

        }, context);
    }

    private static final List<String> done = new ArrayList<>();

    //**********************************************************
    synchronized public static void popup_warning(
            String header, String content,
            boolean for_3_seconds_only, Kontext context)
    //**********************************************************
    {
        String signature = header+content+for_3_seconds_only+context.owner().toString();
        if ( done.contains(signature) )
        {
            context.log("suppressed repeated warning "+signature);
            return;
        }
        done.add(signature);
        Jfx_batch_injector.inject(() ->
        {
            context.log("Warning Popup: "+header+" "+content);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            if ( context.owner() != null)
            {
                try
                {
                    alert.initOwner(context.owner());
                }
                catch(NullPointerException e)
                {
                    context.log(Stack_trace_getter.get_stack_trace("Typically this error occurs because you are calling popup_warning(), passing a Window owner that has no scene yet "+e));
                }
            }

            Look_and_feel_manager.set_dialog_look(alert, context.logger());
            alert.setTitle(My_I18n.get_I18n_string("Warning", context));
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.initModality(Modality.WINDOW_MODAL);

            if (for_3_seconds_only)
            {
                PauseTransition delay = new PauseTransition(Duration.millis(1000));
                delay.setOnFinished(e -> alert.hide());
                alert.show();
                delay.play();
            } else {
                alert.showAndWait();
            }
        },context);
    }

    //**********************************************************
    public static boolean popup_ask_for_confirmation(String header, String content, Kontext context)
    //**********************************************************
    {
        context.log("confirm: "+header+" "+content);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        Look_and_feel_manager.set_dialog_look(alert, context.logger());
        if ( context.owner() != null) alert.initOwner(context.owner());
        alert.setTitle(My_I18n.get_I18n_string("Please_confirm", context));
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent())
        {
            if (result.get() == ButtonType.OK)
            {
                return true;
            }
        }

        // ... user chose CANCEL or closed the dialog
        return false;
    }


    //**********************************************************
    public static void simple_alert(String s, Kontext context)
    //**********************************************************
    {
        // this is a BLOCKING window
        Alert alert = new Alert(Alert.AlertType.INFORMATION,s, ButtonType.CLOSE);
        Look_and_feel_manager.set_dialog_look(alert, context.logger());
        alert.initOwner(context.owner());
        alert.show();
    }

    // if returns true, means "dont show me this again"
    //**********************************************************
    public static boolean info_popup(String s, String OK_button_alternate_text, Kontext context)
    //**********************************************************
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        Look_and_feel_manager.set_dialog_look(alert, context.logger());
        alert.initOwner(context.owner());
        alert.setTitle("Information");
        alert.setHeaderText("");
        Text text = new Text(s);
        text.setWrappingWidth(800);
        alert.getDialogPane().setContent(text);
        alert.setContentText("");
        Button ok = (Button)alert.getDialogPane().lookupButton(ButtonType.OK);
        if ( OK_button_alternate_text != null) ok.setText(OK_button_alternate_text);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent())
        {
            if (result.get() == ButtonType.OK)
            {
                return true;
            }
        }

        // ... user chose CANCEL or closed the dialog
        return false;

    }
}
