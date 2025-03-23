import json


def count_json_objects(file_path):
    # Load the JSON file
    with open(file_path, 'r') as file:
        data = json.load(file)

    # Ensure "items" exists and is a list
    items = data.get("items", [])

    # Initialize counters
    count_java_null_filtered = 0
    count_non_java_null_filtered = 0
    counter =0
    # Iterate through each object in the "items" array
    for obj in items:
        llm_raw_response = obj.get("LLM_raw_response")
        llm_filtered_raw_response = obj.get("LLM_filtered_raw_response")


        if llm_raw_response is not None and llm_filtered_raw_response is None:
            counter += 1
            if llm_raw_response.startswith("<JAVA>"):
                count_java_null_filtered += 1
            else:
                count_non_java_null_filtered += 1

    return count_java_null_filtered, count_non_java_null_filtered, counter


# Example usage
file_path = "ADDRESS GOES HERE"
java_count, non_java_count, counter = count_json_objects(file_path)
print("Counter: ",counter)

print(
    f"Number of JSON objects where LLM_raw_response starts with '<JAVA>' and LLM_filtered_raw_response is null: {java_count}")
print(
    f"Number of JSON objects where LLM_raw_response does not start with '<JAVA>' and LLM_filtered_raw_response is null: {non_java_count}")
