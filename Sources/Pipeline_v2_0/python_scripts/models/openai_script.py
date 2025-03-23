import argparse
import json
import os
import sys
import time

from openai import OpenAI

import openai



def get_embedding(client,list_, model="text-embedding-3-small"):
    # text = list_.replace("\n", " ")
    return client.embeddings.create(input = list_, model=model)


def extract_message(response):
    if response is None:
        return None
    elif len(response.choices) == 0:
        return None
    else:
        choice = response.choices[0]
        if choice.message is None or choice.message.content is None:
            return None
        return choice.message.content


def extract_embedding(response, is_batch):
    if response is None:
        return None
    elif len(response.data) == 0:
        return None
    else:
        if is_batch:
            total_embeddings = []
            for item in response.data:
                if item.embedding is None or len(item.embedding) == 0:
                    continue
                total_embeddings.append(item.embedding)
            return total_embeddings
        else:
            data = response.data[0]
            if data.embedding is None or len(data.embedding) == 0:
                return None
            return data.embedding


def main():
    encodings = [
        "utf-8",
        "utf-16",
        "latin-1",  # ISO-8859-1
        "cp1252",  # Windows-1252
        "windows-1251",  # Russian (Windows)
        "koi8-r",  # Russian (KOI8-R)
        "gb2312",  # Simplified Chinese (GB2312)
        "gbk",  # Simplified Chinese (GBK)
        "big5",  # Traditional Chinese (Big5)
        "utf-8-sig",  # UTF-8 with BOM
        # Add more encodings as needed
    ]

    parser = argparse.ArgumentParser(description="Generate text using a pretrained model.")
    parser.add_argument("--system_prompt", type=str, default=None, help="System Prompt text")
    parser.add_argument("--user_prompt", type=str, default=None, help="User Prompt text")
    parser.add_argument("--temperature", type=float, default=1.0, help="Sampling temperature")
    parser.add_argument("--top_p", type=float, default=1.0, help="Top-p sampling threshold")
    parser.add_argument("--api_key", type=str, default=None, help="Your API key")
    parser.add_argument("--model", type=str, default=None, help="Your model name")
    parser.add_argument("--is_embedding", type=str, help="If the model should calculate embedding vector")
    parser.add_argument("--is_batch", type=str, default="false", help="If the model should consider batch inputs")
    parser.add_argument("--trial_number", type=int, default=3, help="Trial number")
    parser.add_argument("--mrds", type=int, default=20, help="MRDS value")
    parser.add_argument("--frequency_penalty", type=float, default=-1, help="Frequency penalty")
    parser.add_argument("--presence_penalty", type=float, default=-1, help="Presence penalty")
    parser.add_argument("--max_length", type=int, default=4096, help="Presence penalty")

    args = parser.parse_args()
    openai.api_key = args.api_key

    client = OpenAI(api_key=args.api_key)

    trial_number = args.trial_number

    user_path = "ADDRESS GOES HERE"
    system_path = "ADDRESS GOES HERE"
    assistant_path = "ADDRESS GOES HERE"
    user_prompt, system_prompt, assistant_prompt = None, None, None
    if os.path.isfile(user_path):
        user_prompt = str(open(user_path, "r", encoding="utf-8").read())
    if os.path.isfile(system_path):
        system_prompt = str(open(system_path, "r", encoding="utf-8").read())
    if os.path.isfile(assistant_path):
        assistant_prompt = str(open(assistant_path, "r", encoding="utf-8").read())

    if args.is_embedding == "false":
        while trial_number > 0:
            messages = []
            if system_prompt:
                if system_prompt and (json.loads(system_prompt) is not None):
                    messages.append({'role': 'system', 'content': json.loads(system_prompt)['message']})

            if assistant_prompt is None:
                if user_prompt:
                    for user in json.loads(json.loads(user_prompt)['message']):
                        messages.append({'role': 'user', 'content': user})
            else:
                for (u, a) in zip(json.loads(json.loads(user_prompt)['message']),
                                  json.loads(json.loads(assistant_prompt)['message'])):
                    messages.append({'role': 'user', 'content': u})
                    messages.append({'role': 'assistant', 'content': a})

                users = json.loads(json.loads(user_prompt)['message'])
                messages.append({'role': 'user', 'content': users[len(users) - 1]})

            arguments = {'model': args.model,
                         'temperature': args.temperature,
                         'top_p': args.top_p,
                         'stream': False,
                         'presence_penalty': args.presence_penalty,
                         'frequency_penalty': args.frequency_penalty,
                         'messages': messages
                         }

            try:
                response = client.chat.completions.create(**arguments)
                message = extract_message(response)
                print(message)
                exit(0)
                if (message is not None) and ("Traceback (most recent call last)" not in message) and (
                        "Bad gateway" not in message) and ("Traceback" not in message):
                    print(message)  # was print(message.encode('utf-8'))
                    sys.stdout.flush()
                    break
                else:
                    trial_number -= 1
            except Exception as e:
                print(e)
                trial_number -= 1
            time.sleep(5)

        if trial_number == 0:
            print("Exceeded maximum trials. Could not generate response!")
            sys.stdout.flush()

    else:
        while trial_number > 0:
            if args.is_batch == "true":
                system_input = json.loads(json.loads(system_prompt)['message'])
            else:
                system_input = json.loads(system_prompt)['message']

            try:

                response = get_embedding(client,system_input, args.model)
                embeddings = extract_embedding(response, args.is_batch == "true")
                if (embeddings is not None) and ("Traceback (most recent call last)" not in embeddings) and (
                        "Bad gateway" not in embeddings) and ("Traceback" not in embeddings):
                    print(json.dumps(embeddings))
                    sys.stdout.flush()
                    break
                else:
                    trial_number -= 1
            except Exception as e:
                print(e)
                trial_number -= 1

            time.sleep(5)

        if trial_number == 0:
            print("Exceeded maximum trials. Could not generate response!")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
