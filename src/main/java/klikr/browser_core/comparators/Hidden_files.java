package klikr.browser_core.comparators;

import java.nio.file.Path;

//**********************************************************
public class Hidden_files
//**********************************************************
{

    //make sure hidden stuff shows at the bottom
    //**********************************************************
    public static  Integer show_last(Path f1, Path f2)
    //**********************************************************
    {
        boolean s1Hidden = f1.getFileName().toString().startsWith(".");
        boolean s2Hidden = f2.getFileName().toString().startsWith(".");
        if (s1Hidden && !s2Hidden) return 1;
        if (!s1Hidden && s2Hidden) return -1;
        return null;
    }


}
