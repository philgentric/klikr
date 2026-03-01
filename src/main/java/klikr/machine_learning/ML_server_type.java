package klikr.machine_learning;

import javafx.stage.Window;
import klikr.settings.Non_booleans_properties;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;

import java.nio.file.Path;


//**********************************************************
public enum ML_server_type
//**********************************************************
{
    // Convolutional Neural Network = CNN
    MobileNet, // feature vectors (CNN embeddings) for general purpose image similarity
    FaceNet, //  feature vectors (CNN embeddings) specialized for face similarity (=> recognition assuming faces with labels are recorded)
    MTCNN, // Convolutional Neural Network for face detection
    // "old tech" face detector
    Haars_tree,
    Haars_default,
    Haars_alt1,
    Haars_alt2;

    //**********************************************************
    public static ML_server_type[] face_detectors()
    //**********************************************************
    {
        return new ML_server_type[]{MTCNN, Haars_alt1, Haars_alt2, Haars_default, Haars_tree};
    }

    //**********************************************************
    Path registry_path(Window owner, Logger logger)
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet, MTCNN, Haars_alt1, Haars_alt2, Haars_default, Haars_tree:
            {
                return Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("face_recognition_server_registry", false, owner, logger);
            }
            case MobileNet:
            {
                return Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("image_similarity_server_registry", false, owner, logger);
            }
        }
        return null;
    }

    private int number_of_image_similarity_servers = -1;
    //**********************************************************
    int quota(Window owner)
    //**********************************************************
    {
        // for the face recog servers we count '1' for a call to start ... which may start more than one
        switch(this)
        {
            case FaceNet, MTCNN:
            {
                return 3;
            }
            case Haars_alt1, Haars_alt2, Haars_default, Haars_tree:
            {
                return 1;
            }
            case MobileNet:
            {
                if ( number_of_image_similarity_servers < 0 ) number_of_image_similarity_servers = Non_booleans_properties.get_number_of_image_similarity_servers(owner);
                return number_of_image_similarity_servers;
            }
        }
        return -1;
    }

    //**********************************************************
    String python_file_name()
    //**********************************************************
    {
        switch(this)
        {
            case FaceNet:
            {
            return "FaceNet_embeddings_server.py";
            }
            case Haars_alt1, Haars_alt2, Haars_default, Haars_tree:
            {
            return "haars_face_detection_server.py";
            }
            case MTCNN:
            {
            return "MTCNN_face_detection_server.py";
            }
            case MobileNet:
            {
                return "MobileNet_embeddings_server.py";
            }
        }
        return null;
    }

    public String get_xml_file_name()
    {
        switch(this)
        {
            case Haars_default:
                return "haarscascade_frontalface_alt_default.xml";
            case Haars_tree:
                return "haarscascade_frontalface_alt_tree.xml";
            case Haars_alt1:
                return "haarscascade_frontalface_alt_1.xml";
            case Haars_alt2:
                return "haarscascade_frontalface_alt_2.xml";
        }
        return null;
    }
}
