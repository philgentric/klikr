package klikr.browser_core.diskview;

import javafx.geometry.Rectangle2D;

import java.util.List;


//**********************************************************
public class Layout_result
//**********************************************************
{
    public final File_node node;
    public final Rectangle2D bounds;
    public final List<Layout_result> children;

    //**********************************************************
    public Layout_result(File_node node, Rectangle2D bounds, List<Layout_result> children)
    //**********************************************************
    {
        this.node = node;
        this.bounds = bounds;
        this.children = children;
    }
}