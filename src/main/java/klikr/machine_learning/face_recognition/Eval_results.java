package klikr.machine_learning.face_recognition;

import klikr.machine_learning.feature_vector.Feature_vector;

import java.util.List;

public record Eval_results(String label, Feature_vector feature_vector, Eval_situation eval_situation, boolean adding, String tag, List<Eval_result_for_one_prototype> list){};
