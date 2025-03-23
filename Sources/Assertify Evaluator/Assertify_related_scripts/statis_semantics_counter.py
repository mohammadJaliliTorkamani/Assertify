import json
from typing import List, Tuple, Dict

def count_specific_items(json_paths: List[str], search_strings: List[str]) -> Tuple[Dict[str, Dict[str, int]], int]:
    aggregated_counts = {search_str: 0 for search_str in search_strings}
    total_counter = 0
    results_by_path = {}

    for json_path in json_paths:
        try:
            with open(json_path, 'r') as file:
                print("Path:",json_path)
                data = json.load(file)

                if "items" not in data or not isinstance(data["items"], list):
                    print(f"The 'items' field is missing or is not a list in file: {json_path}")
                    continue

                path_counts = {search_str: 0 for search_str in search_strings}
                path_total = 0

                for item in data["items"]:
                    if "is_compiled_after" in item and "post_log" in item and item["post_log"]:
                        if item["is_compiled_after"] is False:
                            path_total += 1
                            total_counter += 1


                            found=False
                            for search_str in search_strings:
                                if search_str in item["post_log"]:
                                    path_counts[search_str] += 1
                                    aggregated_counts[search_str] += 1
                                    print(item['id'],": ",search_str)
                                    found=True
                            if not found:
                                print('??: ',item['id'])

                results_by_path[json_path] = {"counts": path_counts, "total": path_total}
        except (json.JSONDecodeError, FileNotFoundError) as e:
            print(f"Error loading JSON file {json_path}: {e}")

    return results_by_path, aggregated_counts, total_counter

search_terms = ["cannot find symbol", "unreachable statement","bad operand types","illegal start of expression","might not have been initialized","unexpected type"]
json_paths = [
    'ADDRESSES GO HERE',

]

results_by_path, individual_counts, total_counter = count_specific_items(json_paths, search_terms)

for path, result in results_by_path.items():
    print(f"\nResults for {path}:")
    print(f"  Total records with compilation error: {result['total']}")
    c=0
    for error, count in result["counts"].items():
        c+=count
        print(f"  Count for '{error}': {count} ({(count / result['total'] * 100) if result['total'] else 0}%)")
    print("Sum:",c)

print("\nAggregated results across all paths:")
print(f"Total records with compilation error: {total_counter}")
for error, count in individual_counts.items():
    print(f"Count for '{error}': {count} ({(count / total_counter * 100) if total_counter else 0}%)")
