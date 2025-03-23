import json
from typing import List, Dict, Tuple


def analyze_assertions(file_paths: List[str]):
    # Results container for all JSON files
    results = []

    for file_path in file_paths:
        try:
            # Open and load JSON file
            with open(file_path, 'r') as f:
                data = json.load(f)

            # Initialize counters for this file
            weaker_count = 0
            stronger_count = 0
            equal_count = 0
            total_count = 0

            # Iterate over each item in the "items" array
            for item in data.get("items", []):
                rouge_scores = item.get("rouge_scores")

                # Check if rouge_scores is not None and has the fields we need
                if rouge_scores:
                    reference_summary = rouge_scores.get("referenceSummary")
                    candidate_summary = rouge_scores.get("candidateSummary")

                    # Proceed if both summaries are non-empty strings
                    if reference_summary and candidate_summary:
                        ref_assertions = set([i.strip() for i in reference_summary.split('\n')])
                        cand_assertions = set([ i.strip() for i in candidate_summary.split('\n')])

                        # Determine the relationship between reference and candidate assertions
                        if ref_assertions == cand_assertions:
                            equal_count += 1
                        elif ref_assertions.issubset(cand_assertions):
                            stronger_count += 1
                        elif cand_assertions.issubset(ref_assertions):
                            weaker_count += 1
                        total_count += 1

            # Calculate percentages
            if total_count > 0:
                weaker_percent = (weaker_count / total_count) * 100
                stronger_percent = (stronger_count / total_count) * 100
                equal_percent = (equal_count / total_count) * 100
            else:
                weaker_percent = stronger_percent = equal_percent = 0.0

            # Append results for this file
            results.append({
                "file": file_path,
                "total": total_count,
                "weaker": weaker_count,
                "stronger": stronger_count,
                "equal": equal_count,
                "weaker_percent": weaker_percent,
                "stronger_percent": stronger_percent,
                "equal_percent": equal_percent
            })

        except Exception as e:
            print(f"Error processing file {file_path}: {e}")

    # Output the analysis summary for each JSON file
    for result in results:
        print(f"File: {result['file']}")
        print(f"  Total Assertions: {result['total']}")
        print(f"  Weaker: {result['weaker']} ({result['weaker_percent']:.2f}%)")
        print(f"  Equal: {result['equal']} ({result['equal_percent']:.2f}%)")
        print(f"  Stronger: {result['stronger']} ({result['stronger_percent']:.2f}%)\n")


# Example usage
json_paths = ['ADDRESSES GO HERE']
analyze_assertions(json_paths)
