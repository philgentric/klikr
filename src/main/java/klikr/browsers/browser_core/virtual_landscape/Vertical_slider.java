// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.virtual_landscape;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.log.Logger;

//**********************************************************
public class Vertical_slider implements Landscape_height_listener, Scroll_to_listener
//**********************************************************
{
    public static final boolean dbg = false;
    public static final double slider_width = 40;
    final Slider the_Slider;
    private final Kontext context;
    Pane the_big_display_pane;
    //public static boolean inverted = Booleans.get_boolean(Advanced_feature.Invert_vectical_scroll.name());

    //**********************************************************
    public Vertical_slider(
            Pane the_big_display_pane,
            Virtual_landscape virtual_landscape,
            Kontext context)
    //**********************************************************
    {
        this.context = context;
        this.the_big_display_pane = the_big_display_pane;


        // we set 100 as pixel_height is not known at slider creation time
        double min = 0;
        double max = 100;
        double val = 0;
        the_Slider = new Slider(min,max,val);//Virtual_landscape.landscape_height);

        the_Slider.setOrientation(Orientation.VERTICAL);
        //the_Slider.toFront();
        //the_Slider.setVisible(true);

        adapt_slider_to_scene(context.get_Stage());

        the_Slider.valueProperty().addListener((ov, old_val_, new_val_) -> {
            double slider = new_val_.doubleValue();
            if ( Virtual_landscape.scroll_dbg) context.log("LISTENER: slider property changed: OLD= "+ old_val_.doubleValue()+" ==> NEW= "+ slider);
            slider_moved_by_user(slider, virtual_landscape);
        });
    }

    //**********************************************************
    private void slider_moved_by_user(double slider, Virtual_landscape virtual_landscape)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(virtual_landscape.get_virtual_landscape_height());
        double new_pixel = slider_to_pixels(slider, pixel_height);

        if ( Virtual_landscape.scroll_dbg)
        {
            String reason = "(normalized+inverted with pixel_height= "+pixel_height+") slider = "+ slider +"  ==> " +new_pixel;
            context.log(reason);
        }
        virtual_landscape.move_absolute(new_pixel, "VIRTUAL LANDSCAPE move absolute = slider moved! ");
    }

    //**********************************************************
    private double get_pixel_height(double virtual_landscape_height)
    //**********************************************************
    {
        if ( dbg) context.log("virtual_landscape_height="+virtual_landscape_height);
        if ( dbg) context.log("pane_height="+ the_big_display_pane.getHeight());

        double pixel_height = virtual_landscape_height - the_big_display_pane.getHeight();
        if (pixel_height < 0)
        {
            // the virtual landscape height is smaller than the pane's height
            pixel_height = the_big_display_pane.getHeight();
        }
        if ( dbg)
            context.log("pixel_height (slider SETMAX to) ="+pixel_height);
        double finalPixel_height = pixel_height;
        Platform.runLater(()->the_Slider.setMax(finalPixel_height)); // when the pixel height is very large this is key to get good manual (mouse/trackpad) scroll accuracy
        return pixel_height;
    }

    //**********************************************************
    private double slider_to_pixels(double slider_value, double pixel_height)
    //**********************************************************
    {
        double fraction = 1;
        if (the_Slider.getMax() == 0)
        {
            context.log(Logger.error+"PANIC: get_slider_max() == 0");
        }
        else
        {
            fraction = slider_value / the_Slider.getMax(); // normalize (0,1)
        }
        //if (inverted)
        {
            fraction = 1.0 - fraction;
        }
        if ( dbg) context.log("pixel_height="+pixel_height);
        if ( dbg) context.log("slider_to_pixels tmp="+fraction);
        double new_pixel = pixel_height * fraction;
        if ( dbg) context.log("new_pixel="+new_pixel);
        if ( Virtual_landscape.scroll_dbg) context.log("slider_to_pixels (with pixel_height="+pixel_height+") gives: "+slider_value+" ==> "+new_pixel);

        return new_pixel;
    }

    //**********************************************************
    private double pixels_to_slider(double pixels, double pixel_height)
    //**********************************************************
    {
        if ( pixel_height == 0)
        {
            context.log(Logger.error+"PANIC pixel_height == 0 max="+the_Slider.getMax()+" min="+the_Slider.getMin());

            pixel_height = 42;
        }
        double tmp = pixels/pixel_height; // normalize (0,1)
        //if (inverted)
        {
            tmp = 1.0 - tmp;
        }
        if ( Virtual_landscape.scroll_dbg) context.log("pixels_to_slider tmp="+tmp);
        if ( Virtual_landscape.scroll_dbg) context.log("Slider.getMax()="+the_Slider.getMax());

        double new_slider= tmp * the_Slider.getMax();
        if ( Virtual_landscape.scroll_dbg) context.log("new_slider="+new_slider);
        if ( Virtual_landscape.scroll_dbg) context.log("pixels_to_slider (with pixel_height="+pixel_height+") gives: "+pixels+" ==> "+new_slider);
        return new_slider;
    }



    //**********************************************************
    @Override // Scroll_to_listener
    public void perform_scroll_to(double y_offset_in_pixels, Virtual_landscape virtual_landscape)
    //**********************************************************
    {
        double pixel_height = get_pixel_height(virtual_landscape.get_virtual_landscape_height());
        if ( Virtual_landscape.scroll_dbg) context.log("y_offset_in_pixels = "+y_offset_in_pixels);
        if ( Virtual_landscape.scroll_dbg) context.log("pixel_height = "+pixel_height);

        double slider = pixels_to_slider(y_offset_in_pixels,pixel_height);
        if ( Virtual_landscape.scroll_dbg) context.log("scroll_absolute NEW slider value = "+slider);
        the_Slider.adjustValue(slider);
    }


    //**********************************************************
    public boolean request_scroll_relative(double dy)
    //**********************************************************
    {
        //if (inverted)
        {
            if ( Virtual_landscape.scroll_dbg) context.log("scroll is inverted="+dy+" ==> "+(-dy));
            dy = -dy;
        }
        /*else
        {
            if ( Scroll_position_cache.scroll_dbg) context.log("scroll is not inverted="+dy);
        }*/
        double old_val = the_Slider.getValue();
        if ( dbg) context.log("scroll_relative old_val="+old_val);
        if ( dbg) context.log("scroll_relative dy="+dy);
        double new_val = old_val - dy;

        if (Virtual_landscape.scroll_dbg) context.log("slider old val:"+old_val+" - scroll="+dy+" SETTING SLIDER VAL ="+new_val);


        if ( Virtual_landscape.scroll_dbg)
            context.log("scroll_relative new slider value (user has scrolled) = "+new_val);
        the_Slider.adjustValue(new_val);

        if ( (new_val < the_Slider.getMin()) || (new_val > the_Slider.getMax())  )
        {
            // no change
            //if (Vertical_slider.dbg)  context.log("NO scroll dy=" + dy+" min="+slider.getMin()+ " old="+old_val+ "new="+new_val+" max="+slider.getMax());
            return false;
        }
        else
        {
            //if (Vertical_slider.dbg) context.log("scroll dy=" + dy+" min="+slider.getMin()+ " old="+old_val+ "new="+new_val+" max="+slider.getMax());
            return true;
        }

    }


    //**********************************************************
    @Override
    public void browsed_landscape_height_has_changed(double new_landscape_height, double current_vertical_offset)
    //**********************************************************
    {
        if ( Virtual_landscape.scroll_dbg)
            context.log("browsed_landscape_height_has_changed = "+new_landscape_height);
        double pixel_height = get_pixel_height(new_landscape_height);
        double slider = pixels_to_slider(current_vertical_offset,pixel_height);
        if ( Virtual_landscape.scroll_dbg) context.log("browsed_landscape_height_has_changed(), new slider value = "+slider);
        //the_Slider.adjustValue(slider);
    }




    //**********************************************************
    public void adapt_slider_to_scene(Window stage)
    //**********************************************************
    {
        if ( dbg) context.log("adapt_slider_to_scene stage.getWidth()="+stage.getWidth()+" h = "+stage.getHeight());
        double height = stage.getHeight() - 100;
        the_Slider.setPrefHeight(height);//2 * half_slider_width);

    }


}
