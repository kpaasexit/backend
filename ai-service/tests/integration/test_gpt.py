#!/usr/bin/env python
"""Test GPT answer generation."""

import json
import requests

# Test data
test_request = {
    "category": "생활경제/계약",
    "question": "전세 계약 시 주의해야 할 사항은 무엇인가요?",
    "context": "서울에서 처음 전세 계약을 하려고 합니다."
}

# Send request to FastAPI endpoint
url = "http://localhost:8000/api/v1/gpt-answer"
headers = {"Content-Type": "application/json"}

print("Sending request to:", url)
print("Request data:", json.dumps(test_request, indent=2, ensure_ascii=False))
print("-" * 50)

try:
    response = requests.post(url, json=test_request, headers=headers, timeout=30)

    if response.status_code == 200:
        result = response.json()
        print("✅ Success!")
        print("\nAnswer:")
        print(result.get("answer", "No answer"))
        print("\n" + "=" * 50)
        print("Category:", result.get("category"))
        print("Tokens used:", result.get("tokens_used", 0))
    else:
        print("❌ Error:", response.status_code)
        print("Response:", response.text)

except requests.exceptions.Timeout:
    print("❌ Request timeout - OpenAI API might be slow")
except Exception as e:
    print("❌ Request failed:", str(e))