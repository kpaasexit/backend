#!/bin/bash

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}AI Service Deployment${NC}"

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}✗ .env file not found!${NC}"
    echo "Create .env file with: OPENAI_API_KEY=your_api_key"
    exit 1
fi

# Stop and remove existing containers
echo -e "${YELLOW}Cleaning up...${NC}"
docker-compose down > /dev/null 2>&1

# Remove dangling images (none:none)
echo -e "${YELLOW}Removing dangling images...${NC}"
docker image prune -f > /dev/null 2>&1

# Proto 컴파일은 Dockerfile에서 처리됨

# Build all services
echo -e "${YELLOW}Building services...${NC}"
docker-compose build ai-service

# Build and start Elasticsearch with nori plugin
echo -e "${YELLOW}Setting up Elasticsearch with nori plugin...${NC}"
docker-compose up -d elasticsearch
echo -e "${YELLOW}Waiting for Elasticsearch to be ready...${NC}"
sleep 15

# Verify nori plugin is installed
echo -e "${YELLOW}Verifying nori plugin installation...${NC}"
max_plugin_attempts=10
plugin_attempt=0
while [ $plugin_attempt -lt $max_plugin_attempts ]; do
    if docker exec ai-service-elasticsearch elasticsearch-plugin list 2>/dev/null | grep -q analysis-nori; then
        echo -e "${GREEN}✓ Nori plugin installed${NC}"
        break
    fi
    plugin_attempt=$((plugin_attempt + 1))
    sleep 2
done

if [ $plugin_attempt -eq $max_plugin_attempts ]; then
    echo -e "${RED}✗ Failed to verify nori plugin installation${NC}"
    exit 1
fi

# Clean up intermediate build images
echo -e "${YELLOW}Cleaning up intermediate images...${NC}"
docker image prune -f

# Start all services
echo -e "${YELLOW}Starting services...${NC}"
docker-compose up -d > /dev/null 2>&1

# Wait for services
echo -e "${YELLOW}Waiting for services...${NC}"
sleep 10

# Health check with retries
max_attempts=20
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s -f http://localhost:8090/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Service is running${NC}"

        # Initialize collections
        echo -e "${YELLOW}Initializing collections...${NC}"
        docker exec ai-service-app python -c "
import sys
sys.path.insert(0, '/app')
from app.vectordb.collections import Collections
results = Collections.create_all_collections(force=False)
for collection, created in results.items():
    if created:
        print(f'✓ Collection {collection} initialized')
" 2>/dev/null || echo -e "${YELLOW}Collections initialization skipped (may already exist)${NC}"

        echo -e "${GREEN}✓ Deploy complete!${NC}"
        echo -e "  Docs: http://localhost:8090/docs"
        echo -e "  Health: http://localhost:8090/health"
        echo ""
        echo -e "${YELLOW}Next steps:${NC}"
        echo -e "  1. Index Naver KIN data:"
        echo -e "     ${GREEN}poetry run python scripts/index_naver_kin.py${NC}"
        echo -e "  2. Test REST autocomplete:"
        echo -e "     ${GREEN}curl 'http://localhost:8090/api/search/autocomplete?query=침실&limit=5'${NC}"
        echo -e "  3. View API docs:"
        echo -e "     ${GREEN}http://localhost:8090/docs${NC}"
        echo ""
        echo "Starting logs..."
        echo "----------------------------------------"
        docker-compose logs -f --tail=20 ai-service
        exit 0
    fi
    attempt=$((attempt + 1))
    sleep 2
done

echo -e "${RED}✗ Service failed to start${NC}"
echo "Check logs: docker logs ai-service-app"
exit 1