import argparse
import os

import tiktoken

tokenizer_path = "ADDRESS GOES HERE"
system_input = None
if os.path.isfile(tokenizer_path):
    system_input = str(open(tokenizer_path, "r", encoding="utf-8").read())


def count_tokens(input_text, encoder):
    enc = tiktoken.get_encoding(encoder)
    num_tokens = len(enc.encode(input_text))
    return num_tokens


def main():
    parser = argparse.ArgumentParser(description="Count the number of tokens in a given input string.")
    parser.add_argument("--encoder", default="cl100k_base", help="The tokenizer encoder.")

    args = parser.parse_args()
    encoder = args.encoder
    token_count = count_tokens(system_input, encoder)

    print(token_count)


if __name__ == "__main__":
    main()
