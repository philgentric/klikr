package klikr.machine_learning.old_python_via_html_ML;

//**********************************************************
public record ML_server(int port, String uuid, String type)
//**********************************************************
{
    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return "port: " + port + "\n" +
                "uuid: " + uuid + "\n" +
                "type: " + type + "\n";
    }
}
