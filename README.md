# ASSERTIFY: 
Utilizing Large Language Models to Generate Assertions for Production Code - Arxiv - 2025

Version 1.0

### Abstract:
Production assertions are statements embedded in the code to help developers validate their assumptions about the code. They assist developers in debugging, provide valuable documentation, and enhance code comprehension. Current research in this area primarily focuses on assertion generation for unit tests using techniques, such as static analysis and deep learning. While these techniques have shown promise, they fall short when it comes to generating production assertions, which serve a different purpose.
This research addresses the gap by introducing Assertify, an automated end-to-end tool that leverages Large Language Models (LLMs) and prompt engineering with few-shot learning to generate production assertions. By creating context-rich prompts, the tool emulates the approach developers take when creating production assertions for their code. To evaluate our approach, we compiled a dataset of 2,810 methods by scraping 22 mature Java repositories from GitHub. Our experiments demonstrate the effectiveness of few-shot learning by producing assertions with an average ROUGE-L score of 0.526, indicating reasonably high structural similarity with the assertions written by developers. This research demonstrates the potential of LLMs in automating the generation of production assertions that resemble the original assertions.

Paper link: https://arxiv.org/abs/2411.16927 


### Authors:
-------------------
Mohammad Jalili Torkamani

Abhinav Sharma

Dr. Nikita Mehrotra

Prof. Rahul Purandare


## How to run:
We recommend using IntelliJ IDEA for running Java sub-projects (to efficiently resolve Maven dependencies used within the programs) and PyCharm for Python scripts.

### Python libraries used:

Torch

Openai

Transformers

Evaluate

Rouge_score

Tiktoken

### Java libraries used:

JavaParser

Selenium

GSON

JUnit

Git

Jsoup

OpenCSV

Slf4j
