import sys

import evaluate as evaluate
from rouge_score import rouge_scorer

rouge = evaluate.load('rouge')
scorer = rouge_scorer.RougeScorer(['rouge1', 'rouge2', 'rougeL'], use_stemmer=False)

DEFAULT_REFERENCE_PATH = 'ADDRESS GOES HERE'
DEFAULT_CANDIDATE_PATH = 'ADDRESS GOES HERE'


def compute(reference_path, candidate_path):
    global DEFAULT_CANDIDATE_PATH, DEFAULT_REFERENCE_PATH

    if reference_path is None:
        reference_path = DEFAULT_REFERENCE_PATH

    if candidate_path is None:
        candidate_path = DEFAULT_CANDIDATE_PATH

    with open(reference_path, 'r', encoding='utf-8') as ref_file:
        reference_lines = ref_file.readlines()

    with open(candidate_path, 'r', encoding='utf-8') as candidate_file:
        candidate_lines = candidate_file.readlines()

    num_permutations = 0
    rouge1_sum = [0.0, 0.0, 0.0]  # F,P,R
    rouge2_sum = [0.0, 0.0, 0.0]
    rougeL_sum = [0.0, 0.0, 0.0]

    # Compute all 2 permutations of lines from both files
    for reference_line in reference_lines:
        for candidate_line in candidate_lines:
            reference_item = reference_line.strip()
            candidate_item = candidate_line.strip()

            scores = scorer.score(reference_item, candidate_item)
            num_permutations += 1
            rouge1_sum = sum_matrices(rouge1_sum,
                                      [scores['rouge1'].fmeasure, scores['rouge1'].precision, scores['rouge1'].recall])
            rouge2_sum = sum_matrices(rouge2_sum,
                                      [scores['rouge2'].fmeasure, scores['rouge2'].precision, scores['rouge2'].recall])

            rougeL_sum = sum_matrices(rougeL_sum,
                                      [scores['rougeL'].fmeasure, scores['rougeL'].precision, scores['rougeL'].recall])

    avg_rouge1 = div(rouge1_sum, num_permutations)
    avg_rouge2 = div(rouge2_sum, num_permutations)
    avg_rougeL = div(rougeL_sum, num_permutations)

    # Return the average Rouge scores as a dictionary
    avg_scores = {
        'rouge1': {'F': avg_rouge1[0], 'P': avg_rouge1[1], 'R': avg_rouge1[2]},
        'rouge2': {'F': avg_rouge2[0], 'P': avg_rouge2[1], 'R': avg_rouge2[2]},
        'rougeL': {'F': avg_rougeL[0], 'P': avg_rougeL[1], 'R': avg_rougeL[2]}
    }

    return avg_scores


def sum_matrices(a: list, b: list):
    return [x + y for x, y in zip(a, b)]


def div(a: list, b: int):
    return [x / b for x in a]


if __name__ == "__main__":
    if len(sys.argv) == 3:
        reference_path = sys.argv[1]
        candidate_path = sys.argv[2]
    else:
        reference_path = None
        candidate_path = None

    print(compute(reference_path, candidate_path))
