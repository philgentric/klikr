package klikr.machine_learning;

//**********************************************************
public record ML_server(int port, String uuid, String type)
//**********************************************************
{
    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        StringBuilder sb = new StringBuilder();
        sb.append("port: ").append(port).append("\n");
        sb.append("uuid: ").append(uuid).append("\n");
        sb.append("type: ").append(type).append("\n");
        return sb.toString();
    }
}
