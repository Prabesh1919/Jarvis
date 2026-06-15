import os
import json
import urllib.request
import urllib.error

def load_api_key():
    # 1. Try reading from .env file
    env_path = "/Users/prabeshshah/Desktop/jarvis/.env"
    if os.path.exists(env_path):
        with open(env_path, "r") as f:
            for line in f:
                if line.startswith("GEMINI_API_KEY="):
                    val = line.split("=")[1].strip()
                    if val and val != "your_gemini_api_key_here":
                        return val
                        
    # 2. Try reading from local.properties file
    props_path = "/Users/prabeshshah/Desktop/jarvis/local.properties"
    if os.path.exists(props_path):
        with open(props_path, "r") as f:
            for line in f:
                if line.startswith("GEMINI_API_KEY="):
                    val = line.split("=")[1].strip()
                    if val and val != "YOUR_GEMINI_API_KEY_HERE":
                        return val
                        
    return None

def test_llm():
    print("=========================================================")
    print("🧪 JARVIS LLM BRAIN CONNECTOR TEST")
    print("=========================================================")
    
    api_key = load_api_key()
    if not api_key:
        print("❌ Error: Gemini API Key is not configured yet!")
        print("👉 Please edit the '.env' file or 'local.properties' and set your GEMINI_API_KEY.")
        return
        
    print("🔑 Loaded API Key successfully (length: {} chars)...".format(len(api_key)))
    print("🔌 Connecting to Gemini API (gemini-2.5-flash)...")

    url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={}".format(api_key)
    headers = {"Content-Type": "application/json"}
    
    # Prompt payload
    data = {
        "contents": [{
            "parts": [{"text": "Hello Gemini! Confirm in exactly 5 words that you are online and ready."}]
        }]
    }
    
    req_body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=req_body, headers=headers, method="POST")
    
    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            res_data = json.loads(res_body)
            
            # Extract response text
            candidates = res_data.get("candidates", [])
            if candidates:
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    text_response = parts[0].get("text", "").strip()
                    print("\n🟢 SUCCESS! Gemini Response:")
                    print("---------------------------------------------------------")
                    print(text_response)
                    print("---------------------------------------------------------")
                    return
            print("❌ Error: Received unexpected response format.")
            print(res_body)
            
    except urllib.error.HTTPError as e:
        print("\n❌ HTTP Connection Error: Code {}".format(e.code))
        try:
            err_msg = e.read().decode("utf-8")
            print("Details: ", err_msg)
        except Exception:
            pass
    except Exception as e:
        print("\n❌ Connection Failed: {}".format(str(e)))

if __name__ == "__main__":
    test_llm()
