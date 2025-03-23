import argparse
import json
import os
import sys

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer


# needs changes to adapt with users and assistants
def main():
    parser = argparse.ArgumentParser(description="Generate text using a pretrained model.")
    parser.add_argument("--temperature", type=float, default=1.0, help="Sampling temperature")
    parser.add_argument("--top_p", type=float, default=1.0, help="Top-p sampling threshold")
    parser.add_argument("--api_key", type=str, default=None, help="Your API key")
    parser.add_argument("--trial_number", type=int, default=3, help="Trial number")
    parser.add_argument("--mrds", type=int, default=20, help="MRDS value")
    parser.add_argument("--frequency_penalty", type=float, default=-1, help="Frequency penalty")
    parser.add_argument("--presence_penalty", type=float, default=-1, help="Presence penalty")
    parser.add_argument("--max_length", type=int, default=2048, help="Max length")
    parser.add_argument("--model", type=str, default=None, help="Your model name")
    parser.add_argument("--is_embedding", type=str, help="If the model should calculate embedding vector")

    args = parser.parse_args()
    # Load the pretrained model and tokenizer
    model = AutoModelForCausalLM.from_pretrained(args.model, trust_remote_code=True, torch_dtype=torch.float32)
    tokenizer = AutoTokenizer.from_pretrained(args.model, trust_remote_code=True, torch_dtype=torch.float32)
    tokenizer.add_special_tokens({'pad_token': '[PAD]'})

    trial_number = args.trial_number

    user_path = "C:\\Users\\Administrator\\Desktop\\Assertions_generation\\Sources\\Pipeline_v2_0\\python_scripts\\models\\user.txt"
    system_path = "C:\\Users\\Administrator\\Desktop\\Assertions_generation\\Sources\\Pipeline_v2_0\\python_scripts\\models\\system.txt"
    user_prompt, system_prompt = None, None
    if os.path.isfile(user_path):
        user_prompt = str(open(user_path, "r", encoding="utf-8").read()).encode("UTF-8").decode("UTF-8")
    if os.path.isfile(system_path):
        system_prompt = str(open(system_path, "r", encoding="utf-8").read()).encode("UTF-8").decode("UTF-8")

    if args.is_embedding == "false":
        while trial_number > 0:
            inputs = tokenizer(
                json.loads(system_prompt)['message'] +
                (
                    " Here is some examples followed by the method for which you generate assertions:\n" +
                    json.loads(user_prompt)['message'] if user_prompt is not None else ""),
                return_tensors="pt", return_attention_mask=False, max_length=args.max_length, truncation=True)

            outputs = model.generate(
                **inputs,
                max_new_tokens=args.max_length,
                temperature=args.temperature,
                top_p=args.top_p,
            )

            generated_text = str(tokenizer.batch_decode(outputs)[0])

            if generated_text is not None:
                print(generated_text)
                sys.stdout.flush()
                break
            else:
                trial_number -= 1

        if trial_number == 0:
            print("Exceeded maximum trials. Could not generate response!")
            sys.stdout.flush()
    else:
        model = AutoModelForCausalLM.from_pretrained(args.model, trust_remote_code=True,
                                                     torch_dtype=torch.float32)
        tokenizer = AutoTokenizer.from_pretrained(args.model, trust_remote_code=True,
                                                  torch_dtype=torch.float32)
        tokenizer.add_special_tokens({'pad_token': '[PAD]'})

        trial_number = args.trial_number

        while trial_number > 0:
            input_text = json.loads(system_prompt)['message']

            inputs = tokenizer(input_text, return_tensors="pt", return_attention_mask=False)

            embeddings = model.base_model.get_input_embeddings()(inputs.input_ids)

            mean_embedding = torch.mean(embeddings, dim=1)
            mean_embedding_array = mean_embedding.detach().numpy()[0]

            if mean_embedding is not None:
                print(json.dumps(mean_embedding_array.tolist()))
                break
            else:
                trial_number -= 1

        if trial_number == 0:
            print("Exceeded maximum trials. Could not generate embeddings!")


if __name__ == "__main__":
    main()
