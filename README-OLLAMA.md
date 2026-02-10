# Setting up Ollama for Incident Analyst

This project uses **Ollama** to run local Large Language Models (LLMs) for incident diagnosis and text embeddings.

## Quick Start (Docker)

We have provided a script to set up Ollama and the required models using Docker Compose.

1. Ensure Docker and Docker Compose are installed and running.
2. Run the setup script:
   ```bash
   ./scripts/setup-ollama.sh
   ```

This will:
- Start an Ollama container on port `11434`.
- Pull the chat model: `qwen2.5:7b-instruct`.
- Pull the embedding model: `nomic-embed-text`.

## Manual Setup (Native)

If you prefer to run Ollama natively:

1. Download and install Ollama from [ollama.com](https://ollama.com/).
2. Start the Ollama server.
3. Pull the required models:
   ```bash
   ollama pull qwen2.5:7b-instruct
   ollama pull nomic-embed-text
   ```

## Configuration

The application is configured to connect to Ollama at `http://localhost:11434`. If your Ollama instance is running elsewhere, update `src/main/resources/application.properties`:

```properties
quarkus.langchain4j.ollama.base-url=http://your-host:11434
```

## Troubleshooting

- **Connection Refused**: Ensure the Ollama container or server is running and port `11434` is accessible.
- **Model Not Found**: Ensure you have run `ollama pull` for both models.
- **Performance**: Running a 7B model on CPU can be slow. If you have an NVIDIA GPU, consider configuring the NVIDIA Container Toolkit for Docker.
