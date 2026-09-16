package klikr.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.IOException;

public record Kontext(Window owner, Aborter aborter, Logger logger)
{
    public void log(String message)
    {
        logger.log(message);
    }

    public boolean should_abort()
    {
        if (aborter == null) {return false;}
        return aborter.should_abort();
    }

    public void log_stack_trace(String string) {
        logger.log_stack_trace(string);
    }

    public void log_exception(String s, Exception e) {
        logger.log_exception(s, e);
    }

    public double getX()
    {
        if ( owner == null ) return 0;
        return owner.getX()+100;
    }

    public double getY()
    {
        if ( owner == null ) return 0;
        return owner.getY()+100;
    }

    public void abort(String s)
    {
        if(aborter == null) return;
        aborter().abort(s);
    }

    public void setX(double x)
    {
        if ( owner == null ) return;
        owner.setX(x);
    }

    public void setY(double minY)
    {
        if ( owner == null ) return;
        owner.setY(minY);
    }

    public void setWidth(double w)
    {
        if ( owner == null ) return;
        owner.setWidth(w);
    }

    public void setHeight(double h)
    {
        if ( owner == null ) return;
        owner.setHeight(h);
    }

    public Rectangle2D get_Rectangle2D()
    {
        if ( owner == null ) return new Rectangle2D(0,0,800,600);
        return new Rectangle2D(owner().getX(),owner().getY(),owner().getWidth(),owner().getHeight());
    }

    public double getWidth()
    {
        if ( owner == null ) return 0;
        return owner.getWidth();
    }

    public double getHeight() {
        if ( owner == null ) return 0;
        return owner.getHeight();
    }

    public void on_abort()
    {
        if ( aborter == null ) return;
        aborter.on_abort();
    }

    public String abort_reason()
    {
        if ( aborter == null ) return "";
        return aborter.reason();
    }

    public Stage get_Stage()
    {
        if ( owner == null )
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"owner is null"));
            return null;
        }
        if ( !(owner instanceof Stage) )
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"owner is not a Stage"));
            return null;
        }
        return (Stage)owner();
    }

    public void setTitle(String s)
    {
        Stage stage = get_Stage();
        if ( stage != null ) stage.setTitle(s);
    }

    public void log_with_stack(String s)
    {
        log(Stack_trace_getter.get_stack_trace(s));
    }
}
