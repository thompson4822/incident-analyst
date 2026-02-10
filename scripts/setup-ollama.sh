#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Starting Ollama via Docker Compose...${NC}"
docker compose -f docker-compose-ollama.yml up -d

echo -e "${BLUE}Waiting for Ollama to be ready...${NC}"
until curl -s http://localhost:11434/api/tags > /dev/null; do
  sleep 2
done

echo -e "${GREEN}Ollama is ready!${NC}"

echo -e "${BLUE}Pulling model: qwen2.5:7b-instruct...${NC}"
docker exec ollama ollama pull qwen2.5:7b-instruct

echo -e "${BLUE}Pulling model: nomic-embed-text...${NC}"
docker exec ollama ollama pull nomic-embed-text

echo -e "${GREEN}Setup complete! Your local LLM and Embedding models are ready.${NC}"
echo -e "You can now run the application with ${BLUE}mvn quarkus:dev${NC}"
